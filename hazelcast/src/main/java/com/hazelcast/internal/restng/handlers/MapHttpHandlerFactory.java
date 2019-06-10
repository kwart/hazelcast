package com.hazelcast.internal.restng.handlers;

import org.kohsuke.MetaInfServices;

import com.hazelcast.instance.Node;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;

@MetaInfServices
public class MapHttpHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return "/hazelcast/rest/maps";
    }

    @Override
    public HttpHandler create(Node node) {
        return new MapHttpHandler(node, uri());
    }

    @Override
    public boolean uriIsPrefix() {
        return true;
    }

}
