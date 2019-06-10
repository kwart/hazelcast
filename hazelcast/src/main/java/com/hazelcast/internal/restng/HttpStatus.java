package com.hazelcast.internal.restng;

public interface HttpStatus {

    int code();

    String description();

    boolean includeBody();

}