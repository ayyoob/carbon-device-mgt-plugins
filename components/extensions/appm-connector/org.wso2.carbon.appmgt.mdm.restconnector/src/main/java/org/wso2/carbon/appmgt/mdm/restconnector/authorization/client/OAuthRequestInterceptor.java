/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.appmgt.mdm.restconnector.authorization.client;

import feign.Client;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxrs.JAXRSContract;
import feign.slf4j.Slf4jLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appmgt.mdm.restconnector.Constants;
import org.wso2.carbon.appmgt.mdm.restconnector.authorization.client.dto.AccessTokenInfo;
import org.wso2.carbon.appmgt.mdm.restconnector.authorization.client.dto.ApiApplicationKey;
import org.wso2.carbon.appmgt.mdm.restconnector.authorization.client.dto.ApiApplicationRegistrationService;
import org.wso2.carbon.appmgt.mdm.restconnector.authorization.client.dto.ApiRegistrationProfile;
import org.wso2.carbon.appmgt.mdm.restconnector.authorization.client.dto.TokenIssuerService;
import org.wso2.carbon.appmgt.mdm.restconnector.config.AuthorizationConfigurationManager;
import org.wso2.carbon.appmgt.mdm.restconnector.internal.AuthorizationDataHolder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * This is a request interceptor to add oauth token header.
 */
public class OAuthRequestInterceptor implements RequestInterceptor {
    private AccessTokenInfo tokenInfo;
    private String refreshTimeOffset;
    private static final String API_APPLICATION_REGISTRATION_CONTEXT = "/api-application-registration";
    private static final String DEVICE_MANAGEMENT_SERVICE_TAG[] = {"device_management"};
    private static final String APPLICATION_NAME = "appm_restconnector_application";
    private static final String PASSWORD_GRANT_TYPE = "password";
    private static final String REFRESH_GRANT_TYPE = "refresh_token";
    private ApiApplicationRegistrationService apiApplicationRegistrationService;
    private TokenIssuerService tokenIssuerService;
    private static ApiApplicationKey apiApplicationKey;
    private static Log log = LogFactory.getLog(OAuthRequestInterceptor.class);


    /**
     * Creates an interceptor that authenticates all requests.
     */
    public OAuthRequestInterceptor() {
        refreshTimeOffset = AuthorizationConfigurationManager.getInstance().getTokenRefreshTimeOffset();
        String username = AuthorizationConfigurationManager.getInstance().getUserName();
        String password = AuthorizationConfigurationManager.getInstance().getPassword();
        apiApplicationRegistrationService = Feign.builder().client(getSSLClient()).logger(new Slf4jLogger()).logLevel(
                Logger.Level.FULL).requestInterceptor(new BasicAuthRequestInterceptor(username, password))
                .contract(new JAXRSContract()).encoder(new GsonEncoder()).decoder(new GsonDecoder())
                .target(ApiApplicationRegistrationService.class,
                        AuthorizationConfigurationManager.getInstance().getServerURL() +
                                API_APPLICATION_REGISTRATION_CONTEXT);
        AuthorizationDataHolder.getInstance().setApiApplicationRegistrationService(apiApplicationRegistrationService);

    }

    /**
     * Api create.
     *
     * @param template {@link RequestTemplate} object
     */
    @Override
    public void apply(RequestTemplate template) {
        if (tokenInfo == null) {
            if (apiApplicationKey == null) {
                ApiRegistrationProfile apiRegistrationProfile = new ApiRegistrationProfile();
                apiRegistrationProfile.setApplicationName(APPLICATION_NAME);
                apiRegistrationProfile.setIsAllowedToAllDomains(false);
                apiRegistrationProfile.setIsMappingAnExistingOAuthApp(false);
                apiRegistrationProfile.setTags(DEVICE_MANAGEMENT_SERVICE_TAG);
                apiApplicationKey = apiApplicationRegistrationService.register(apiRegistrationProfile);
            }
            String consumerKey = apiApplicationKey.getConsumerKey();
            String consumerSecret = apiApplicationKey.getConsumerSecret();
            String username = AuthorizationConfigurationManager.getInstance().getUserName();
            String password = AuthorizationConfigurationManager.getInstance().getPassword();
            if (tokenIssuerService == null) {
                tokenIssuerService = Feign.builder().client(getSSLClient()).logger(new Slf4jLogger()).logLevel(
                        Logger.Level.FULL)
                        .requestInterceptor(new BasicAuthRequestInterceptor(consumerKey, consumerSecret))
                        .contract(new JAXRSContract()).encoder(new GsonEncoder()).decoder(new GsonDecoder())
                        .target(TokenIssuerService.class,
                                AuthorizationConfigurationManager.getInstance().getTokenApiURL());
            }
            tokenInfo = tokenIssuerService.getToken(PASSWORD_GRANT_TYPE, username, password);
            tokenInfo.setExpires_in(System.currentTimeMillis() + tokenInfo.getExpires_in());
        }
        synchronized (this) {
            if (System.currentTimeMillis() + Long.parseLong(refreshTimeOffset) > tokenInfo.getExpires_in()) {
                try {
                    tokenInfo = tokenIssuerService.getToken(REFRESH_GRANT_TYPE, tokenInfo.getRefresh_token());
                    tokenInfo.setExpires_in(System.currentTimeMillis() + tokenInfo.getExpires_in());
                } catch (FeignException e) {
                    tokenInfo = null;
                    apply(template);
                }
            }
        }
        String headerValue = Constants.RestConstants.BEARER + tokenInfo.getAccess_token();
        template.header(Constants.RestConstants.AUTHORIZATION, headerValue);
    }

    private static Client getSSLClient() {
        return new Client.Default(getTrustedSSLSocketFactory(), new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    private static SSLSocketFactory getTrustedSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            return null;
        }
    }

}
