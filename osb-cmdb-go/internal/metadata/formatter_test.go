package metadata

import (
	"testing"

	osbcontext "github.com/orange-cloudfoundry/osb-cmdb-go/internal/context"
	"github.com/stretchr/testify/assert"
)

func TestFormatter_Format_CloudFoundry(t *testing.T) {
	formatter := NewFormatter()

	ctx := &osbcontext.Context{
		Platform: osbcontext.PlatformCloudFoundry,
		Raw: map[string]interface{}{
			"organization_guid": "org-guid-123",
			"organization_name": "my-org",
			"space_guid":        "space-guid-456",
			"space_name":        "my-space",
		},
	}

	labels, annotations := formatter.Format(ctx)

	// GUIDs should be labels
	assert.Contains(t, labels, "brokered_service_context_organization_guid")
	assert.Equal(t, "org-guid-123", *labels["brokered_service_context_organization_guid"])
	assert.Contains(t, labels, "brokered_service_context_space_guid")
	assert.Equal(t, "space-guid-456", *labels["brokered_service_context_space_guid"])

	// Names should be annotations
	assert.Contains(t, annotations, "brokered_service_context_organization_name")
	assert.Equal(t, "my-org", *annotations["brokered_service_context_organization_name"])
	assert.Contains(t, annotations, "brokered_service_context_space_name")
	assert.Equal(t, "my-space", *annotations["brokered_service_context_space_name"])
}

func TestFormatter_Format_Kubernetes(t *testing.T) {
	formatter := NewFormatter()

	ctx := &osbcontext.Context{
		Platform: osbcontext.PlatformKubernetes,
		Raw: map[string]interface{}{
			"uid":       "k8s-uid-789",
			"clusterid": "cluster-abc",
			"namespace": "default",
		},
	}

	labels, annotations := formatter.Format(ctx)

	// UID and clusterid should be labels
	assert.Contains(t, labels, "brokered_service_context_uid")
	assert.Equal(t, "k8s-uid-789", *labels["brokered_service_context_uid"])
	assert.Contains(t, labels, "brokered_service_context_clusterid")
	assert.Equal(t, "cluster-abc", *labels["brokered_service_context_clusterid"])

	// Namespace should be annotation
	assert.Contains(t, annotations, "brokered_service_context_namespace")
	assert.Equal(t, "default", *annotations["brokered_service_context_namespace"])
}

func TestFormatter_Format_OrangeAnnotations(t *testing.T) {
	formatter := NewFormatter()

	ctx := &osbcontext.Context{
		Platform: osbcontext.PlatformCloudFoundry,
		Raw: map[string]interface{}{
			"organization_guid": "org-guid",
			"instance_annotations": map[string]interface{}{
				"orange.com/team":      "platform",
				"orange.com/owner":     "john.doe",
				"other-annotation":     "ignored",
			},
		},
	}

	labels, _ := formatter.Format(ctx)

	// Orange annotations should be promoted to labels
	assert.Contains(t, labels, "brokered_service_context_orange_team")
	assert.Equal(t, "platform", *labels["brokered_service_context_orange_team"])
	assert.Contains(t, labels, "brokered_service_context_orange_owner")
	assert.Equal(t, "john.doe", *labels["brokered_service_context_orange_owner"])

	// Non-orange annotations should not be promoted
	assert.NotContains(t, labels, "brokered_service_context_other-annotation")
}

func TestFormatter_Format_Nil(t *testing.T) {
	formatter := NewFormatter()

	labels, annotations := formatter.Format(nil)
	assert.Empty(t, labels)
	assert.Empty(t, annotations)
}

func TestFormatter_Format_Unknown(t *testing.T) {
	formatter := NewFormatter()

	ctx := &osbcontext.Context{
		Platform: osbcontext.PlatformUnknown,
		Raw:      map[string]interface{}{"key": "value"},
	}

	labels, annotations := formatter.Format(ctx)
	assert.Empty(t, labels)
	assert.Empty(t, annotations)
}

func TestSanitizeKey(t *testing.T) {
	tests := []struct {
		input string
		want  string
	}{
		{"simple_key", "simple_key"},
		{"key-with-dashes", "key-with-dashes"},
		{"key.with.dots", "key_with_dots"},
		{"key/with/slashes", "key_with_slashes"},
		{"MixedCase", "mixedcase"},
		{"123numeric", "_123numeric"},
		{"special!@#chars", "special___chars"},
	}

	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			got := sanitizeKey(tt.input)
			assert.Equal(t, tt.want, got)
		})
	}
}
