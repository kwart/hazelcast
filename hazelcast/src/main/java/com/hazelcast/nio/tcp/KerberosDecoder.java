/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.nio.tcp;

import static com.hazelcast.internal.networking.ChannelOption.DIRECT_BUF;
import static com.hazelcast.internal.networking.ChannelOption.SO_RCVBUF;
import static com.hazelcast.internal.networking.ChannelOption.SO_SNDBUF;
import static com.hazelcast.internal.networking.HandlerStatus.CLEAN;
import static com.hazelcast.internal.networking.HandlerStatus.DIRTY;
import static com.hazelcast.nio.ConnectionType.MEMBER;
import static com.hazelcast.nio.IOService.KILO_BYTE;
import static com.hazelcast.nio.IOUtil.compactOrClear;
import static com.hazelcast.nio.IOUtil.newByteBuffer;
import static com.hazelcast.nio.Protocols.CLIENT_BINARY_NEW;
import static com.hazelcast.nio.Protocols.CLUSTER;
import static com.hazelcast.nio.Protocols.PROTOCOL_LENGTH;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_CLIENT_RECEIVE_BUFFER_SIZE;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_RECEIVE_BUFFER_SIZE;
import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.security.auth.Subject;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.MessageProp;

import com.hazelcast.client.impl.protocol.util.ClientMessageDecoder;
import com.hazelcast.internal.networking.ChannelInboundHandler;
import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.nio.IOService;
import com.hazelcast.nio.ascii.TextDecoder;
import com.hazelcast.nio.ascii.TextEncoder;
import com.hazelcast.spi.properties.HazelcastProperties;
import com.hazelcast.util.ExceptionUtil;

/**
 * A {@link ChannelInboundHandler} that reads the protocol bytes
 * {@link com.hazelcast.nio.Protocols} and based on the protocol it creates the
 * appropriate handlers.
 *
 * The ProtocolDecoder doesn't forward to the dst; it replaces itself once the
 * protocol bytes are known. So that is why the Void type for dst.
 */
public class KerberosDecoder extends ChannelInboundHandler<ByteBuffer, ByteBuffer> {

    private final KerberosEncoder kerberosEncoder;
    private final GSSContext context;
    private byte[] activeToken;
    private int activeTokenPos;
    private byte[] toWrite;
    private int toWritePos;

    public KerberosDecoder(GSSContext context, KerberosEncoder protocolEncoder) {
        this.kerberosEncoder = protocolEncoder;
        this.context= context;
    }

    @Override
    public void handlerAdded() {
        initSrcBuffer(2048);
    }

    @Override
    public HandlerStatus onRead() {
        src.flip();

        try {
            if (dst.hasRemaining() && toWrite!=null) {
                int min = Math.min(dst.remaining(), toWrite.length-toWritePos);
                dst.put(toWrite, toWritePos, min);
                toWritePos+=min;
                if (toWritePos>=toWrite.length) {
                     toWritePos = 0;
                     toWrite=null;
                } else {
                    return DIRTY;
                }
            }
            
            if (activeToken==null ) {
                if (src.remaining()<4) {
                    // The token length has not yet been fully received.
                    return CLEAN;
                }
                activeToken = new byte[src.getInt()];
                activeTokenPos = 0;
            }
            
            while (src.hasRemaining()) {
                if (activeTokenPos <activeToken.length) {
                    int toRead = Math.min(activeToken.length-activeTokenPos, src.remaining());
                    src.get(activeToken, activeTokenPos, toRead);
                    activeTokenPos+=toRead;
                }
                if (activeTokenPos<activeToken.length) {
                    return CLEAN;
                }
                if (!context.isEstablished() ) {
                    if (channel.isClientMode()) {
                        kerberosEncoder.writeToken(context.initSecContext(activeToken, 0, activeToken.length));
                    } else {
                        kerberosEncoder.writeToken(context.acceptSecContext(activeToken, 0, activeToken.length));
                    }
                } else {
                    toWrite = context.unwrap(activeToken, 0, activeToken.length, new MessageProp(false));
                    toWritePos = 0;
                }
            }
            return CLEAN;

        } catch (GSSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw ExceptionUtil.rethrow(e);
        } finally {
            compactOrClear(src);
        }
    }
}
