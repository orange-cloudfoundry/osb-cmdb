// Package metadata provides formatting of OSB context as CF labels and annotations.
package metadata

import (
	"strings"

	osbcontext "github.com/orange-cloudfoundry/osb-cmdb-go/internal/context"
)

// Service formats OSB context as CF labels and annotations.
type Service interface {
	// Format converts parsed context to CF labels and annotations.
	Format(ctx *osbcontext.Context) (labels map[string]*string, annotations map[string]*string)
}

// Formatter implements the Service interface.
type Formatter struct {
	// Prefix for brokered service context labels/annotations
	prefix string
}

// NewFormatter creates a new metadata formatter.
func NewFormatter() *Formatter {
	return &Formatter{
		prefix: "brokered_service_context_",
	}
}

// Format converts parsed context to CF labels and annotations.
// GUID properties become labels, mutable properties become annotations.
func (f *Formatter) Format(ctx *osbcontext.Context) (labels map[string]*string, annotations map[string]*string) {
	labels = make(map[string]*string)
	annotations = make(map[string]*string)

	if ctx == nil {
		return labels, annotations
	}

	switch ctx.Platform {
	case osbcontext.PlatformCloudFoundry:
		return f.formatCloudFoundry(ctx)
	case osbcontext.PlatformKubernetes:
		return f.formatKubernetes(ctx)
	default:
		// Unknown platform - return minimal metadata
		return labels, annotations
	}
}

// formatCloudFoundry formats CF-specific context as labels and annotations.
func (f *Formatter) formatCloudFoundry(ctx *osbcontext.Context) (labels map[string]*string, annotations map[string]*string) {
	labels = make(map[string]*string)
	annotations = make(map[string]*string)

	if ctx == nil || ctx.Raw == nil {
		return labels, annotations
	}

	for key, value := range ctx.Raw {
		strValue, ok := value.(string)
		if !ok {
			// Handle nested objects like annotations
			continue
		}

		labelKey := f.prefix + sanitizeKey(key)

		// GUID and ID properties become labels (immutable identifiers)
		if strings.Contains(key, "_guid") || strings.Contains(key, "_id") {
			labels[labelKey] = &strValue
		} else {
			// Other properties become annotations (potentially mutable)
			annotations[labelKey] = &strValue
		}
	}

	// Extract whitelisted Orange annotations and promote to labels
	if instanceAnnotations, ok := ctx.Raw["instance_annotations"].(map[string]interface{}); ok {
		f.extractOrangeAnnotations(instanceAnnotations, labels)
	}

	return labels, annotations
}

// formatKubernetes formats K8S-specific context as labels and annotations.
func (f *Formatter) formatKubernetes(ctx *osbcontext.Context) (labels map[string]*string, annotations map[string]*string) {
	labels = make(map[string]*string)
	annotations = make(map[string]*string)

	if ctx == nil || ctx.Raw == nil {
		return labels, annotations
	}

	for key, value := range ctx.Raw {
		strValue, ok := value.(string)
		if !ok {
			continue
		}

		labelKey := f.prefix + sanitizeKey(key)

		// K8S UID becomes a label (immutable identifier)
		if key == "uid" || key == "clusterid" {
			labels[labelKey] = &strValue
		} else {
			// Namespace and other properties become annotations
			annotations[labelKey] = &strValue
		}
	}

	return labels, annotations
}

// extractOrangeAnnotations extracts orange.com/ prefixed annotations and promotes them to labels.
func (f *Formatter) extractOrangeAnnotations(annotations map[string]interface{}, labels map[string]*string) {
	const orangePrefix = "orange.com/"

	for key, value := range annotations {
		if !strings.HasPrefix(key, orangePrefix) {
			continue
		}

		strValue, ok := value.(string)
		if !ok {
			continue
		}

		// Convert "orange.com/team" to "brokered_service_context_orange_team"
		suffix := strings.TrimPrefix(key, orangePrefix)
		labelKey := f.prefix + "orange_" + sanitizeKey(suffix)
		labels[labelKey] = &strValue
	}
}

// sanitizeKey converts a context key to a valid CF label/annotation key.
// CF labels have restrictions on allowed characters.
func sanitizeKey(key string) string {
	// Replace dots, slashes, and other special chars with underscores
	result := strings.Map(func(r rune) rune {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_' || r == '-' {
			return r
		}
		return '_'
	}, key)

	// Ensure it starts with a letter
	if len(result) > 0 && result[0] >= '0' && result[0] <= '9' {
		result = "_" + result
	}

	return strings.ToLower(result)
}
