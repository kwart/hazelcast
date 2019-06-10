package com.hazelcast.internal.restng;

public interface HttpRequestHandler {
    HttpResponse doGet(HttpRequest request);
}
