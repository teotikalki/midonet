/*
 * Copyright 2012 Midokura PTE LTD.
 */
package org.midonet.api.auth;

import com.google.inject.Inject;
import org.midonet.cluster.DataClient;
import org.midonet.midolman.state.StateAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Configurable auth client that skips authentication but allows setting of
 * roles.
 */
public final class MockAuthService implements AuthService {

    private final static Logger log = LoggerFactory
            .getLogger(MockAuthService.class);
    private final MockAuthConfig config;
    private final Map<String, UserIdentity> tokenMap;
    private final DataClient dataClient;

    /**
     * Create a MockAuthService object.
     *
     * @param config
     *            MockAuthConfig object.
     * @param dataClient
     *            {@link DataClient} object to retrieve from data store
     */
    @Inject
    public MockAuthService(MockAuthConfig config, DataClient dataClient) {

        this.config = config;
        this.dataClient = dataClient;
        this.tokenMap = new HashMap<String, UserIdentity>();
        String token = config.getAdminToken();
        if (token != null && token.length() > 0) {
            setRoles(token, AuthRole.ADMIN);
        }

        token = config.getTenantAdminToken();
        if (token != null && token.length() > 0) {
            setRoles(token, AuthRole.TENANT_ADMIN);
        }

        token = config.getTenantUserToken();
        if (token != null && token.length() > 0) {
            setRoles(token, AuthRole.TENANT_USER);
        }

    }

    private UserIdentity createUserIdentity() {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setTenantId("no_auth_tenant_id");
        userIdentity.setTenantName("no_auth_tenant_name");
        userIdentity.setUserId("no_auth_user");
        userIdentity.setToken("no_auth_token");
        return userIdentity;
    }

    private void setRoles(String tokenStr, String role) {
        String[] tokens = tokenStr.split(",");
        for (String token : tokens) {
            String tok = token.trim();
            if (tok.length() > 0) {
                UserIdentity identity = tokenMap.get(tok);
                if (identity == null) {
                    identity = createUserIdentity();
                    tokenMap.put(tok, identity);
                }
                identity.addRole(role);
            }
        }
    }

    /**
     * Return a UserIdentity object.
     *
     * @param token
     *            Token to use to get the roles.
     * @return UserIdentity object.
     */
    @Override
    public UserIdentity getUserIdentityByToken(String token) {
        log.debug("MockAuthService.getUserIdentityByToken entered. {}", token);

        UserIdentity user = tokenMap.get(token);
        if (user == null) {
            // For backward compatibility, no token == admin privilege.
            user = createUserIdentity();
            user.addRole(AuthRole.ADMIN);
        }

        log.debug("MockAuthService.getUserIdentityByToken exiting. {}", user);
        return user;
    }

    /**
     * Always return admin token
     * @param _username username Unused
     * @param _password password Unused
     * @param _request HttpServletRequest object Unsued
     * @return Admin token specified in config.
     * @throws AuthException
     */
    @Override
    public Token login(String _username, String _password,
                           HttpServletRequest _request) throws AuthException {
        return new Token(this.config.getAdminToken(), null);
    }

    /**
     * Gets the tenants stored in data store.
     *
     * @param request Servlet request if additional field is needed to retrieve
     *                tenants.
     * @return List of Tenant objects
     * @throws AuthException
     */
    @Override
    public List<Tenant> getTenants(HttpServletRequest request)
            throws AuthException {
        List<Tenant> tenantIds = new ArrayList<Tenant>();

        try {
            Set<String> ids = dataClient.tenantsGetAll();
            for (String id : ids) {
                tenantIds.add(new MockTenant(id));
            }

        } catch (StateAccessException ex) {
            throw new AuthDataAccessException(
                    "Data access error while getting tenants", ex);
        }

        return tenantIds;
    }

    public static class MockTenant implements Tenant {

        private final String id;

        public MockTenant(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }
    }
}
