package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.config.RestEndpointGroup.CLUSTER_WRITE;
import static com.hazelcast.internal.restng.HttpUtils.URI_BASE_CLUSTER_MANAGEMENT;
import static com.hazelcast.internal.restng.HttpUtils.createOkTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.createServerErrorTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.METHOD_NOT_ALLOWED_405;

import org.kohsuke.MetaInfServices;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

@MetaInfServices
public class MemberShutdownHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return URI_BASE_CLUSTER_MANAGEMENT + "/memberShutdown";
    }

    @Override
    public HttpHandler create(Node node) {
        return new Handler(node, uri());
    }

    @Override
    public boolean uriIsPrefix() {
        return false;
    }

    private static class Handler extends AbstractHttpHandler {

        public Handler(Node node, String factoryUri) {
            super(node, factoryUri);
        }

        @Override
        public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
            return CLUSTER_WRITE;
        }

        @Override
        protected HttpResponse doGet(HttpRequest req) {
            return new DefaultHttpResponse(req, METHOD_NOT_ALLOWED_405);
        }

        @Override
        protected HttpResponse doPost(HttpRequest req) {
            try {
                node.hazelcastInstance.shutdown();
                return createOkTextResponse(req, null);
            } catch (Exception e) {
                return createServerErrorTextResponse(req, e.getMessage());
            }
        }

    }

}
