package com.hazelcast.internal.restng.handlers;

import static com.hazelcast.internal.restng.HttpUtils.CONTENT_TYPE;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.NOT_FOUND_404;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.OK_200;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.SERVICE_UNAVAILABLE_503;
import static com.hazelcast.util.StringUtil.stringToBytes;
import static java.lang.Thread.currentThread;

import java.util.concurrent.TimeUnit;

import com.hazelcast.collection.IQueue;
import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.ascii.rest.RestValue;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;
import com.hazelcast.internal.restng.HttpUtils;

public class QueueHttpHandler extends AbstractDataHttpHandler {

    public static final String SIZE = "size";

    public QueueHttpHandler(Node node, String factoryUri) {
        super(node, factoryUri);
    }

    @Override
    public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
        return RestEndpointGroup.DATA;
    }

    @Override
    protected HttpResponse doGet(HttpRequest req) {
        NameAndParam mk = getNameAndParam(req, false);
        if (mk == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        Object value = null;
        IQueue<Object> queue = node.hazelcastInstance.getQueue(mk.name());
        if (SIZE.equals(mk.param())) {
            value = Integer.toString(queue.size());
        } else {
            Integer seconds = mk.paramAsInteger();
            if (seconds != null && seconds > 0) {
                try {
                    value = queue.poll(seconds, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                }
            } else {
                value = queue.poll();
            }
        }
        return valueToResponse(req, value);
    }

    @Override
    protected HttpResponse doPost(HttpRequest req) {
        NameAndParam np = getNameAndParam(req, true);
        if (np == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        String contentType = null;
        byte[] value;
        if (np.param() != null && !np.param().isEmpty()) {
            // Path param is a simple text value here
            contentType = HttpUtils.CONTENT_TYPE_PLAIN_TEXT;
            value = stringToBytes(np.param());
        } else {
            contentType = req.getHeaderValue(CONTENT_TYPE);
            value = req.body();
        }
        boolean valueInserted = node.hazelcastInstance.getQueue(np.name()).offer(new RestValue(value, contentType));
        return new DefaultHttpResponse(req, valueInserted ? OK_200 : SERVICE_UNAVAILABLE_503);
    }

    @Override
    protected HttpResponse doDelete(HttpRequest req) {
        NameAndParam np = getNameAndParam(req, false);
        if (np == null) {
            return new DefaultHttpResponse(req, NOT_FOUND_404);
        }
        node.hazelcastInstance.getQueue(np.name()).clear();
        return new DefaultHttpResponse(req, OK_200);
    }
}
