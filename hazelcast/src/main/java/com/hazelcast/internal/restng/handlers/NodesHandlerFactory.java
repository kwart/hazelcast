package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.config.RestEndpointGroup.CLUSTER_READ;
import static com.hazelcast.internal.restng.HttpUtils.URI_BASE_CLUSTER_MANAGEMENT;
import static com.hazelcast.internal.restng.HttpUtils.createOkTextResponse;

import org.kohsuke.MetaInfServices;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

@MetaInfServices
public class NodesHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return URI_BASE_CLUSTER_MANAGEMENT + "/nodes";
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
            return CLUSTER_READ;
        }

        @Override
        protected HttpResponse doGet(HttpRequest req) {
            return createOkTextResponse(req, 
                    node.getClusterService().getMembers().toString() + "\n"
                            + node.getBuildInfo().getVersion() + "\n"
                            + System.getProperty("java.version"));
        }
    }
}
