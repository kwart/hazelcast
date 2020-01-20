package com.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

public class TestApp {

    public static void main(String args[]) {
        Hazelcast.newHazelcastInstance(new Config());
        HazelcastClient.newHazelcastClient();
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }
}
