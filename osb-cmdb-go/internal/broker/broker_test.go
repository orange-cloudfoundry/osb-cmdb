package broker

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/cloudfoundry/go-cfclient/v3/resource"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/catalog"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/cfclient"
	osbcontext "github.com/orange-cloudfoundry/osb-cmdb-go/internal/context"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/metadata"
	"github.com/pivotal-cf/brokerapi/v11/domain"
	"github.com/pivotal-cf/brokerapi/v11/middlewares"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestOperationState_EncodeAndDecode(t *testing.T) {
	tests := []struct {
		name string
		op   *OperationState
	}{
		{
			name: "provision operation",
			op: &OperationState{
				Type:       OperationProvision,
				InstanceID: "osb-instance-123",
				JobGUID:    "job-123",
			},
		},
		{
			name: "deprovision operation with job",
			op: &OperationState{
				Type:                       OperationDeprovision,
				BackingCFServiceInstanceID: "cf-guid-456",
				JobGUID:                    "job-789",
			},
		},
		{
			name: "bind operation",
			op: &OperationState{
				Type:                       OperationBind,
				BackingCFServiceInstanceID: "cf-guid-abc",
				JobGUID:                    "job-def",
			},
		},
		{
			name: "unbind operation",
			op: &OperationState{
				Type:    OperationUnbind,
				JobGUID: "job-unbind",
			},
		},
		{
			name: "update operation",
			op: &OperationState{
				Type:                       OperationUpdate,
				BackingCFServiceInstanceID: "cf-guid-update",
				JobGUID:                    "job-update",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			encoded := tt.op.Encode()
			assert.NotEmpty(t, encoded)

			decoded, err := DecodeOperationState(encoded)
			require.NoError(t, err)
			assert.Equal(t, tt.op.Type, decoded.Type)
			assert.Equal(t, tt.op.InstanceID, decoded.InstanceID)
			assert.Equal(t, tt.op.BackingCFServiceInstanceID, decoded.BackingCFServiceInstanceID)
			assert.Equal(t, tt.op.JobGUID, decoded.JobGUID)
		})
	}
}

func TestDecodeOperationState_RawJSON(t *testing.T) {
	// Test backwards compatibility with raw JSON
	op := &OperationState{Type: OperationProvision, InstanceID: "instance-123", JobGUID: "job-123"}

	rawJSON, _ := json.Marshal(op)

	decoded, err := DecodeOperationState(string(rawJSON))
	require.NoError(t, err)
	assert.Equal(t, OperationProvision, decoded.Type)
	assert.Equal(t, "instance-123", decoded.InstanceID)
	assert.Equal(t, "job-123", decoded.JobGUID)
}

