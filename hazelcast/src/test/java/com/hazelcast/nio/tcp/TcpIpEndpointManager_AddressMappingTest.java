package com.hazelcast.nio.tcp;

import static com.hazelcast.test.HazelcastTestSupport.assertClusterSize;
import static com.hazelcast.test.HazelcastTestSupport.smallInstanceConfig;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class TcpIpEndpointManager_AddressMappingTest {

    @AfterClass
    public static void afterClass() {
        HazelcastInstanceFactory.terminateAll();
    }

    /**
     * Regression test for https://github.com/hazelcast/hazelcast/issues/15722
     */
    @Test
    public void regression15722() {
        newMember();
        newMember();
//        HazelcastInstance hz1 = newMember();
//        try {
//            assertClusterSize(1, hz1, hz2);
//        } finally {
            Hazelcast.shutdownAll();
//        }
    }

    private static HazelcastInstance newMember() {
        Config config = smallInstanceConfig().setProperty(GroupProperty.MAX_JOIN_SECONDS.getName(), "5");
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).clear().addMember("127.0.0.1");
//        join.getTcpIpConfig().setEnabled(true).clear().addMember("localhost");
//        config.getNetworkConfig().getInterfaces().addInterface("localhost");
        return Hazelcast.newHazelcastInstance(config);
    }
}