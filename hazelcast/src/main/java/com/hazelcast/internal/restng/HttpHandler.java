package com.hazelcast.internal.restng;

import com.hazelcast.config.RestEndpointGroup;

public interface HttpHandler {
    RestEndpointGroup getRestEndpointGroup(HttpRequest req);
    HttpResponse handle(HttpRequest req);
}
