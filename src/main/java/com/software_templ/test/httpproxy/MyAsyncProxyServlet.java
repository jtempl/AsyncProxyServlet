/*
 * Copyright Josef Templ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.software_templ.test.httpproxy;

import com.software_templ.httpproxy.AsyncProxyServlet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;


@WebServlet(urlPatterns = "/*", asyncSupported = true)
public class MyAsyncProxyServlet extends AsyncProxyServlet {

    @Override
    public String getServletInfo() {
        return "Test of AsyncProxyServlet";
    }

    @Override
    public void init() throws ServletException {
        log("init");
        super.init();
        doPreserveHost = true; // adapt for versions < 1.11
    }

    protected CloseableHttpAsyncClient createAsyncHttpClient() {
        if (connectionManager instanceof PoolingNHttpClientConnectionManager) {
            PoolingNHttpClientConnectionManager pool = (PoolingNHttpClientConnectionManager) connectionManager;
            pool.setMaxTotal(50000);
            pool.setDefaultMaxPerRoute(50000);
        };
        return super.createAsyncHttpClient();
    }

    @Override
    protected void initTarget() throws ServletException { // no fixed target
    }

    protected String getTargetUri(HttpServletRequest servletRequest) { // rewrite target dynamically
        String targetUri = servletRequest.getRequestURI();
        String rewriteUri = "http://localhost:80" + targetUri;
        //String rewriteUri =  "http://www.apache.org" + targetUri;
        return rewriteUri;
    }

    @Override // adapt for versions < 1.11
    protected String rewritePathInfoFromRequest(HttpServletRequest servletRequest) { // disabled
        return null;
    }
}
