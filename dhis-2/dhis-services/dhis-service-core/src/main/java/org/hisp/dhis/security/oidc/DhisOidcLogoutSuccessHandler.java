/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.oidc;

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_LOGOUT_REDIRECT_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.STANDARD_LOGOUT_REDIRECT_URL;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class DhisOidcLogoutSuccessHandler implements LogoutSuccessHandler
{
    private OidcClientInitiatedLogoutSuccessHandler handler;

    @Autowired
    private DhisOidcProviderRepository dhisOidcProviderRepository;

    @Autowired
    public DhisConfigurationProvider dhisConfigurationProvider;

    @PostConstruct
    public void init()
    {
        String logoutUri = dhisConfigurationProvider.getProperty( OIDC_LOGOUT_REDIRECT_URL );
        this.handler = new OidcClientInitiatedLogoutSuccessHandler( dhisOidcProviderRepository );
        this.handler.setPostLogoutRedirectUri( logoutUri );

        String standardLogoutUri = dhisConfigurationProvider.getProperty( STANDARD_LOGOUT_REDIRECT_URL );
        if ( StringUtils.isNotBlank( standardLogoutUri ) )
        {
            this.handler.setDefaultTargetUrl( standardLogoutUri );
        }
    }

    @Override
    public void onLogoutSuccess( HttpServletRequest request, HttpServletResponse response,
        Authentication authentication )
        throws IOException,
        ServletException
    {
        handler.onLogoutSuccess( request, response, authentication );
    }
}
