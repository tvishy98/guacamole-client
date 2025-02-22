/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.header;

import com.google.inject.Inject;
import com.google.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.auth.header.user.AuthenticatedUser;
import java.security.Principal;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;

/**
 * Service providing convenience functions for the HTTP Header
 * AuthenticationProvider implementation.
 */
public class AuthenticationProviderService {

    private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";
    private static final String X_GOOGLE_JWT_HEADER = "x-goog-iap-jwt-assertion" ;

    /**  
     *  TODO : Read from config later.
    **/
    private static final String jwtValidationProjectID = "123" ;
    private static final String jwtValidationBackendServiceID = "123" ; 


    /**
     * Service for retrieving header configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Pull HTTP header from request if present
        HttpServletRequest request = credentials.getRequest();
        if (request != null) {
            // TODO : Check and handle if the result is a result.
            String jwtToken = request.getHeader(X_GOOGLE_JWT_HEADER) ;

            String expectedAudience = String.format("/projects/%s/global/backendServices/%s", 

            if (jwtToken != null) {
                TokenVerifier tokenVerifier = TokenVerifier.newBuilder().setAudience(expectedAudience).setIssuer(IAP_ISSUER_URL).build();

                try {
                    JsonWebToken jsonWebToken = tokenVerifier.verify(jwtToken);
                    JsonWebToken.Payload payload = jsonWebToken.getPayload();
                    String username = payload.get("email");

                    AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                    authenticatedUser.init(username, credentials);
                    return authenticatedUser;

                } catch (TokenVerifier.VerificationException e) {
                    // Authentication not provided via header, yet, so we request it.
                    throw new GuacamoleInvalidCredentialsException("Invalid login.", CredentialsInfo.USERNAME_PASSWORD);
                }
            }

        }

        // Authentication not provided via header, yet, so we request it.
        throw new GuacamoleInvalidCredentialsException("Invalid login.", CredentialsInfo.USERNAME_PASSWORD);

    }

}
