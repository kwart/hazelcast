package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.config.RestEndpointGroup.CLUSTER_READ;
import static com.hazelcast.config.RestEndpointGroup.CLUSTER_WRITE;
import static com.hazelcast.internal.restng.HttpUtils.URI_BASE_CLUSTER_MANAGEMENT;
import static com.hazelcast.internal.restng.HttpUtils.bodyAsString;
import static com.hazelcast.internal.restng.HttpUtils.createOkTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.createServerErrorTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.getHttpMethod;
import static com.hazelcast.internal.restng.HttpUtils.HttpMethod.GET;

import org.kohsuke.MetaInfServices;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;
import com.hazelcast.internal.restng.HttpUtils;
import com.hazelcast.version.Version;

@MetaInfServices
public class ClusterVersionHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return URI_BASE_CLUSTER_MANAGEMENT + "/version";
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
            return getHttpMethod(req) == GET ? CLUSTER_READ : CLUSTER_WRITE;
        }

        @Override
        protected HttpResponse doGet(HttpRequest req) {
            return createOkTextResponse(req, node.getClusterService().getClusterVersion().toString());
        }

        @Override
        protected HttpResponse doPost(HttpRequest req) {
            String versionStr = bodyAsString(req);
            Version version;
            try {
                version = Version.of(versionStr);
            } catch (Exception e) {
                return HttpUtils.createBadRequestTextResponse(req, "Wrong version :" + versionStr);
            }
            try {
                node.getClusterService().changeClusterVersion(version);
                return createOkTextResponse(req, null);
            } catch (Exception e) {
                return createServerErrorTextResponse(req, e.getMessage());
            }
        }

    }
}
