package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NOT_FOUND_404;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;

public class HttpHandler404 implements HttpHandler {

    @Override
    public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
        return null;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        return new DefaultHttpResponse(req, NOT_FOUND_404);
    }

}
