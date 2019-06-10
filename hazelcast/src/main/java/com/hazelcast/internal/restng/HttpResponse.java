package com.hazelcast.internal.restng;

import java.util.List;

public interface HttpResponse {
    List<HttpHeader> headers();
    HttpStatus status();
    byte[] body();

    HttpRequest request();
}
