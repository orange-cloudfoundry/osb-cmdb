// Package cfclient provides a wrapper around go-cfclient v3 for OSB-CMDB operations.
package cfclient

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"github.com/cloudfoundry/go-cfclient/v3/client"
	"github.com/cloudfoundry/go-cfclient/v3/config"
	"github.com/cloudfoundry/go-cfclient/v3/resource"
)

// Client wraps go-cfclient and provides high-level operations for OSB-CMDB.
type Client struct {
	cf     *client.Client
	config *ClientConfig
	logger *slog.Logger
}

// ClientConfig holds CF client configuration.
type ClientConfig struct {
	APIHost      string
	Username     string
	Password     string
	ClientID     string
	ClientSecret string
	DefaultOrg   string
	DefaultSpace string
	SkipSSL      bool
}

// ServiceInstanceParams holds parameters for creating/updating service instances.
type ServiceInstanceParams struct {
	Name            string
	ServicePlanGUID string
	SpaceGUID       string
	Parameters      map[string]interface{}
	Tags            []string
	Labels          map[string]*string
	Annotations     map[string]*string
}

// ServiceBindingParams holds parameters for creating service bindings (service keys).
type ServiceBindingParams struct {
	Name                string
	ServiceInstanceGUID string
	Parameters          map[string]interface{}
}

// New creates a new CF client wrapper.
func New(ctx context.Context, cfg *ClientConfig) (*Client, error) {
	if cfg == nil {
		return nil, fmt.Errorf("config is required")
	}

	var opts []config.Option
	if cfg.ClientID != "" || cfg.ClientSecret != "" {
		if cfg.ClientID == "" || cfg.ClientSecret == "" {
			return nil, fmt.Errorf("both client ID and client secret are required")
		}
		opts = append(opts, config.ClientCredentials(cfg.ClientID, cfg.ClientSecret))
	} else {
		opts = append(opts, config.UserPassword(cfg.Username, cfg.Password))
	}
	if cfg.SkipSSL {
		opts = append(opts, config.SkipTLSValidation())
	}

	cfCfg, err := config.New(cfg.APIHost, opts...)
	if err != nil {
		return nil, fmt.Errorf("creating cf config: %w", err)
	}

	cf, err := client.New(cfCfg)
	if err != nil {
		return nil, fmt.Errorf("creating cf client: %w", err)
	}

	return &Client{
		cf:     cf,
		config: cfg,
		logger: slog.Default().With("component", "cfclient"),
	}, nil
}

// GetCFClient returns the underlying go-cfclient for advanced operations.
func (c *Client) GetCFClient() *client.Client {
	return c.cf
}

// GetDefaultOrgGUID returns the GUID of the default org.
func (c *Client) GetDefaultOrgGUID(ctx context.Context) (string, error) {
	orgs, _, err := c.cf.Organizations.List(ctx, &client.OrganizationListOptions{
		Names: client.Filter{Values: []string{c.config.DefaultOrg}},
	})
	if err != nil {
		return "", fmt.Errorf("listing orgs: %w", err)
	}
	if len(orgs) == 0 {
		return "", fmt.Errorf("org %q not found", c.config.DefaultOrg)
	}
	return orgs[0].GUID, nil
}

// GetDefaultSpaceGUID returns the GUID of the default space.
func (c *Client) GetDefaultSpaceGUID(ctx context.Context) (string, error) {
	orgGUID, err := c.GetDefaultOrgGUID(ctx)
	if err != nil {
		return "", err
	}

	spaces, _, err := c.cf.Spaces.List(ctx, &client.SpaceListOptions{
		Names:             client.Filter{Values: []string{c.config.DefaultSpace}},
		OrganizationGUIDs: client.Filter{Values: []string{orgGUID}},
	})
	if err != nil {
		return "", fmt.Errorf("listing spaces: %w", err)
	}
	if len(spaces) == 0 {
		return "", fmt.Errorf("space %q not found in org %q", c.config.DefaultSpace, c.config.DefaultOrg)
	}
	return spaces[0].GUID, nil
}

// WaitForJob polls a CF job until it completes or fails.
func (c *Client) WaitForJob(ctx context.Context, jobGUID string, timeout time.Duration) error {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	return c.cf.Jobs.PollComplete(ctx, jobGUID, &client.PollingOptions{
		Timeout:       timeout,
		CheckInterval: 2 * time.Second,
	})
}

// GetServiceOffering returns a service offering by GUID.
func (c *Client) GetServiceOffering(ctx context.Context, guid string) (*resource.ServiceOffering, error) {
	return c.cf.ServiceOfferings.Get(ctx, guid)
}

// GetServicePlan returns a service plan by GUID.
func (c *Client) GetServicePlan(ctx context.Context, guid string) (*resource.ServicePlan, error) {
	return c.cf.ServicePlans.Get(ctx, guid)
}

// ListServiceOfferings lists all service offerings in the default space.
func (c *Client) ListServiceOfferings(ctx context.Context) ([]*resource.ServiceOffering, error) {
	spaceGUID, err := c.GetDefaultSpaceGUID(ctx)
	if err != nil {
		return nil, err
	}

	offerings, _, err := c.cf.ServiceOfferings.List(ctx, &client.ServiceOfferingListOptions{
		SpaceGUIDs: client.Filter{Values: []string{spaceGUID}},
	})
	return offerings, err
}

// ListServicePlans lists all service plans for a service offering.
func (c *Client) ListServicePlans(ctx context.Context, serviceOfferingGUID string) ([]*resource.ServicePlan, error) {
	plans, _, err := c.cf.ServicePlans.List(ctx, &client.ServicePlanListOptions{
		ServiceOfferingGUIDs: client.Filter{Values: []string{serviceOfferingGUID}},
	})
	return plans, err
}
