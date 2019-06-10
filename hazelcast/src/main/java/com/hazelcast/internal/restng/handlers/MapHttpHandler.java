package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.internal.restng.HttpUtils.*;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NOT_FOUND_404;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NO_CONTENT_204;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.OK_200;
import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.instance.Node;
import com.hazelcast.internal.ascii.rest.RestValue;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.DefaultHttpHeader;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

public class MapHttpHandler extends AbstractHttpHandler {

    public MapHttpHandler(Node node, String factoryUri) {
        super(node, factoryUri);
    }

    @Override
    public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
        return RestEndpointGroup.DATA;
    }

    @Override
    protected HttpResponse doGet(HttpRequest req) {
        MapAndKey mk = getMapAndKey(req, true);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        return valueToResponse(req, node.hazelcastInstance.getMap(mk.map).get(mk.key));
    }

    @Override
    protected HttpResponse doPost(HttpRequest req) {
        MapAndKey mk = getMapAndKey(req, true);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        String contentType = req.getHeaderValue(CONTENT_TYPE);
        node.hazelcastInstance.getMap(mk.map).put(mk.key, new RestValue(req.body(), contentType));
        return new DefaultHttpResponse(req, OK_200);
    }

    @Override
    protected HttpResponse doDelete(HttpRequest req) {
        MapAndKey mk = getMapAndKey(req, false);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        IMap map = node.hazelcastInstance.getMap(mk.map);
        if (mk.key == null) {
            map.clear();
        } else {
            map.delete(mk.key);
        }
        return new DefaultHttpResponse(req, OK_200);
    }

    private HttpResponse valueToResponse(HttpRequest req, Object value) {
        if (value == null) {
            return new DefaultHttpResponse(req, NO_CONTENT_204);
        }
        byte[] body;
        String contentType;
        if (value instanceof byte[]) {
            contentType = CONTENT_TYPE_OCTET_STREAM;
            body = (byte[]) value;
        } else if (value instanceof RestValue) {
            RestValue restValue = (RestValue) value;
            contentType = bytesToString(restValue.getContentType());
            body = restValue.getValue();
        } else if (value instanceof HazelcastJsonValue) {
            contentType = CONTENT_TYPE_JSON;
            body = stringToBytes(value.toString());
        } else if (value instanceof String) {
            contentType = CONTENT_TYPE_PLAIN_TEXT;
            body = stringToBytes((String) value);
        } else {
            contentType = CONTENT_TYPE_OCTET_STREAM;
            body = node.getSerializationService().toData(value).toByteArray();
        }
        HttpResponse response = new DefaultHttpResponse(req, OK_200, body);
        response.headers().add(new DefaultHttpHeader(CONTENT_TYPE, contentType));
        return response;
    }

    private MapAndKey getMapAndKey(HttpRequest req, boolean keyRequired) {
        String mapUri = trimQueryParams(trimFactoryUri(req));
        int pos = mapUri.indexOf('/');
        MapAndKey mk = null;
        if (pos > -1) {
            mk = new MapAndKey();
            mk.map = mapUri.substring(0, pos);
            mk.key = mapUri.substring(pos + 1);
        } else if (!keyRequired) {
            mk = new MapAndKey();
            mk.map = mapUri;
        }
        return mk;
    }

    private static class MapAndKey {
        private String map;
        private String key;
    }
}
