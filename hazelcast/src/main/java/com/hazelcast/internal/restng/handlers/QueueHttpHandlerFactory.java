package com.hazelcast.internal.restng.handlers;

import org.kohsuke.MetaInfServices;

import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;

@MetaInfServices
public class QueueHttpHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return "/hazelcast/rest/queues";
    }

    @Override
    public HttpHandler create(Node node) {
        return new QueueHttpHandler(node, uri());
    }

    @Override
    public boolean uriIsPrefix() {
        return true;
    }

}