func TestDecodeOperationState_Invalid(t *testing.T) {
	tests := []struct {
		name    string
		input   string
		wantErr bool
	}{
		{
			name:    "empty string",
			input:   "",
			wantErr: true,
		},
		{
			name:    "invalid base64",
			input:   "not-valid-base64!!!",
			wantErr: true,
		},
		{
			name:    "valid base64 but not JSON",
			input:   base64.StdEncoding.EncodeToString([]byte("not json")),
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := DecodeOperationState(tt.input)
			if tt.wantErr {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func TestNewProvisionOperation(t *testing.T) {
	op := NewProvisionOperation("osb-instance-id", "job-guid")

	assert.Equal(t, OperationProvision, op.Type)
	assert.Equal(t, "osb-instance-id", op.InstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestAsyncProvisionSpecWithoutBackingInstance(t *testing.T) {
	result := &cfclient.CreateServiceInstanceResult{
		ServiceInstance: nil,
		JobGUID:         "create-job-guid",
		IsAsync:         true,
	}

	spec := asyncProvisionSpec("osb-instance-id", result.JobGUID)
	state, err := DecodeOperationState(spec.OperationData)

	require.NoError(t, err)
	assert.True(t, spec.IsAsync)
	assert.Equal(t, OperationProvision, state.Type)
	assert.Equal(t, "osb-instance-id", state.InstanceID)
	assert.Equal(t, "create-job-guid", state.JobGUID)
	assert.Empty(t, state.BackingCFServiceInstanceID)
}

func TestMapJobToLastOperation(t *testing.T) {
	broker := &Broker{}
	tests := []struct {
		name  string
		state resource.JobState
		want  domain.LastOperationState
	}{
		{name: "succeeded", state: resource.JobStateComplete, want: domain.Succeeded},
		{name: "failed", state: resource.JobStateFailed, want: domain.Failed},
		{name: "processing", state: resource.JobStateProcessing, want: domain.InProgress},
		{name: "polling", state: resource.JobStatePolling, want: domain.InProgress},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := broker.mapJobToLastOperation(&resource.Job{State: tt.state})
			assert.Equal(t, tt.want, result.State)
		})
	}
}

func TestMapCFLastOperation(t *testing.T) {
	broker := &Broker{}
	tests := []struct {
		name  string
		state string
		want  domain.LastOperationState
	}{
		{name: "succeeded", state: "succeeded", want: domain.Succeeded},
		{name: "failed", state: "failed", want: domain.Failed},
		{name: "in progress", state: "in progress", want: domain.InProgress},
		{name: "initial", state: "initial", want: domain.InProgress},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := broker.mapCFLastOperation(resource.LastOperation{State: tt.state, Description: "test"})
			assert.Equal(t, tt.want, result.State)
			assert.Equal(t, "test", result.Description)
		})
	}
}

func TestNewDeprovisionOperation(t *testing.T) {
	op := NewDeprovisionOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationDeprovision, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewUpdateOperation(t *testing.T) {
	op := NewUpdateOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationUpdate, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewBindOperation(t *testing.T) {
	op := NewBindOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationBind, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewUnbindOperation(t *testing.T) {
	op := NewUnbindOperation("job-guid")

	assert.Equal(t, OperationUnbind, op.Type)
	assert.Empty(t, op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestBrokerConfig_Validation(t *testing.T) {
	tests := []struct {
		name    string
		cfg     *Config
		wantErr string
	}{
		{
			name:    "nil config",
			cfg:     nil,
			wantErr: "config is required",
		},
		{
			name:    "missing CF client",
			cfg:     &Config{},
			wantErr: "CF client is required",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := New(tt.cfg)
			require.Error(t, err)
			assert.Contains(t, err.Error(), tt.wantErr)
		})
	}
}

func TestBrokerProvisionDuplicateCompatibility(t *testing.T) {
	for _, tt := range []struct {
		name       string
		planID     string
		parameters string
		compatible bool
	}{
		{name: "matching plan and parameters", planID: "plan-1", parameters: `{"size":"small"}`, compatible: true},
		{name: "different plan", planID: "plan-2", parameters: `{"size":"small"}`},
		{name: "different parameters", planID: "plan-1", parameters: `{"size":"large"}`},
	} {
		t.Run(tt.name, func(t *testing.T) {
			server := newCFServer(t, func(w http.ResponseWriter, r *http.Request) {
				switch {
				case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances":
					writeJSON(t, w, http.StatusOK, map[string]interface{}{"pagination": map[string]interface{}{}, "resources": []interface{}{map[string]interface{}{"guid": "instance-guid"}}})
				case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances/instance-guid":
					writeJSON(t, w, http.StatusOK, map[string]interface{}{"guid": "instance-guid", "relationships": map[string]interface{}{"service_plan": map[string]interface{}{"data": map[string]interface{}{"guid": "plan-1"}}}})
				case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances/instance-guid/parameters":
					writeJSON(t, w, http.StatusOK, map[string]interface{}{"size": "small"})
				default:
					t.Errorf("unexpected request: %s %s", r.Method, r.URL)
				}
			})
			defer server.Close()

			broker := newRealBroker(t, server.URL)
			spec, err := broker.Provision(context.Background(), "instance-id", provisionDetails(tt.planID, tt.parameters), true)
			if tt.compatible {
				require.NoError(t, err)
				assert.True(t, spec.AlreadyExists)
				return
			}
			assert.Error(t, err)
			assert.Contains(t, err.Error(), "instance already exists")
		})
	}
}

func TestBrokerProvisionAppliesOriginatingIdentityMetadata(t *testing.T) {
	for _, tt := range []struct {
		name       string
		header     string
		wantLabels map[string]string
		wantAnnots map[string]string
	}{
		{name: "Cloud Foundry", header: identityHeader(t, "cloudfoundry", map[string]interface{}{"user_id": "user-1", "space_guid": "space-1"}), wantLabels: map[string]string{"brokered_service_context_user_id": "user-1", "brokered_service_context_space_guid": "space-1"}},
		{name: "Kubernetes", header: identityHeader(t, "kubernetes", map[string]interface{}{"uid": "user-1", "namespace": "team-a"}), wantLabels: map[string]string{"brokered_service_context_uid": "user-1"}, wantAnnots: map[string]string{"brokered_service_context_namespace": "team-a"}},
		{name: "missing", wantLabels: map[string]string{"brokered_service_instance_guid": "instance-id"}},
		{name: "unknown", header: identityHeader(t, "other", map[string]interface{}{"uid": "user-1"}), wantLabels: map[string]string{"brokered_service_instance_guid": "instance-id"}},
	} {
		t.Run(tt.name, func(t *testing.T) {
			var create map[string]interface{}
			server := newCFServer(t, func(w http.ResponseWriter, r *http.Request) {
				switch {
				case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances":
					writeJSON(t, w, http.StatusOK, map[string]interface{}{"pagination": map[string]interface{}{}, "resources": []interface{}{}})
				case r.Method == http.MethodPost && r.URL.Path == "/v3/service_instances":
					require.NoError(t, json.NewDecoder(r.Body).Decode(&create))
					w.Header().Set("Location", "/v3/jobs/create-job")
					w.WriteHeader(http.StatusAccepted)
				default:
					t.Errorf("unexpected request: %s %s", r.Method, r.URL)
				}
			})
			defer server.Close()

			broker := newRealBroker(t, server.URL)
			ctx := context.Background()
			if tt.header != "" {
				ctx = context.WithValue(ctx, middlewares.OriginatingIdentityKey, tt.header)
			}
			spec, err := broker.Provision(ctx, "instance-id", provisionDetails("plan-1", `{}`), true)
			require.NoError(t, err)
			assert.True(t, spec.IsAsync)
			metadata := create["metadata"].(map[string]interface{})
			assertMetadata(t, metadata["labels"], tt.wantLabels)
			assertMetadata(t, metadata["annotations"], tt.wantAnnots)
		})
	}
}

func TestBrokerOperationsWithRealCFClient(t *testing.T) {
	server := newCFServer(t, func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances":
			if r.URL.Query().Get("names") == "new-instance" || r.URL.Query().Get("label_selector") == "brokered_service_instance_guid=new-instance" {
				writeJSON(t, w, http.StatusOK, map[string]interface{}{"pagination": map[string]interface{}{}, "resources": []interface{}{}})
				return
			}
			writeJSON(t, w, http.StatusOK, map[string]interface{}{
				"pagination": map[string]interface{}{},
				"resources": []interface{}{map[string]interface{}{
					"guid":          "instance-guid",
					"dashboard_url": "https://dashboard",
					"relationships": map[string]interface{}{
						"service_plan": map[string]interface{}{"data": map[string]interface{}{"guid": "plan-1"}},
					},
				}},
			})
		case r.Method == http.MethodGet && r.URL.Path == "/v3/service_credential_bindings":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"pagination": map[string]interface{}{}, "resources": []interface{}{map[string]interface{}{"guid": "binding-guid", "name": "binding-id"}}})
		case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances/instance-guid/parameters":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"size": "small"})
		case r.Method == http.MethodGet && r.URL.Path == "/v3/service_instances/instance-guid":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"guid": "instance-guid", "last_operation": map[string]interface{}{"state": "succeeded", "description": "complete"}})
		case r.Method == http.MethodGet && r.URL.Path == "/v3/service_credential_bindings/binding-guid/details":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"credentials": map[string]interface{}{"uri": "postgres://example"}})
		case r.Method == http.MethodGet && r.URL.Path == "/v3/jobs/job-guid":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"guid": "job-guid", "state": "COMPLETE"})
		case r.Method == http.MethodPost && (r.URL.Path == "/v3/service_instances" || r.URL.Path == "/v3/service_credential_bindings"):
			w.Header().Set("Location", "/v3/jobs/job-guid")
			w.WriteHeader(http.StatusAccepted)
		case r.Method == http.MethodPatch && r.URL.Path == "/v3/service_instances/instance-guid":
			w.Header().Set("Location", "/v3/jobs/job-guid")
			w.WriteHeader(http.StatusAccepted)
		case r.Method == http.MethodDelete && (r.URL.Path == "/v3/service_instances/instance-guid" || r.URL.Path == "/v3/service_credential_bindings/binding-guid"):
			w.Header().Set("Location", "/v3/jobs/job-guid")
			w.WriteHeader(http.StatusAccepted)
		default:
			t.Errorf("unexpected request: %s %s", r.Method, r.URL)
		}
	})
	defer server.Close()

	broker := newRealBroker(t, server.URL)
	ctx := context.Background()

	provisioned, err := broker.Provision(ctx, "new-instance", provisionDetails("plan-1", `{}`), true)
	require.NoError(t, err)
	assert.True(t, provisioned.IsAsync)

	deprovisioned, err := broker.Deprovision(ctx, "instance-id", domain.DeprovisionDetails{}, true)
	require.NoError(t, err)
	assert.True(t, deprovisioned.IsAsync)

	updated, err := broker.Update(ctx, "instance-id", domain.UpdateDetails{PlanID: "plan-1"}, true)
	require.NoError(t, err)
	assert.True(t, updated.IsAsync)

	bound, err := broker.Bind(ctx, "instance-id", "new-binding", domain.BindDetails{}, true)
	require.NoError(t, err)
	assert.True(t, bound.IsAsync)

	unbound, err := broker.Unbind(ctx, "instance-id", "binding-id", domain.UnbindDetails{}, true)
	require.NoError(t, err)
	assert.True(t, unbound.IsAsync)

	instance, err := broker.GetInstance(ctx, "instance-id", domain.FetchInstanceDetails{ServiceID: "service-1", PlanID: "plan-1"})
	require.NoError(t, err)
	assert.Equal(t, map[string]interface{}{"size": "small"}, instance.Parameters)

	operation, err := broker.LastOperation(ctx, "instance-id", domain.PollDetails{OperationData: NewProvisionOperation("instance-id", "job-guid").Encode()})
	require.NoError(t, err)
	assert.Equal(t, domain.Succeeded, operation.State)
}

