package com.hazelcast.internal.restng;

import static com.hazelcast.util.StringUtil.stringToBytes;

import java.util.ArrayList;
import java.util.List;

public class DefaultHttpResponse implements HttpResponse {

    private final HttpStatus status;
    private final HttpRequest request;
    private final byte[] body;
    private final List<HttpHeader> headers = new ArrayList<>();

    public DefaultHttpResponse(HttpRequest req, HttpStatus status) {
        this(req, status, null);
    }

    public DefaultHttpResponse(HttpRequest req, HttpStatus status, byte[] body) {
        this.request = req;
        this.status = status;
        if (body == null) {
            this.body = status.includeBody() ? stringToBytes(status.description()) : HttpUtils.EMPTY;
        } else {
            this.body = body;
        }
    }

    @Override
    public List<HttpHeader> headers() {
        return headers;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public byte[] body() {
        return body;
    }

    @Override
    public HttpRequest request() {
        return request;
    }

}
