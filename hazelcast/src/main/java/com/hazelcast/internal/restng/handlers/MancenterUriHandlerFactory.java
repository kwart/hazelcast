package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.config.RestEndpointGroup.CLUSTER_READ;
import static com.hazelcast.config.RestEndpointGroup.CLUSTER_WRITE;
import static com.hazelcast.internal.restng.HttpUtils.URI_BASE_MANCENTER;
import static com.hazelcast.internal.restng.HttpUtils.getHttpMethod;
import static com.hazelcast.internal.restng.HttpUtils.HttpMethod.GET;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NO_CONTENT_204;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.OK_200;
import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;

import org.kohsuke.MetaInfServices;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.management.ManagementCenterService;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

@MetaInfServices
public class MancenterUriHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return URI_BASE_MANCENTER + "/url";
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
            ManagementCenterService managementCenterService = node.getManagementCenterService();
            String url = managementCenterService != null ? managementCenterService.getManagementCenterUrl() : null;
            byte[] body = url != null ? stringToBytes(url) : null;
            DefaultHttpResponse response = new DefaultHttpResponse(req, body != null ? OK_200 : NO_CONTENT_204, body);
            return response;
        }

        @Override
        protected HttpResponse doPost(HttpRequest req) {
            ManagementCenterService managementCenterService = node.getManagementCenterService();
            byte[] body = req.body();
            DefaultHttpResponse response;
            if (managementCenterService != null && body != null) {
                managementCenterService.clusterWideUpdateManagementCenterUrl(bytesToString(body));
                response = new DefaultHttpResponse(req, OK_200);
            } else {
                response = new DefaultHttpResponse(req, NO_CONTENT_204);
            }
            return response;
        }

    }
}
