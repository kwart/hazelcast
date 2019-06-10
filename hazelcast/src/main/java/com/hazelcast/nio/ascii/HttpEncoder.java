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

import static com.hazelcast.internal.networking.HandlerStatus.CLEAN;
import static com.hazelcast.internal.networking.HandlerStatus.DIRTY;
import static com.hazelcast.nio.IOUtil.compactOrClear;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.OutboundHandler;
import com.hazelcast.internal.restng.ResponseOutboundFrame;

/**
 * A {@link OutboundHandler} for HTTP REST calls. It writes HttpResponse to the ByteBuffer.
 */
public class HttpEncoder extends OutboundHandler<Supplier<ResponseOutboundFrame>, ByteBuffer> {

    private ResponseOutboundFrame response;

    @Override
    public void handlerAdded() {
        initDstBuffer();
    }

    @Override
    public HandlerStatus onWrite() {
        compactOrClear(dst);
        try {
            for (; ; ) {
                if (response == null) {
                    response = src.get();

                    if (response == null) {
                        // everything is processed, so we are done
                        return CLEAN;
                    }
                }

                if (response.writeTo(dst)) {
                    response.request().flagProcessingDone();
                    channel.inboundPipeline().wakeup();
                    // message got written, lets see if another message can be written
                    response = null;
                } else {
                    // the message didn't get written completely, so we are done.
                    return DIRTY;
                }
            }
        } finally {
            dst.flip();
        }
    }
}
