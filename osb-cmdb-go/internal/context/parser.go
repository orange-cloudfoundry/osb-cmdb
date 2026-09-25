// Package context provides parsing for OSB context and originating identity headers.
package context

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"
)

// Platform represents the originating platform type.
type Platform string

const (
	PlatformCloudFoundry Platform = "cloudfoundry"
	PlatformKubernetes   Platform = "kubernetes"
	PlatformUnknown      Platform = "unknown"
)

// Context holds parsed OSB request context.
type Context struct {
	Platform Platform
	Raw      map[string]interface{}
}

// CloudFoundryContext holds CF-specific context properties.
type CloudFoundryContext struct {
	OrganizationGUID        string            `json:"organization_guid,omitempty"`
	OrganizationName        string            `json:"organization_name,omitempty"`
	SpaceGUID               string            `json:"space_guid,omitempty"`
	SpaceName               string            `json:"space_name,omitempty"`
	InstanceName            string            `json:"instance_name,omitempty"`
	OrganizationAnnotations map[string]string `json:"organization_annotations,omitempty"`
	SpaceAnnotations        map[string]string `json:"space_annotations,omitempty"`
	InstanceAnnotations     map[string]string `json:"instance_annotations,omitempty"`
}

// KubernetesContext holds K8S-specific context properties.
type KubernetesContext struct {
	Namespace    string `json:"namespace,omitempty"`
	ClusterID    string `json:"clusterid,omitempty"`
	InstanceName string `json:"instance_name,omitempty"`
}

// OriginatingIdentity holds parsed X-Broker-API-Originating-Identity header.
type OriginatingIdentity struct {
	Platform Platform
	Value    map[string]interface{}
}

// Parser parses OSB context and originating identity.
type Parser struct{}

// NewParser creates a new context parser.
func NewParser() *Parser {
	return &Parser{}
}

// Parse parses raw JSON context into a Context struct.
func (p *Parser) Parse(rawContext json.RawMessage) (*Context, error) {
	if len(rawContext) == 0 {
		return &Context{Platform: PlatformUnknown}, nil
	}

	var raw map[string]interface{}
	if err := json.Unmarshal(rawContext, &raw); err != nil {
		return nil, fmt.Errorf("invalid context JSON: %w", err)
	}

	ctx := &Context{
		Platform: p.detectPlatform(raw),
		Raw:      raw,
	}

	return ctx, nil
}

// ParseOriginatingIdentity parses the X-Broker-API-Originating-Identity header.
// Format: "<platform> <base64-encoded-json>"
func (p *Parser) ParseOriginatingIdentity(header string) (*OriginatingIdentity, error) {
	if header == "" {
		return nil, nil
	}

	parts := strings.Fields(header)
	if len(parts) != 2 {
		return nil, fmt.Errorf("invalid originating identity format")
	}

	platform := Platform(strings.ToLower(parts[0]))
	encodedValue := parts[1]

	decoded, err := base64.StdEncoding.DecodeString(encodedValue)
	if err != nil {
		return nil, fmt.Errorf("invalid base64 in originating identity: %w", err)
	}

	var value map[string]interface{}
	if err := json.Unmarshal(decoded, &value); err != nil {
		return nil, fmt.Errorf("invalid JSON in originating identity: %w", err)
	}

	return &OriginatingIdentity{
		Platform: platform,
		Value:    value,
	}, nil
}

// ParseOriginatingIdentityContext converts an originating identity header into
// metadata context suitable for formatting as Cloud Foundry metadata.
func (p *Parser) ParseOriginatingIdentityContext(header string) (*Context, error) {
	identity, err := p.ParseOriginatingIdentity(header)
	if err != nil || identity == nil {
		return nil, err
	}

	return &Context{Platform: identity.Platform, Raw: identity.Value}, nil
}

// GetCloudFoundryContext extracts CF-specific context from the parsed context.
func (p *Parser) GetCloudFoundryContext(ctx *Context) *CloudFoundryContext {
	if ctx == nil || ctx.Raw == nil {
		return nil
	}

	cfCtx := &CloudFoundryContext{}

	if v, ok := ctx.Raw["organization_guid"].(string); ok {
		cfCtx.OrganizationGUID = v
	}
	if v, ok := ctx.Raw["organization_name"].(string); ok {
		cfCtx.OrganizationName = v
	}
	if v, ok := ctx.Raw["space_guid"].(string); ok {
		cfCtx.SpaceGUID = v
	}
	if v, ok := ctx.Raw["space_name"].(string); ok {
		cfCtx.SpaceName = v
	}
	if v, ok := ctx.Raw["instance_name"].(string); ok {
		cfCtx.InstanceName = v
	}

	// Extract annotations
	if orgAnnotations, ok := ctx.Raw["organization_annotations"].(map[string]interface{}); ok {
		cfCtx.OrganizationAnnotations = toStringMap(orgAnnotations)
	}
	if spaceAnnotations, ok := ctx.Raw["space_annotations"].(map[string]interface{}); ok {
		cfCtx.SpaceAnnotations = toStringMap(spaceAnnotations)
	}
	if instanceAnnotations, ok := ctx.Raw["instance_annotations"].(map[string]interface{}); ok {
		cfCtx.InstanceAnnotations = toStringMap(instanceAnnotations)
	}

	return cfCtx
}

// GetKubernetesContext extracts K8S-specific context from the parsed context.
func (p *Parser) GetKubernetesContext(ctx *Context) *KubernetesContext {
	if ctx == nil || ctx.Raw == nil {
		return nil
	}

	k8sCtx := &KubernetesContext{}

	if v, ok := ctx.Raw["namespace"].(string); ok {
		k8sCtx.Namespace = v
	}
	if v, ok := ctx.Raw["clusterid"].(string); ok {
		k8sCtx.ClusterID = v
	}
	if v, ok := ctx.Raw["instance_name"].(string); ok {
		k8sCtx.InstanceName = v
	}

	return k8sCtx
}

// detectPlatform determines the platform from context properties.
func (p *Parser) detectPlatform(raw map[string]interface{}) Platform {
	// Check for CF-specific properties
	if _, ok := raw["organization_guid"]; ok {
		return PlatformCloudFoundry
	}
	if _, ok := raw["space_guid"]; ok {
		return PlatformCloudFoundry
	}

	// Check for K8S-specific properties
	if _, ok := raw["namespace"]; ok {
		return PlatformKubernetes
	}
	if _, ok := raw["clusterid"]; ok {
		return PlatformKubernetes
	}

	// Check platform field
	if platform, ok := raw["platform"].(string); ok {
		switch strings.ToLower(platform) {
		case "cloudfoundry":
			return PlatformCloudFoundry
		case "kubernetes":
			return PlatformKubernetes
		}
	}

	return PlatformUnknown
}

// toStringMap converts map[string]interface{} to map[string]string.
func toStringMap(m map[string]interface{}) map[string]string {
	result := make(map[string]string)
	for k, v := range m {
		if s, ok := v.(string); ok {
			result[k] = s
		}
	}
	return result
}
