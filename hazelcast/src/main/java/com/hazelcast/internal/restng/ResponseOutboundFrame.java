package com.hazelcast.internal.restng;

import static com.hazelcast.internal.restng.HttpUtils.CONTENT_LENGTH;
import static com.hazelcast.internal.restng.HttpUtils.CR_LF;
import static com.hazelcast.internal.restng.HttpUtils.EMPTY;
import static com.hazelcast.util.ExceptionUtil.rethrow;
import static com.hazelcast.util.StringUtil.stringToBytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.hazelcast.internal.networking.OutboundFrame;

public class ResponseOutboundFrame implements OutboundFrame {

    private int pos = 0;
    private final byte[] message;
    private final HttpRequest request;

    public ResponseOutboundFrame(HttpResponse response) {
        this.request = response.request();
        HttpStatus status = response.status();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.write(stringToBytes(response.request().protocol() + " " + status.code() + " " + status.description() + CR_LF));
            for (HttpHeader header : response.headers()) {
                if (!CONTENT_LENGTH.equals(header.name())) {
                    baos.write(stringToBytes(header.name() + ":" + header.value() + CR_LF));
                }
            }
            byte[] body = response.body();
            if (body == null) {
                body = EMPTY;
            }
            baos.write(stringToBytes(CONTENT_LENGTH + ":" + body.length + CR_LF + CR_LF));
            baos.write(body);
            message = baos.toByteArray();
        } catch (IOException e) {
            throw rethrow(e);
        }
    }

    @Override
    public boolean isUrgent() {
        return false;
    }

    @Override
    public int getFrameLength() {
        return message.length;
    }

    public HttpRequest request() {
        return request;
    }

    public boolean writeTo(ByteBuffer dst) {
        int toWrite = Math.min(dst.remaining(), message.length - pos);
        dst.put(message, pos, toWrite);
        pos += toWrite;
        return pos >= message.length;
    }

}
