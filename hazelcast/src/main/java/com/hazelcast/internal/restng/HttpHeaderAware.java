package com.hazelcast.internal.restng;

public interface HttpHeaderAware {

    void addHeader(String fieldName, String value);
}
