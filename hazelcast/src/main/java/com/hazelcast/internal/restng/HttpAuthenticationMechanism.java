package com.hazelcast.internal.restng;

import com.hazelcast.security.Credentials;

public interface HttpAuthenticationMechanism {
    String name();
    void addChallenge(HttpResponse response, String realmName);
    Credentials getCredentials(HttpRequest request);
}
