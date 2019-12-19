/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.nio.tcp;

import static com.hazelcast.test.HazelcastTestSupport.assertClusterSize;
import static com.hazelcast.test.HazelcastTestSupport.smallInstanceConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.spi.properties.ClusterProperty;
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
        assumeLocalhostResolvesTo_127_0_0_1();
        HazelcastInstance hz1 = newMember("127.0.0.1");
        try {
            HazelcastInstance hz2 = newMember("localhost");
            assertClusterSize(1, hz1, hz2);
        } finally {
            Hazelcast.shutdownAll();
        }
    }

    @Test
    public void test() {
        assumeLocalhostResolvesTo_127_0_0_1();
        HazelcastInstance hz1 = newMember2("127.0.0.1");
        try {
            HazelcastInstance hz2 = newMember2("localhost");
            assertClusterSize(2, hz1, hz2);
        } finally {
            Hazelcast.shutdownAll();
        }
    }

    private static HazelcastInstance newMember(String hostname) {
        Config config = smallInstanceConfig().setProperty(ClusterProperty.MAX_JOIN_SECONDS.getName(), "5");
        config.setClusterName(hostname);
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).clear().addMember(hostname);
        return Hazelcast.newHazelcastInstance(config);
    }

    private static HazelcastInstance newMember2(String hostname) {
        Config config = smallInstanceConfig()
//                .setProperty(ClusterProperty.MAX_JOIN_SECONDS.getName(), "5")
                ;
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).clear().addMember(hostname);
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Throws {@link AssumptionViolatedException} if the "localhost" hostname doesn't resolve to 127.0.0.1.
     */
    public static void assumeLocalhostResolvesTo_127_0_0_1() {
        boolean resolvedToLoopback = false;
        try {
            InetAddress ia = InetAddress.getByName("localhost");
            resolvedToLoopback = "127.0.0.1".equals(ia.getHostAddress());
        } catch (UnknownHostException e) {
            // OK
        }
        Assume.assumeTrue("The localhost doesn't resolve to 127.0.0.1. Skipping the test.", resolvedToLoopback);
    }


}
