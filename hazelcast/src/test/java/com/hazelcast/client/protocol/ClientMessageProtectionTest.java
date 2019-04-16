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

package com.hazelcast.client.protocol;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ClientAuthenticationCodec;
import com.hazelcast.client.impl.protocol.util.ClientMessageDecoder;
import com.hazelcast.client.impl.protocol.util.ClientMessageEncoder;
import com.hazelcast.client.impl.protocol.util.ClientMessageSplitter;
import com.hazelcast.client.test.TestAwareClientFactory;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.BuildInfo;
import com.hazelcast.instance.BuildInfoProvider;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.internal.util.counters.SwCounter;
import com.hazelcast.nio.Connection;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestAwareInstanceFactory;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.hazelcast.nio.IOUtil.readFully;
import static com.hazelcast.test.HazelcastTestSupport.getNode;
import static com.hazelcast.test.HazelcastTestSupport.smallInstanceConfig;
import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class})
public class ClientMessageProtectionTest {

    private final TestAwareInstanceFactory factory = new TestAwareInstanceFactory();

    @After
    public void after() {
        factory.terminateAll();
    }

    @Test
    public void splitAndBuild() {
        Config config = smallInstanceConfig();
        HazelcastInstance hz = factory.newHazelcastInstance(config);
        int FRAME_SIZE = 50;
        String passwd = UUID.randomUUID().toString() + UUID.randomUUID().toString();
//        Object authentication(String username, String password, @Nullable String uuid, @Nullable String ownerUuid,
//                boolean isOwnerConnection, String clientType, byte serializationVersion,
//                @Since(value = "1.3") String clientHazelcastVersion, @Since(value = "1.8") String clientName,
//                @Since(value = "1.8") List<String> labels,
//                @Since(value = "1.8") @Nullable Integer partitionCount,
//                @Since(value = "1.8") @Nullable String clusterId);
        final ClientMessage clientMessage = ClientAuthenticationCodec.encodeRequest(config.getGroupConfig().getName(),
                passwd, null, null, false, "FOO", (byte) 1, "abc", "xxx", null, null, null);
        clientMessage.addFlag(ClientMessage.BEGIN_AND_END_FLAGS);

        InetSocketAddress address = getNode(hz).getLocalMember().getSocketAddress(EndpointQualifier.CLIENT);
        Socket socket = new Socket(address.getAddress(), address.getPort());
        socket.setSoTimeout(5000);
        try {
            OutputStream os = socket.getOutputStream();
            os.write(clientMessage.getByteArray());
            os.flush();
            readFully(socket.getInputStream(), response);
        } finally {
            socket.close();
        }
    }

}
