package com.hazelcast.internal.restng;

import java.util.List;

public interface HttpRequest {
    String method();
    String uri();
    String protocol();
    List<HttpHeader> headers();
    String getHeaderValue(String name);
    String[] getHeaderValues(String name);

    long contentLength();

    byte[] body();
    void body(byte[] body);

    AuthenticationStatus authenticationStatus();
    void authenticationStatus(AuthenticationStatus authenticationStatus);

    boolean processingDone();
    void flagProcessingDone();
}