type staticCatalog struct{}

func (staticCatalog) GetServices() []domain.Service { return nil }
func (staticCatalog) GetService(serviceID string) *domain.Service {
	if serviceID != "service-1" {
		return nil
	}
	return &domain.Service{ID: serviceID, Plans: []domain.ServicePlan{{ID: "plan-1"}, {ID: "plan-2"}}}
}
func (staticCatalog) GetPlan(string) *domain.ServicePlan { return nil }
func (staticCatalog) GetPlanForService(serviceID, planID string) *domain.ServicePlan {
	if serviceID == "service-1" && (planID == "plan-1" || planID == "plan-2") {
		return &domain.ServicePlan{ID: planID}
	}
	return nil
}
func (staticCatalog) Refresh(context.Context) error { return nil }
func (staticCatalog) Start(context.Context)         {}
func (staticCatalog) Stop()                         {}

var _ catalog.Service = staticCatalog{}

func newRealBroker(t *testing.T, apiHost string) *Broker {
	t.Helper()
	client, err := cfclient.New(context.Background(), &cfclient.ClientConfig{APIHost: apiHost, Username: "user", Password: "password"})
	if err != nil && strings.Contains(err.Error(), "connection refused") {
		t.Skipf("loopback HTTP is unavailable in this environment: %v", err)
	}
	require.NoError(t, err)
	broker, err := New(&Config{CFClient: client, Catalog: staticCatalog{}, MetadataService: metadata.NewFormatter(), ContextParser: osbcontext.NewParser(), DefaultSpaceGUID: "space-guid"})
	require.NoError(t, err)
	return broker
}

