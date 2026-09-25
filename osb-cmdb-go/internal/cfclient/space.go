package cfclient

import (
	"context"
	"fmt"

	"github.com/cloudfoundry/go-cfclient/v3/client"
	"github.com/cloudfoundry/go-cfclient/v3/resource"
)

// GetOrCreateSpace gets an existing space or creates it if it doesn't exist.
// This is used for creating backing spaces for brokered services.
func (c *Client) GetOrCreateSpace(ctx context.Context, spaceName, orgGUID string) (*resource.Space, error) {
	c.logger.Debug("getting or creating space", "name", spaceName, "org_guid", orgGUID)

	// Try to find existing space
	spaces, _, err := c.cf.Spaces.List(ctx, &client.SpaceListOptions{
		Names:             client.Filter{Values: []string{spaceName}},
		OrganizationGUIDs: client.Filter{Values: []string{orgGUID}},
	})
	if err != nil {
		return nil, fmt.Errorf("listing spaces: %w", err)
	}

	if len(spaces) > 0 {
		c.logger.Debug("found existing space", "guid", spaces[0].GUID)
		return spaces[0], nil
	}

	// Create new space
	c.logger.Debug("creating new space", "name", spaceName)

	space, err := c.cf.Spaces.Create(ctx, &resource.SpaceCreate{
		Name: spaceName,
		Relationships: &resource.SpaceRelationships{
			Organization: &resource.ToOneRelationship{
				Data: &resource.Relationship{GUID: orgGUID},
			},
		},
	})
	if err != nil {
		return nil, fmt.Errorf("creating space: %w", err)
	}

	c.logger.Debug("created space", "guid", space.GUID)
	return space, nil
}

// GetSpace retrieves a space by GUID.
func (c *Client) GetSpace(ctx context.Context, guid string) (*resource.Space, error) {
	c.logger.Debug("getting space", "guid", guid)
	return c.cf.Spaces.Get(ctx, guid)
}

// GetSpaceByName retrieves a space by name in an org.
func (c *Client) GetSpaceByName(ctx context.Context, name, orgGUID string) (*resource.Space, error) {
	c.logger.Debug("getting space by name", "name", name, "org_guid", orgGUID)

	spaces, _, err := c.cf.Spaces.List(ctx, &client.SpaceListOptions{
		Names:             client.Filter{Values: []string{name}},
		OrganizationGUIDs: client.Filter{Values: []string{orgGUID}},
	})
	if err != nil {
		return nil, fmt.Errorf("listing spaces: %w", err)
	}

	if len(spaces) == 0 {
		return nil, nil
	}

	return spaces[0], nil
}

// GetOrg retrieves an organization by GUID.
func (c *Client) GetOrg(ctx context.Context, guid string) (*resource.Organization, error) {
	c.logger.Debug("getting org", "guid", guid)
	return c.cf.Organizations.Get(ctx, guid)
}

// GetOrgByName retrieves an organization by name.
func (c *Client) GetOrgByName(ctx context.Context, name string) (*resource.Organization, error) {
	c.logger.Debug("getting org by name", "name", name)

	orgs, _, err := c.cf.Organizations.List(ctx, &client.OrganizationListOptions{
		Names: client.Filter{Values: []string{name}},
	})
	if err != nil {
		return nil, fmt.Errorf("listing orgs: %w", err)
	}

	if len(orgs) == 0 {
		return nil, nil
	}

	return orgs[0], nil
}
