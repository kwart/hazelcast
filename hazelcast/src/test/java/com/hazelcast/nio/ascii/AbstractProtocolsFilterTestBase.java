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

import static com.hazelcast.test.HazelcastTestSupport.assertTrueEventually;
import static com.hazelcast.test.HazelcastTestSupport.getAddress;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.hazelcast.config.Config;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.test.AssertTask;
import com.hazelcast.test.OverridePropertyRule;
import com.hazelcast.test.TestAwareInstanceFactory;

/**
 * Shared code for {@link RestApiConfig} and {@link TextProtocolsFilter} testing.
 */
public abstract class AbstractProtocolsFilterTestBase {

    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public static final String CRLF = "\r\n";

    protected final TestAwareInstanceFactory factory = new TestAwareInstanceFactory();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    @SuppressWarnings("deprecation")
    public OverridePropertyRule restEnabled = OverridePropertyRule.clear(GroupProperty.REST_ENABLED.getName());
    @Rule
    @SuppressWarnings("deprecation")
    public OverridePropertyRule memcacheEnabled = OverridePropertyRule.clear(GroupProperty.MEMCACHE_ENABLED.getName());
    @Rule
    @SuppressWarnings("deprecation")
    public OverridePropertyRule healthCheckEnabled = OverridePropertyRule
            .clear(GroupProperty.HTTP_HEALTHCHECK_ENABLED.getName());

    @After
    public void cleanup() {
        factory.terminateAll();
    }

    /**
     * Creates Hazelcast {@link Config} with enabled all but provided {@link RestEndpointGroup RestEndpointGroups}.
     */
    protected Config createConfigWithDisabledGroups(RestEndpointGroup... group) {
        RestApiConfig restApiConfig = new RestApiConfig();
        restApiConfig.setEnabled(true);
        restApiConfig.enableAllGroups().disableGroups(group);
        return new Config().setRestApiConfig(restApiConfig);
    }

    /**
     * Creates Hazelcast {@link Config} with disabled all but provided {@link RestEndpointGroup RestEndpointGroups}.
     */
    protected Config createConfigWithEnabledGroups(RestEndpointGroup... group) {
        RestApiConfig restApiConfig = new RestApiConfig();
        restApiConfig.setEnabled(true);
        restApiConfig.disableAllGroups().enableGroups(group);
        return new Config().setRestApiConfig(restApiConfig);
    }

    protected static void assertEmptyString(String stringToCheck) {
        if (stringToCheck != null && !stringToCheck.isEmpty()) {
            fail("Empty string was expected, but got '" + stringToCheck + "'");
        }
    }

    /**
     * Asserts that a text protocol client call to given {@link TestUrl} returns an expected response.
     */
    protected void assertTextProtocolResponse(HazelcastInstance hz, TestUrl testUrl) throws UnknownHostException, IOException {
        final TextProtocolClient client = new TextProtocolClient(getAddress(hz).getInetSocketAddress());
        try {
            client.connect();
            if (testUrl.restEndpointGroup == RestEndpointGroup.MEMCACHE) {
                client.sendData(testUrl.method + " " + testUrl.requestUri + "\n");
            } else {
                client.sendData(testUrl.method + " " + testUrl.requestUri + " HTTP/1.0" + CRLF + CRLF);
            }
            assertTrueEventually(createResponseAssertTask(client, testUrl), 10);
        } finally {
            client.close();
        }
    }

    /**
     * Asserts that a wrong text protocol method call using given {@link TestUrl} closes the connection without returning any
     * response (e.g. a call to a disabled REST API).
     */
    protected void assertNoTextProtocolResponse(HazelcastInstance hz, TestUrl testUrl)
            throws UnknownHostException, InterruptedException, IOException {
        TextProtocolClient client = new TextProtocolClient(getAddress(hz).getInetSocketAddress());
        try {
            client.connect();
            client.sendData(testUrl.method + " " + testUrl.requestUri + " HTTP/1.0" + CRLF);
            client.waitUntilClosed();
            assertTrue("Connection close was expected (from server side). " + testUrl, client.isConnectionClosed());
            String receivedResponse = client.getReceivedString();
            if (receivedResponse != null && !receivedResponse.isEmpty()) {
                fail("Empty response was expected, but got '" + receivedResponse + "'. " + testUrl);
            }
        } finally {
            client.close();
        }
    }

    protected AssertTask createResponseAssertTask(final TextProtocolClient client, final TestUrl testUrl) {
        return new AssertTask() {
            @Override
            public void run() throws Exception {
                assertThat(testUrl.toString(), client.getReceivedString(), containsString(testUrl.expectedSubstring));
            }
        };
    }

    static class TestUrl {
        final RestEndpointGroup restEndpointGroup;
        final String method;
        final String requestUri;
        final String expectedSubstring;

        public TestUrl(RestEndpointGroup restEndpointGroup, String httpMethod, String requestUri, String expectedSubstring) {
            this.restEndpointGroup = restEndpointGroup;
            this.method = httpMethod;
            this.requestUri = requestUri;
            this.expectedSubstring = expectedSubstring;
        }

        @Override
        public String toString() {
            return "TestUrl [restEndpointGroup=" + restEndpointGroup + ", method=" + method + ", requestUri=" + requestUri
                    + ", expectedSubstring=" + expectedSubstring + "]";
        }
    }
}
