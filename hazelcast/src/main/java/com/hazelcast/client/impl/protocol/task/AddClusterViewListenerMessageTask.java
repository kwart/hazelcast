/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.impl.protocol.task;

import com.hazelcast.client.impl.ClusterViewListenerService;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ClientAddClusterViewListenerCodec;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.cluster.impl.ClusterServiceImpl;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.internal.util.UuidUtil;

import java.security.Permission;
import java.time.LocalTime;

public class AddClusterViewListenerMessageTask
        extends AbstractCallableMessageTask<ClientAddClusterViewListenerCodec.RequestParameters> {

    public AddClusterViewListenerMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected boolean acceptOnIncompleteStart() {
        return true;
    }

    @Override
    protected Object call() {
        System.out.println("Registering " + endpoint + ", time=" + LocalTime.now());
        ClusterViewListenerService service = clientEngine.getClusterListenerService();
        service.registerListener(endpoint, clientMessage.getCorrelationId());
        endpoint.addDestroyAction(UuidUtil.newUnsecureUUID(), () -> {
            System.out.println("Deregistering " + endpoint + ", time=" + LocalTime.now());
            service.deregisterListener(endpoint);
            System.out.println("Deregistered " + endpoint + ", time=" + LocalTime.now());
            return Boolean.TRUE;
        });

        return true;
    }

    @Override
    protected ClientAddClusterViewListenerCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return ClientAddClusterViewListenerCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return ClientAddClusterViewListenerCodec.encodeResponse();
    }

    @Override
    public String getServiceName() {
        return ClusterServiceImpl.SERVICE_NAME;
    }

    @Override
    public String getDistributedObjectName() {
        return null;
    }

    @Override
    public String getMethodName() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    public Permission getRequiredPermission() {
        return null;
    }

}