func newCFServer(t *testing.T, handler http.HandlerFunc) *httptest.Server {
	t.Helper()
	var serverURL string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/":
			writeJSON(t, w, http.StatusOK, map[string]string{"token_endpoint": serverURL + "/oauth/token"})
		case "/oauth/token":
			writeJSON(t, w, http.StatusOK, map[string]interface{}{"access_token": "test-token", "token_type": "bearer", "expires_in": 3600})
		default:
			handler(w, r)
		}
	}))
	serverURL = server.URL
	return server
}

func provisionDetails(planID, parameters string) domain.ProvisionDetails {
	return domain.ProvisionDetails{ServiceID: "service-1", PlanID: planID, RawParameters: json.RawMessage(parameters)}
}

func identityHeader(t *testing.T, platform string, value map[string]interface{}) string {
	t.Helper()
	raw, err := json.Marshal(value)
	require.NoError(t, err)
	return platform + " " + base64.StdEncoding.EncodeToString(raw)
}

func writeJSON(t *testing.T, w http.ResponseWriter, status int, value interface{}) {
	t.Helper()
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	require.NoError(t, json.NewEncoder(w).Encode(value))
}

func assertMetadata(t *testing.T, got interface{}, want map[string]string) {
	t.Helper()
	actual, _ := got.(map[string]interface{})
	if len(want) == 0 {
		assert.Empty(t, actual)
		return
	}
	for key, value := range want {
		assert.Equal(t, value, actual[key])
	}
}
