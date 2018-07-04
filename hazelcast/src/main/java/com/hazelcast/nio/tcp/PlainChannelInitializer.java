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

import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.networking.ChannelConfig;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.nio.IOService;
import com.hazelcast.spi.properties.HazelcastProperties;
import com.hazelcast.util.ExceptionUtil;

import static com.hazelcast.internal.networking.ChannelOption.DIRECT_BUF;
import static com.hazelcast.internal.networking.ChannelOption.SO_KEEPALIVE;
import static com.hazelcast.internal.networking.ChannelOption.SO_LINGER;
import static com.hazelcast.internal.networking.ChannelOption.SO_RCVBUF;
import static com.hazelcast.internal.networking.ChannelOption.SO_SNDBUF;
import static com.hazelcast.internal.networking.ChannelOption.TCP_NODELAY;
import static com.hazelcast.nio.IOService.KILO_BYTE;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_BUFFER_DIRECT;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_KEEP_ALIVE;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_LINGER_SECONDS;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_NO_DELAY;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_RECEIVE_BUFFER_SIZE;
import static com.hazelcast.spi.properties.GroupProperty.SOCKET_SEND_BUFFER_SIZE;

import java.security.AccessController;

import javax.security.auth.Subject;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;

/**
 * A {@link ChannelInitializer} that runs on a member and used for unencrypted
 * channels. It will deal with the exchange of protocols and based on that it
 * will set up the appropriate handlers in the pipeline.
 */
public class PlainChannelInitializer implements ChannelInitializer {

    private final static Oid KRB5_OID;
    static {
        try {
            KRB5_OID = new Oid("1.2.840.113554.1.2.2");
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }

    private final IOService ioService;
    private final HazelcastProperties props;

    public PlainChannelInitializer(IOService ioService) {
        this.props = ioService.properties();
        this.ioService = ioService;
    }

    @Override
    public void initChannel(Channel channel) {
        ChannelConfig config = channel.config();
        config.setOption(DIRECT_BUF, props.getBoolean(SOCKET_BUFFER_DIRECT))
                .setOption(TCP_NODELAY, props.getBoolean(SOCKET_NO_DELAY))
                .setOption(SO_KEEPALIVE, props.getBoolean(SOCKET_KEEP_ALIVE))
                .setOption(SO_SNDBUF, props.getInteger(SOCKET_SEND_BUFFER_SIZE) * KILO_BYTE)
                .setOption(SO_RCVBUF, props.getInteger(SOCKET_RECEIVE_BUFFER_SIZE) * KILO_BYTE)
                .setOption(SO_RCVBUF, props.getInteger(SOCKET_RECEIVE_BUFFER_SIZE) * KILO_BYTE)
                .setOption(SO_LINGER, props.getSeconds(SOCKET_LINGER_SECONDS));

        ProtocolEncoder encoder = new ProtocolEncoder(ioService);
        ProtocolDecoder decoder = new ProtocolDecoder(ioService, encoder);
        
        GSSManager manager = GSSManager.getInstance();
        GSSContext gssContext=null;
        
//        Subject activeSubject = Subject.getSubject(AccessController.getContext());
        try {
        if (channel.isClientMode()) {
            
            gssContext = manager.createContext(manager.createName("server1/hzc@JBOSS.ORG", null), KRB5_OID, null, GSSContext.DEFAULT_LIFETIME);

            //            gssContext.requestCredDeleg(true);
//            gssContext.requestMutualAuth(true);
//            gssContext.requestConf(true);
//            gssContext.requestInteg(true);
        } else {
            gssContext = manager.createContext((GSSCredential) null);
        }
        } catch (GSSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ExceptionUtil.rethrow(e);
        }
        KerberosEncoder krbEncoder=new KerberosEncoder(gssContext);
        KerberosDecoder krbDecoder=new KerberosDecoder(gssContext,krbEncoder);
        
        TcpIpConnection connection = (TcpIpConnection) channel.attributeMap().get(TcpIpConnection.class);
        channel.outboundPipeline().addLast(ioService.createMemberOutboundHandlers(connection));
        
        channel.outboundPipeline().addLast(krbEncoder);
        channel.outboundPipeline().addLast(encoder);
        
        channel.inboundPipeline().addLast(decoder);
        channel.inboundPipeline().addLast(krbDecoder);
        
//        channel.inboundPipeline().addLast(ioService.createMemberInboundHandlers(connection));
    }
}
