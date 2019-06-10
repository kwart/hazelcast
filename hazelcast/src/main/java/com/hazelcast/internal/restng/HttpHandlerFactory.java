package com.hazelcast.internal.restng;

import com.hazelcast.instance.Node;

public interface HttpHandlerFactory {
    String uri();
    boolean uriIsPrefix();
    HttpHandler create(Node node);
}
