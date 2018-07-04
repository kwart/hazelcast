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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.MessageProp;

import com.hazelcast.internal.networking.ChannelOutboundHandler;
import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.util.ExceptionUtil;

/**
 * The ProtocolEncoder is responsible for writing the protocol and once the protocol
 * has been written, the ProtocolEncoder is replaced by the appropriate handler.
 *
 * The ProtocolEncoder and the 'client' side of a member connection, will always
 * write the cluster protocol immediately. The ProtocolEncoder on the 'server' side
 * of the connection will wait till it has received the protocol and then will only
 * send the protocol if the client side was a member.
 */
public class KerberosEncoder extends ChannelOutboundHandler<ByteBuffer, ByteBuffer> {

    private final Queue<byte[]> tokenQueue;
    private final GSSContext context;
    private byte[] activeToken;
    private int activeTokenPos;

    public KerberosEncoder(GSSContext context) {
        this.context= context;
        this.tokenQueue = new ConcurrentLinkedQueue<byte[]>();
    }
    
    @Override
    public void handlerAdded() {
        initDstBuffer(2048);

        if (channel.isClientMode() && !context.isEstablished()) {
            try {
                tokenQueue.offer(context.initSecContext(new byte[0], 0, 0));
            } catch (GSSException e) {
                ExceptionUtil.rethrow(e);
            }
        }
    }

    void writeToken(byte[] token) {
        tokenQueue.offer(token);
        System.out.println("Token to write: " + token.length);
        channel.outboundPipeline().wakeup();
    }

    @Override
    public HandlerStatus onWrite() {
        compactOrClear(dst);

        try {
            if (context.isEstablished() && src.remaining() > 0) {
                byte[] token = new byte[src.remaining()];
                src.get(token);
                tokenQueue.offer(context.wrap(token, 0, token.length, new MessageProp(false)));
            }
        if (activeToken!=null && dst.hasRemaining()) {
            int toWrite = Math.min(dst.remaining(), activeToken.length-activeTokenPos);
            dst.put(activeToken, activeTokenPos, toWrite);
            activeTokenPos += toWrite;
            if (activeTokenPos>=activeToken.length) {
                activeToken = null;
                activeTokenPos=0;
            } else {
                return DIRTY;
            }
        }
        while (dst.remaining()>4 && !tokenQueue.isEmpty()) {
            activeToken = tokenQueue.poll();
            if (activeToken!=null) {
              dst.putInt(activeToken.length);
              activeTokenPos = 0;
            }
            if (activeToken!=null && dst.hasRemaining()) {
                int toWrite = Math.min(dst.remaining(), activeToken.length-activeTokenPos);
                dst.put(activeToken, activeTokenPos, toWrite);
                activeTokenPos += toWrite;
                if (activeTokenPos>=activeToken.length) {
                    activeToken = null;
                    activeTokenPos=0;
                }
            }
        }

        if (!dst.hasRemaining() || (activeToken==null && dst.remaining()<4 && !tokenQueue.isEmpty())) {
            return DIRTY;
        }
        
//        if (!context.isEstablished()) {
//            // wait until Decoder provides more messages
//            return CLEAN;
//        } 

            return CLEAN;
        } catch (GSSException e) {
            e.printStackTrace();
            throw ExceptionUtil.rethrow(e);
        } finally {
            dst.flip();
        }
    }

}
