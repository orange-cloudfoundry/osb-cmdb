package context

import (
	"encoding/base64"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestParser_Parse_CloudFoundry(t *testing.T) {
	parser := NewParser()

	rawContext := json.RawMessage(`{
		"organization_guid": "org-guid-123",
		"organization_name": "my-org",
		"space_guid": "space-guid-456",
		"space_name": "my-space",
		"instance_name": "my-service"
	}`)

	ctx, err := parser.Parse(rawContext)
	require.NoError(t, err)
	assert.Equal(t, PlatformCloudFoundry, ctx.Platform)
	assert.Equal(t, "org-guid-123", ctx.Raw["organization_guid"])
	assert.Equal(t, "my-org", ctx.Raw["organization_name"])
}

func TestParser_Parse_Kubernetes(t *testing.T) {
	parser := NewParser()

	rawContext := json.RawMessage(`{
		"namespace": "default",
		"clusterid": "cluster-abc",
		"instance_name": "my-service"
	}`)

	ctx, err := parser.Parse(rawContext)
	require.NoError(t, err)
	assert.Equal(t, PlatformKubernetes, ctx.Platform)
	assert.Equal(t, "default", ctx.Raw["namespace"])
}

func TestParser_Parse_Empty(t *testing.T) {
	parser := NewParser()

	ctx, err := parser.Parse(nil)
	require.NoError(t, err)
	assert.Equal(t, PlatformUnknown, ctx.Platform)

	ctx, err = parser.Parse(json.RawMessage{})
	require.NoError(t, err)
	assert.Equal(t, PlatformUnknown, ctx.Platform)
}

func TestParser_Parse_Invalid(t *testing.T) {
	parser := NewParser()

	_, err := parser.Parse(json.RawMessage(`not json`))
	assert.Error(t, err)
}

func TestParser_ParseOriginatingIdentity_CloudFoundry(t *testing.T) {
	parser := NewParser()

	identity := map[string]interface{}{
		"user_id": "user-123",
	}
	identityJSON, _ := json.Marshal(identity)
	header := "cloudfoundry " + base64.StdEncoding.EncodeToString(identityJSON)

	result, err := parser.ParseOriginatingIdentity(header)
	require.NoError(t, err)
	assert.Equal(t, PlatformCloudFoundry, result.Platform)
	assert.Equal(t, "user-123", result.Value["user_id"])
}

func TestParser_ParseOriginatingIdentity_Kubernetes(t *testing.T) {
	parser := NewParser()

	identity := map[string]interface{}{
		"uid":      "k8s-user-456",
		"username": "admin",
		"groups":   []string{"system:masters"},
	}
	identityJSON, _ := json.Marshal(identity)
	header := "kubernetes " + base64.StdEncoding.EncodeToString(identityJSON)

	result, err := parser.ParseOriginatingIdentity(header)
	require.NoError(t, err)
	assert.Equal(t, PlatformKubernetes, result.Platform)
	assert.Equal(t, "k8s-user-456", result.Value["uid"])
	assert.Equal(t, "admin", result.Value["username"])
}

func TestParser_ParseOriginatingIdentity_Empty(t *testing.T) {
	parser := NewParser()

	result, err := parser.ParseOriginatingIdentity("")
	require.NoError(t, err)
	assert.Nil(t, result)
}

func TestParser_ParseOriginatingIdentity_Invalid(t *testing.T) {
	parser := NewParser()

	tests := []struct {
		name   string
		header string
	}{
		{
			name:   "no space separator",
			header: "cloudfoundrybase64data",
		},
		{
			name:   "invalid base64",
			header: "cloudfoundry not-valid-base64!!!",
		},
		{
			name:   "valid base64 but not JSON",
			header: "cloudfoundry " + base64.StdEncoding.EncodeToString([]byte("not json")),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := parser.ParseOriginatingIdentity(tt.header)
			assert.Error(t, err)
		})
	}
}

func TestParser_GetCloudFoundryContext(t *testing.T) {
	parser := NewParser()

	rawContext := json.RawMessage(`{
		"organization_guid": "org-guid-123",
		"organization_name": "my-org",
		"space_guid": "space-guid-456",
		"space_name": "my-space",
		"instance_name": "my-service",
		"instance_annotations": {
			"orange.com/team": "platform",
			"other": "value"
		}
	}`)

	ctx, err := parser.Parse(rawContext)
	require.NoError(t, err)

	cfCtx := parser.GetCloudFoundryContext(ctx)
	assert.Equal(t, "org-guid-123", cfCtx.OrganizationGUID)
	assert.Equal(t, "my-org", cfCtx.OrganizationName)
	assert.Equal(t, "space-guid-456", cfCtx.SpaceGUID)
	assert.Equal(t, "my-space", cfCtx.SpaceName)
	assert.Equal(t, "my-service", cfCtx.InstanceName)
	assert.Equal(t, "platform", cfCtx.InstanceAnnotations["orange.com/team"])
}

func TestParser_GetKubernetesContext(t *testing.T) {
	parser := NewParser()

	rawContext := json.RawMessage(`{
		"namespace": "default",
		"clusterid": "cluster-abc",
		"instance_name": "my-service"
	}`)

	ctx, err := parser.Parse(rawContext)
	require.NoError(t, err)

	k8sCtx := parser.GetKubernetesContext(ctx)
	assert.Equal(t, "default", k8sCtx.Namespace)
	assert.Equal(t, "cluster-abc", k8sCtx.ClusterID)
	assert.Equal(t, "my-service", k8sCtx.InstanceName)
}

func TestParser_GetCloudFoundryContext_Nil(t *testing.T) {
	parser := NewParser()

	cfCtx := parser.GetCloudFoundryContext(nil)
	assert.Nil(t, cfCtx)
}

func TestParser_GetKubernetesContext_Nil(t *testing.T) {
	parser := NewParser()

	k8sCtx := parser.GetKubernetesContext(nil)
	assert.Nil(t, k8sCtx)
}
