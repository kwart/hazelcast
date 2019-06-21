package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NOT_FOUND_404;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.OK_200;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.core.IMap;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.ascii.rest.RestValue;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

public class MapHttpHandler extends AbstractDataHttpHandler {

    public MapHttpHandler(Node node, String factoryUri) {
        super(node, factoryUri);
    }

    @Override
    public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
        return RestEndpointGroup.DATA;
    }

    @Override
    protected HttpResponse doGet(HttpRequest req) {
        NameAndParam mk = getNameAndParam(req, true);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        return valueToResponse(req, node.hazelcastInstance.getMap(mk.name()).get(mk.param()));
    }

    @Override
    protected HttpResponse doPost(HttpRequest req) {
        NameAndParam mk = getNameAndParam(req, true);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        String contentType = req.getHeaderValue(CONTENT_TYPE);
        node.hazelcastInstance.getMap(mk.name()).put(mk.param(), new RestValue(req.body(), contentType));
        return new DefaultHttpResponse(req, OK_200);
    }

    @Override
    protected HttpResponse doDelete(HttpRequest req) {
        NameAndParam mk = getNameAndParam(req, false);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        IMap map = node.hazelcastInstance.getMap(mk.name());
        if (mk.param() == null) {
            map.clear();
        } else {
            map.delete(mk.param());
        }
        return new DefaultHttpResponse(req, OK_200);
    }
}
