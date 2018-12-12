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

package com.hazelcast.nio.ascii;

import static com.hazelcast.test.HazelcastTestSupport.getAddress;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.hazelcast.config.Config;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.QuickTest;

/**
 * Tests enabling text protocols by {@link RestApiConfig}.
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class TextProtocolsFilterTest extends AbstractRestApiConfigTestBase {

    /**
     * <pre>
     * Given: RestApiConfig is explicitly disabled
     * When: a custom text protocol is used by client
     * Then: connection is terminated after reading the first 3 bytes (protocol header)
     * </pre>
     */
    @Test
    public void testRestApiDisabled() throws Exception {
        Config config = new Config();
        config.setRestApiConfig(new RestApiConfig().setEnabled(false));
        HazelcastInstance hz = factory.newHazelcastInstance(config);
        TextProtocolClient client = new TextProtocolClient(getAddress(hz).getInetSocketAddress());
        try {
            client.connect();
            client.sendData("ABC");
            client.waitUntilClosed();
            assertEquals(3, client.getSentBytesCount());
            assertEquals(0, client.getReceivedBytes().length);
            assertTrue(client.isConnectionClosed());
        } finally {
            client.close();
        }
    }

    /**
     * <pre>
     * Given: RestApiConfig is explicitly enabled
     * When: a custom text protocol is used by client
     * Then: connection is not terminated after reading the first 3 bytes (protocol header)
     * </pre>
     */
    @Test
    public void testRestApiDisabledByDefault() throws Exception {
        HazelcastInstance hz = factory.newHazelcastInstance(null);
        TextProtocolClient client = new TextProtocolClient(getAddress(hz).getInetSocketAddress());
        try {
            client.connect();
            client.sendData("ABC");
            client.waitUntilClosed();
            assertTrue(client.isConnectionClosed());
            assertEmptyString(client.getReceivedString());
        } finally {
            client.close();
        }
    }

    /**
     * <pre>
     * Given: RestApiConfig is explicitly enabled
     * When: a custom text protocol is used by client
     * Then: the connection is terminated after reading an unknown command line
     * </pre>
     */
    @Test
    public void testRestApiEnabled() throws Exception {
        Config config = new Config();
        config.setRestApiConfig(new RestApiConfig().setEnabled(true));
        HazelcastInstance hz = factory.newHazelcastInstance(config);
        TextProtocolClient client = new TextProtocolClient(getAddress(hz).getInetSocketAddress());
        try {
            client.connect();
            client.sendData("ABC");
            client.waitUntilClosed(2000);
            client.sendData(CRLF);
            client.waitUntilClosed();
            assertTrue(client.isConnectionClosed());
            assertEmptyString(client.getReceivedString());
        } finally {
            client.close();
        }
    }
}
