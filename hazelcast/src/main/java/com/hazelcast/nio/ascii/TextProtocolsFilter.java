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

import java.util.StringTokenizer;

import com.hazelcast.config.RestApiConfig;
import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.internal.ascii.rest.HttpCommandProcessor;
import com.hazelcast.nio.tcp.TcpIpConnection;

/**
 * This class is a text protocols policy enforcement point. It checks incoming command lines and validates if the command can be
 * processed. If the command is unknown or not allowed the connection is closed.
 */
class TextProtocolsFilter {

    private final RestApiConfig restApiConfig;

    TextProtocolsFilter(RestApiConfig restApiConfig) {
        this.restApiConfig = restApiConfig;
    }

    void filterConnection(String commandLine, TcpIpConnection connection) {
        RestEndpointGroup restEndpointGroup = getEndpointGroup(commandLine);
        if (restEndpointGroup != null) {
            if (!restApiConfig.isGroupEnabled(restEndpointGroup)) {
                connection.close("REST endpoint group is not enabled - " + restEndpointGroup, null);
            }
        } else if (!commandLine.isEmpty()) {
            connection.close("Unsupported command received on Text protocols handler.", null);
        }
    }

    /**
     * Parses given command line and return corresponding {@link RestEndpointGroup} instance, or {@code null} if no such is
     * found.
     */
    private RestEndpointGroup getEndpointGroup(String commandLine) {
        if (commandLine == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(commandLine);
        if (!st.hasMoreTokens()) {
            return null;
        }
        String operation = st.nextToken();
        // If the operation doesn't have a parser, then it's unknown.
        if (!TextDecoder.MAP_COMMAND_PARSERS.containsKey(operation)) {
            return null;
        }
        // If the operation is not an HTTP method name, then it has to be a Memcache command
        if (!isHttpMethod(operation)) {
            return RestEndpointGroup.MEMCACHE;
        }
        // the operation is a HTTP method so the next token should be a resource path
        if (!st.hasMoreTokens()) {
            return null;
        }
        return getHttpApiEndpointGroup(operation, st.nextToken());
    }

    @SuppressWarnings({"checkstyle:cyclomaticcomplexity", "checkstyle:npathcomplexity"})
    private RestEndpointGroup getHttpApiEndpointGroup(String operation, String requestUri) {
        if (requestUri.startsWith(HttpCommandProcessor.URI_MAPS)
                || requestUri.startsWith(HttpCommandProcessor.URI_QUEUES)) {
            return RestEndpointGroup.DATA;
        }
        if (requestUri.startsWith(HttpCommandProcessor.URI_HEALTH_URL)) {
            return RestEndpointGroup.HEALTH_CHECK;
        }
        if (requestUri.startsWith(HttpCommandProcessor.URI_MANCENTER_BASE_URL + "/wan/")
                || requestUri.startsWith("/hazelcast/rest/wan/")
                || requestUri.startsWith(HttpCommandProcessor.LEGACY_URI_MANCENTER_WAN_CLEAR_QUEUES)) {
            return RestEndpointGroup.WAN;
        }
        if (requestUri.startsWith(HttpCommandProcessor.URI_FORCESTART_CLUSTER_URL)
                || requestUri.startsWith(HttpCommandProcessor.URI_PARTIALSTART_CLUSTER_URL)
                || requestUri.startsWith(HttpCommandProcessor.URI_HOT_RESTART_BACKUP_CLUSTER_URL)
                || requestUri.startsWith(HttpCommandProcessor.URI_HOT_RESTART_BACKUP_INTERRUPT_CLUSTER_URL)) {
            return RestEndpointGroup.HOT_RESTART;
        }
        if (requestUri.startsWith(HttpCommandProcessor.URI_CLUSTER)
                || requestUri.startsWith(HttpCommandProcessor.URI_CLUSTER_STATE_URL)
                || requestUri.startsWith(HttpCommandProcessor.URI_CLUSTER_NODES_URL)
                || ("GET".equals(operation) && requestUri.startsWith(HttpCommandProcessor.URI_CLUSTER_VERSION_URL))) {
            return RestEndpointGroup.CLUSTER_READ;
        }
        if (requestUri.startsWith("/hazelcast/")) {
            return RestEndpointGroup.CLUSTER_WRITE;
        }
        return null;
    }

    private boolean isHttpMethod(String operation) {
        return "GET".equals(operation) || "HEAD".equals(operation) || "POST".equals(operation) || "PUT".equals(operation)
                || "DELETE".equals(operation);
    }
}
