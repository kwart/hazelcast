package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE;
import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE_JSON;
import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE_OCTET_STREAM;
import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE_PLAIN_TEXT;
import static com.hazelcast.internal.restng.HttpUtils.trimQueryParams;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NO_CONTENT_204;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.OK_200;
import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.ascii.rest.RestValue;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.DefaultHttpHeader;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

public abstract class AbstractDataHttpHandler extends AbstractHttpHandler {

    public AbstractDataHttpHandler(Node node, String factoryUri) {
        super(node, factoryUri);
    }

    protected HttpResponse valueToResponse(HttpRequest req, Object value) {
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

    protected NameAndParam getNameAndParam(HttpRequest req, boolean paramRequired) {
        String dataUri = trimQueryParams(trimFactoryUri(req));
        int pos = dataUri.indexOf('/');
        NameAndParam nameAndParam = null;
        if (pos > -1) {
            nameAndParam = new NameAndParam(dataUri.substring(0, pos), dataUri.substring(pos + 1));
        } else if (!paramRequired) {
            nameAndParam = new NameAndParam(dataUri, null);
        }
        return nameAndParam;
    }

    protected static class NameAndParam {
        private final String name;
        private final String param;

        public NameAndParam(String name, String param) {
            this.name = name;
            this.param = param;
        }

        public String name() {
            return name;
        }

        public String param() {
            return param;
        }

        public Integer paramAsInteger() {
            if (param==null || param.isEmpty()) {
                return null;
            }
            try {
                return new Integer(param);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

}