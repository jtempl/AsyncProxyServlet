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

package com.software_templ.httpproxy;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncByteConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Customizable asynchronous (non-blocking) proxy servlet derived from MITRE ProxyServlet.
 * Customization follows the MITRE ProxyServlet design.
 *
 * @author Josef Templ
 */
public class AsyncProxyServlet extends ProxyServlet {

    protected ConnectingIOReactor ioReactor;
    protected NHttpClientConnectionManager connectionManager;
    protected CloseableHttpAsyncClient asyncProxyClient;
    protected RequestConfig requestConfig;

    @Override
    public String getServletInfo() {
        return this.getClass().getName();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        ioReactor = createIOReactor();
        connectionManager = createConnectionManager();
        asyncProxyClient = createAsyncHttpClient();
        requestConfig = createRequestConfig();
        asyncProxyClient.start();
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        HttpUriRequest proxyRequest = RequestBuilder.create(servletRequest.getMethod())
                .setConfig(requestConfig)
                .setUri(rewriteUrlFromRequest(servletRequest))
                .setEntity(new InputStreamEntity(servletRequest.getInputStream(), servletRequest.getContentLength()))
                .build();
        copyRequestHeaders(servletRequest, proxyRequest);
        HttpAsyncRequestProducer producer = HttpAsyncMethods.create(proxyRequest);
        AsyncContext asyncCtx = servletRequest.startAsync();
        AsyncByteConsumer<HttpResponse> consumer = createAsyncByteConsumer(asyncCtx);
        asyncProxyClient.execute(producer, consumer, createFutureCallback(asyncCtx, proxyRequest));
    }

    @Override
    public void destroy() {
        try {
            asyncProxyClient.close();
        } catch (IOException e) {
            log("While destroying servlet, closing async HttpClient: " + e, e);
        }
        super.destroy();
    }

    protected ConnectingIOReactor createIOReactor() throws ServletException {
        try {
            return new DefaultConnectingIOReactor();
        } catch (IOReactorException e) {
            throw new ServletException(e);
        }
    }

    protected NHttpClientConnectionManager createConnectionManager() {
        return new PoolingNHttpClientConnectionManager(ioReactor);
    }

    @Override //adapt for versions < 1.11
    protected HttpClient createHttpClient() {
        return null; // no need for the synchronous proxy
    }

    protected CloseableHttpAsyncClient createAsyncHttpClient() {
        return HttpAsyncClients.custom().setConnectionManager(connectionManager).build();
    }

    protected RequestConfig createRequestConfig() {
        return RequestConfig.custom()
                .setRedirectsEnabled(false)
                .build();
    }

    protected AsyncByteConsumer<HttpResponse> createAsyncByteConsumer(AsyncContext asyncCtx) {
        return new AsyncByteConsumer<HttpResponse>() {
            HttpResponse proxyResponse;

            @Override
            protected void onResponseReceived(final HttpResponse proxyResponse) {
                this.proxyResponse = proxyResponse;
                HttpServletRequest servletRequest = (HttpServletRequest) asyncCtx.getRequest();
                HttpServletResponse servletResponse = (HttpServletResponse) asyncCtx.getResponse();
                StatusLine statusLine = proxyResponse.getStatusLine();
                servletResponse.setStatus(statusLine.getStatusCode());
                copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
            }

            @Override
            protected void onByteReceived(final ByteBuffer buf, final IOControl ioctrl) throws IOException {
                byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                ServletOutputStream outputStream = asyncCtx.getResponse().getOutputStream();
                outputStream.write(bytes);
                outputStream.flush();
            }

            @Override
            protected void releaseResources() {
            }

            @Override
            protected HttpResponse buildResult(final HttpContext context) {
                return this.proxyResponse;
            }
        };
    }

    protected FutureCallback<HttpResponse> createFutureCallback(AsyncContext asyncCtx, HttpUriRequest request) {
        return new FutureCallback<HttpResponse>() {

            public void completed(final HttpResponse response) {
                log(request, response);
                asyncCtx.complete();
            }

            public void failed(final Exception e) {
                log("request failed: " + request.getRequestLine().toString(), e);
                asyncCtx.complete();
            }

            public void cancelled() {
                log("request cancelled: " + request.getRequestLine());
                asyncCtx.complete();
            }
        };
    }

    public void log(HttpUriRequest request, HttpResponse response) {
        log("request completed: " + request.getRequestLine() + " -> " + response.getStatusLine());
    }
}
