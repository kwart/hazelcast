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

import static com.hazelcast.internal.networking.HandlerStatus.CLEAN;
import static com.hazelcast.internal.networking.HandlerStatus.DIRTY;
import static com.hazelcast.nio.IOUtil.compactOrClear;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.MessageProp;

import com.hazelcast.internal.networking.ChannelInboundHandler;
import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.util.ExceptionUtil;

/**
 * A {@link ChannelInboundHandler} that reads the protocol bytes {@link com.hazelcast.nio.Protocols} and based on the protocol
 * it creates the appropriate handlers.
 *
 * The ProtocolDecoder doesn't forward to the dst; it replaces itself once the protocol bytes are known. So that is why the Void
 * type for dst.
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
        this.context = context;
    }

    @Override
    public void handlerAdded() {
        initSrcBuffer(2048);
    }

    @Override
    public HandlerStatus onRead() {
        src.flip();

        try {
            System.out.println(Thread.currentThread().getName() + " onRead 1");
            if (dst != null && dst.hasRemaining() && toWrite != null) {
                System.out.println("decoder send data");
                int min = Math.min(dst.remaining(), toWrite.length - toWritePos);
                dst.put(toWrite, toWritePos, min);
                toWritePos += min;
                if (toWritePos >= toWrite.length) {
                    toWritePos = 0;
                    toWrite = null;
                } else {
                    return DIRTY;
                }
            }
            System.out.println(Thread.currentThread().getName() + " onRead 2");
            if (activeToken == null) {
                if (src.remaining() < 4) {
                    // The token length has not yet been fully received.
                    return CLEAN;
                }
                activeToken = new byte[src.getInt()];
                activeTokenPos = 0;
            }
            System.out.println(Thread.currentThread().getName() + " onRead 3");

            if (activeTokenPos < activeToken.length) {
                int toRead = Math.min(activeToken.length - activeTokenPos, src.remaining());
                src.get(activeToken, activeTokenPos, toRead);
                activeTokenPos += toRead;
            }
            if (activeTokenPos < activeToken.length) {
                return CLEAN;
            }
            System.out.println(Thread.currentThread().getName() + " Readen token " + Arrays.toString(activeToken));

            if (!context.isEstablished()) {
                if (channel.isClientMode()) {
                    System.out.println("init sec context ");
                    byte[] initSecContext = context.initSecContext(activeToken, 0, activeToken.length);
                    activeToken = null;
                    activeTokenPos = 0;
                    if (initSecContext != null) {
                        kerberosEncoder.writeToken(initSecContext);
                    } else {
                        channel.outboundPipeline().wakeup();
                        System.out.println("init sec context was null");
                        return CLEAN;
                    }
                } else {
                    System.out.println("accept sec context");
                    byte[] acceptSecContext = context.acceptSecContext(activeToken, 0, activeToken.length);
                    activeToken = null;
                    activeTokenPos = 0;
                    if (acceptSecContext != null) {
                        kerberosEncoder.writeToken(acceptSecContext);
                    } else {
                        channel.outboundPipeline().wakeup();
                        System.out.println("accept sec context was null");
                        return CLEAN;
                    }
                }
            } else {
                System.out.println("unwrap");
                toWrite = context.unwrap(activeToken, 0, activeToken.length, new MessageProp(false));
                toWritePos = 0;
                activeToken = null;
                activeTokenPos = 0;
                return DIRTY;
            }
            activeToken = null;
            activeTokenPos = 0;

            if (src.hasRemaining()) {
                return DIRTY;
            }
            return CLEAN;

        } catch (GSSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw ExceptionUtil.rethrow(e);
        } finally {
            compactOrClear(src);
            System.out.println("onRead finally");
        }
    }
}
