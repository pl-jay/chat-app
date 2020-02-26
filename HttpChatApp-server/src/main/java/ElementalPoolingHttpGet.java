/*
 * ====================================================================
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */



import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.pool.BasicConnFactory;
import org.apache.http.impl.pool.BasicConnPool;
import org.apache.http.impl.pool.BasicPoolEntry;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;



/**
 * Elemental example for executing multiple GET requests from different threads using a connection
 * pool.
 */
public class ElementalPoolingHttpGet {

    public static void main(String[] args) throws Exception {
        final HttpProcessor httpproc = HttpProcessorBuilder.create()
                .add(new RequestContent())
                .add(new RequestTargetHost())
                .add(new RequestConnControl())
                .add(new RequestUserAgent("Test/1.1"))
                .add(new RequestExpectContinue(true)).build();

        final HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

        final BasicConnPool pool = new BasicConnPool(new BasicConnFactory());
        pool.setDefaultMaxPerRoute(2);
        pool.setMaxTotal(2);

        HttpHost[] targets = {
                new HttpHost("www.google.com", 80),
                new HttpHost("www.yahoo.com", 80),
                new HttpHost("www.apache.com", 80)
        };

        class WorkerThread extends Thread {

            private final HttpHost target;

            WorkerThread(final HttpHost target) {
                super();
                this.target = target;
            }

            @Override
            public void run() {
                ConnectionReuseStrategy connStrategy = DefaultConnectionReuseStrategy.INSTANCE;
                try {
                    Future<BasicPoolEntry> future = pool.lease(this.target, null);

                    boolean reusable = false;
                    BasicPoolEntry entry = future.get();
                    try {
                        HttpClientConnection conn = entry.getConnection();
                        HttpCoreContext coreContext = HttpCoreContext.create();
                        coreContext.setTargetHost(this.target);

                        BasicHttpRequest request = new BasicHttpRequest("GET", "/");
                        System.out.println(">> Request URI: " + request.getRequestLine().getUri());

                        httpexecutor.preProcess(request, httpproc, coreContext);
                        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
                        httpexecutor.postProcess(response, httpproc, coreContext);

                        System.out.println("<< Response: " + response.getStatusLine());
                        System.out.println(EntityUtils.toString(response.getEntity()));

                        reusable = connStrategy.keepAlive(response, coreContext);
                    } catch (IOException ex) {
                        throw ex;
                    } catch (HttpException ex) {
                        throw ex;
                    } finally {
                        if (reusable) {
                            System.out.println("Connection kept alive...");
                        }
                        pool.release(entry, reusable);
                    }
                } catch (Exception ex) {
                    System.out.println("Request to " + this.target + " failed: " + ex.getMessage());
                }
            }

        };

        WorkerThread[] workers = new WorkerThread[targets.length];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new WorkerThread(targets[i]);
        }
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }
        for (int i = 0; i < workers.length; i++) {
            workers[i].join();
        }
    }

}










///*
// * ====================================================================
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// * ====================================================================
// *
// * This software consists of voluntary contributions made by many
// * individuals on behalf of the Apache Software Foundation.  For more
// * information on the Apache Software Foundation, please see
// * <http://www.apache.org/>.
// *
// */
//
//
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Future;
//
//import org.apache.hc.core5.concurrent.FutureCallback;
//import org.apache.hc.core5.http.Header;
//import org.apache.hc.core5.http.HttpConnection;
//import org.apache.hc.core5.http.HttpHost;
//import org.apache.hc.core5.http.HttpResponse;
//import org.apache.hc.core5.http.Message;
//import org.apache.hc.core5.http.Method;
//import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncRequester;
//import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
//import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
//import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
//import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;
//import org.apache.hc.core5.http2.HttpVersionPolicy;
//import org.apache.hc.core5.http2.config.H2Config;
//import org.apache.hc.core5.http2.frame.RawFrame;
//import org.apache.hc.core5.http2.impl.nio.H2StreamListener;
//import org.apache.hc.core5.http2.impl.nio.bootstrap.H2RequesterBootstrap;
//import org.apache.hc.core5.io.CloseMode;
//import org.apache.hc.core5.util.Timeout;
//
///**
// * Example of HTTP/2 request execution.
// */
//public class H2RequestExecutionExample {
//
//    public static void main(final String[] args) throws Exception {
//
//        // Create and start requester
//        final H2Config h2Config = H2Config.custom()
//                .setPushEnabled(false)
//                .build();
//
//        final HttpAsyncRequester requester = H2RequesterBootstrap.bootstrap()
//                .setH2Config(h2Config)
//                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2)
//                .setStreamListener(new H2StreamListener() {
//
//                    @Override
//                    public void onHeaderInput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
//                        for (int i = 0; i < headers.size(); i++) {
//                            System.out.println(connection.getRemoteAddress() + " (" + streamId + ") << " + headers.get(i));
//                        }
//                    }
//
//                    @Override
//                    public void onHeaderOutput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
//                        for (int i = 0; i < headers.size(); i++) {
//                            System.out.println(connection.getRemoteAddress() + " (" + streamId + ") >> " + headers.get(i));
//                        }
//                    }
//
//                    @Override
//                    public void onFrameInput(final HttpConnection connection, final int streamId, final RawFrame frame) {
//                    }
//
//                    @Override
//                    public void onFrameOutput(final HttpConnection connection, final int streamId, final RawFrame frame) {
//                    }
//
//                    @Override
//                    public void onInputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
//                    }
//
//                    @Override
//                    public void onOutputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
//                    }
//
//                })
//                .create();
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                System.out.println("HTTP requester shutting down");
//                requester.close(CloseMode.GRACEFUL);
//            }
//        });
//        requester.start();
//
//        final HttpHost target = new HttpHost("nghttp2.org");
//        final String[] requestUris = new String[] {"/httpbin/ip", "/httpbin/user-agent", "/httpbin/headers"};
//
//        final CountDownLatch latch = new CountDownLatch(requestUris.length);
//        for (final String requestUri: requestUris) {
//            final Future<AsyncClientEndpoint> future = requester.connect(target, Timeout.ofSeconds(5));
//            final AsyncClientEndpoint clientEndpoint = future.get();
//            clientEndpoint.execute(
//                    new BasicRequestProducer(Method.GET, target, requestUri),
//                    new BasicResponseConsumer<>(new StringAsyncEntityConsumer()),
//                    new FutureCallback<Message<HttpResponse, String>>() {
//
//                        @Override
//                        public void completed(final Message<HttpResponse, String> message) {
//                            clientEndpoint.releaseAndReuse();
//                            final HttpResponse response = message.getHead();
//                            final String body = message.getBody();
//                            System.out.println(requestUri + "->" + response.getCode());
//                            System.out.println(body);
//                            latch.countDown();
//                        }
//
//                        @Override
//                        public void failed(final Exception ex) {
//                            clientEndpoint.releaseAndDiscard();
//                            System.out.println(requestUri + "->" + ex);
//                            latch.countDown();
//                        }
//
//                        @Override
//                        public void cancelled() {
//                            clientEndpoint.releaseAndDiscard();
//                            System.out.println(requestUri + " cancelled");
//                            latch.countDown();
//                        }
//
//                    });
//        }
//
//        latch.await();
//        System.out.println("Shutting down I/O reactor");
//        requester.initiateShutdown();
//    }
//
//}