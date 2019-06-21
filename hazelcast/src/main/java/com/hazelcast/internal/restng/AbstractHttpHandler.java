package com.hazelcast.internal.restng;

import static com.hazelcast.internal.restng.HttpUtils.getHttpMethod;

import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.HttpUtils.HttpMethod;
import com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus;

public abstract class AbstractHttpHandler implements HttpHandler {

    protected final String factoryUri;
    protected final Node node;

    public AbstractHttpHandler(Node node, String factoryUri) {
        this.node = node;
        this.factoryUri = factoryUri;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        HttpMethod httpMethod = getHttpMethod(req);
        if (httpMethod == null) {
            return new DefaultHttpResponse(req, WellKnownHttpStatus.NOT_IMPLEMENTED_501);
        }
        switch (httpMethod) {
            case GET:
                return doGet(req);
            case POST:
                return doPost(req);
            case PUT:
                return doPut(req);
            case DELETE:
                return doDelete(req);
            case HEAD:
                return doHead(req);
            case TRACE:
                return doTrace(req);
            case OPTIONS:
                return doOptions(req);
            default:
                return new DefaultHttpResponse(req, WellKnownHttpStatus.NOT_IMPLEMENTED_501);
        }
    }

    protected abstract HttpResponse doGet(HttpRequest req);

    protected HttpResponse doPost(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.METHOD_NOT_ALLOWED_405);
    }

    protected HttpResponse doPut(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.METHOD_NOT_ALLOWED_405);
    }

    protected HttpResponse doDelete(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.METHOD_NOT_ALLOWED_405);
    }

    protected HttpResponse doHead(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.OK_200);
    }

    protected HttpResponse doTrace(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.METHOD_NOT_ALLOWED_405);
    }

    protected HttpResponse doOptions(HttpRequest req) {
        return new DefaultHttpResponse(req, WellKnownHttpStatus.METHOD_NOT_ALLOWED_405);
    }

    protected String trimFactoryUri(HttpRequest req) {
        int prefixLength = factoryUri.length() + 1;
        String uri = req.uri();
        return uri.length() >= prefixLength ? uri.substring(prefixLength) : "";
    }
}
