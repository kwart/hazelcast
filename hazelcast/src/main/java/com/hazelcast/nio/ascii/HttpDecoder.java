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

import static com.hazelcast.internal.networking.HandlerStatus.BLOCKED;
import static com.hazelcast.internal.networking.HandlerStatus.CLEAN;
import static com.hazelcast.nio.IOUtil.compactOrClear;
import static com.hazelcast.util.StringUtil.trim;

import java.nio.ByteBuffer;

import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.InboundHandler;
import com.hazelcast.internal.restng.AuthenticationStatus;
import com.hazelcast.internal.restng.BadRequestException;
import com.hazelcast.internal.restng.DefaultHttpHeader;
import com.hazelcast.internal.restng.DefaultHttpRequest;
import com.hazelcast.internal.restng.HttpUtils;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpProcessor;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.spi.annotation.PrivateApi;
import com.hazelcast.util.StringUtil;

@PrivateApi
public class HttpDecoder extends InboundHandler<ByteBuffer, Void> {

    private static final byte[] EMPTY_BODY = new byte[0];

    private State currentState = State.SKIP_CONTROL_CHARS;
    private volatile StringBuffer lineInProgress = new StringBuffer();
    private volatile String headerName;
    private volatile String headerValue;
    private volatile HttpRequest request;
    private volatile byte[] body;
    private volatile int bodyPos;
    private final HttpProcessor httpProcessor;
    private volatile HttpHandler httpHandler; 

    /**
     * The internal state of {@link HttpObjectDecoder}.
     * <em>Internal use only</em>.
     */
    private enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        AUTHENTICATE,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT,
        READ_CHUNK_SIZE,
        READ_CHUNKED_CONTENT,
        READ_CHUNK_DELIMITER,
        READ_CHUNK_FOOTER,
        PROCESS_REQUEST,
        BAD_MESSAGE,
        UPGRADED
    }
    
    public HttpDecoder(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    @Override
    public void handlerAdded() {
//        initSrcBuffer();
    }

    @Override
    public HandlerStatus onRead() throws Exception {
        src.flip();
        try {
            while (src.hasRemaining() || (request!=null && ! request.processingDone())) {
                switch (currentState) {
                    case SKIP_CONTROL_CHARS: {
                        if (!skipControlCharacters()) {
                            return CLEAN;
                        }
                        currentState = State.READ_INITIAL;
                    }
                    case READ_INITIAL:
                        String line = getNextLine(4096);
                        if (line == null) {
                            return CLEAN;
                        }

                        request = new DefaultHttpRequest(line.toString());
                        currentState = State.READ_HEADER;
                        // fall-through
                    case READ_HEADER:
                        if (!readHeaders()) {
                            return CLEAN;
                        }
                        currentState = State.AUTHENTICATE;
                        httpHandler = httpProcessor.getHandler(request);
                        httpProcessor.startAuthentication(request, channel, httpHandler);

//                        switch (nextState) {
//                        case READ_CHUNK_SIZE:
//                            out.add(message);
//                            return;
//                        default:
//                            /**
//                             * <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">RFC 7230, 3.3.3</a> states that if a
//                             * request does not have either a transfer-encoding or a content-length header then the message body
//                             * length is 0. However for a response the body length is the number of octets received prior to the
//                             * server closing the connection. So we treat this as variable length chunked encoding.
//                             */
//                            long contentLength = contentLength();
//                            if (contentLength == 0 || contentLength == -1 && isDecodingRequest()) {
//                                out.add(message);
//                                out.add(LastHttpContent.EMPTY_LAST_CONTENT);
//                                resetNow();
//                                return;
//                            }
//
//                            assert nextState == State.READ_FIXED_LENGTH_CONTENT ||
//                                    nextState == State.READ_VARIABLE_LENGTH_CONTENT;
//
//                            out.add(message);
//
//                            if (nextState == State.READ_FIXED_LENGTH_CONTENT) {
//                                // chunkSize will be decreased as the READ_FIXED_LENGTH_CONTENT state reads data chunk by chunk.
//                                chunkSize = contentLength;
//                            }
//
//                            // We return here, this forces decode to be called again where we will decode the content
//                            return;
//                        }
//                    case READ_VARIABLE_LENGTH_CONTENT: {
//                        // Keep reading data as a chunk until the end of connection is reached.
//                        int toRead = Math.min(buffer.readableBytes(), maxChunkSize);
//                        if (toRead > 0) {
//                            ByteBuf content = buffer.readRetainedSlice(toRead);
//                            out.add(new DefaultHttpContent(content));
//                        }
//                        return;
//                    }
                        // fall-through
                    case AUTHENTICATE:
                        switch (request.authenticationStatus()) {
                            case IN_PROGRESS:
                                return BLOCKED;
                            case FAILED:
                                break;
                            case PASSED:
                                break;
                            default:
                                throw new BadRequestException();
                        }

                        if (StringUtil.equalsIgnoreCase("chunked", request.getHeaderValue("transfer-encoding"))) {
                            currentState = State.READ_CHUNK_SIZE;
                        } else if (request.contentLength() >= 0) {
                            currentState = State.READ_FIXED_LENGTH_CONTENT;
                        } else {
//                            nextState = State.READ_VARIABLE_LENGTH_CONTENT;
                            throw new BadRequestException();
                        }
                    case READ_FIXED_LENGTH_CONTENT:
                        if (request.authenticationStatus() == AuthenticationStatus.FAILED
                                || httpHandler.getRestEndpointGroup(request) == null) {
                            // skip body - we don't care about invalid requests (404, 401, 30x)
                            long length = request.contentLength();
                            if (length > 8192) {
                                throw new BadRequestException();
                            }
                            int toTransfer = Math.min((int) length - bodyPos, src.remaining());
                            src.position(src.position() + toTransfer);
                            bodyPos += toTransfer;
                            if (bodyPos < length) {
                                return CLEAN;
                            }
                        } else {
                            if (body == null) {
                                long length = request.contentLength();
                                if (length <= 0L) {
                                    body = EMPTY_BODY;
                                } else {
                                    if (length > 8192) {
                                        throw new BadRequestException();
                                    }
                                    body = new byte[(int) length];
                                }
                            }
                            if (body.length > bodyPos) {
                                int toTransfer = Math.min(body.length - bodyPos, src.remaining());
                                src.get(body, bodyPos, toTransfer);
                                bodyPos += toTransfer;
                                if (bodyPos < body.length) {
                                    return CLEAN;
                                }
                            }
                            request.body(body);
                        }
                        httpProcessor.submit(request, channel, httpHandler);
//                        // Check if the buffer is readable first as we use the readable byte count
//                        // to create the HttpChunk. This is needed as otherwise we may end up with
//                        // create a HttpChunk instance that contains an empty buffer and so is
//                        // handled like it is the last HttpChunk.
//                        //
//                        // See https://github.com/netty/netty/issues/433
//                        if (readLimit == 0) {
//                            return CLEAN;
//                        }

//                        if (readLimit > contentLength) {
//                            readLimit = (int) contentLength;
//                        }
//                        ByteBuf content = buffer.readRetainedSlice(toRead);
//                        chunkSize -= toRead;
//
//                        if (chunkSize == 0) {
//                            // Read all content.
//                            out.add(new DefaultLastHttpContent(content, validateHeaders));
//                            resetNow();
//                        } else {
//                            out.add(new DefaultHttpContent(content));
//                        }
                        currentState = State.PROCESS_REQUEST;
                    case PROCESS_REQUEST:
                        if (!request.processingDone()) {
                            return BLOCKED;
                        }
                        resetStatus();
                        break;
                    /**
                     * everything else after this point takes care of reading chunked content. basically, read chunk size,
                     * read chunk, read and ignore the CRLF and repeat until 0
                     */
//                    case READ_CHUNK_SIZE: try {
//                        AppendableCharSequence line = lineParser.parse(buffer);
//                        if (line == null) {
//                            return;
//                        }
//                        int chunkSize = getChunkSize(line.toString());
//                        this.chunkSize = chunkSize;
//                        if (chunkSize == 0) {
//                            currentState = State.READ_CHUNK_FOOTER;
//                            return;
//                        }
//                        currentState = State.READ_CHUNKED_CONTENT;
//                        // fall-through
//                    } catch (Exception e) {
//                        out.add(invalidChunk(buffer, e));
//                        return;
//                    }
//                    case READ_CHUNKED_CONTENT: {
//                        assert chunkSize <= Integer.MAX_VALUE;
//                        int toRead = Math.min((int) chunkSize, maxChunkSize);
//                        toRead = Math.min(toRead, buffer.readableBytes());
//                        if (toRead == 0) {
//                            return;
//                        }
//                        HttpContent chunk = new DefaultHttpContent(buffer.readRetainedSlice(toRead));
//                        chunkSize -= toRead;
//
//                        out.add(chunk);
//
//                        if (chunkSize != 0) {
//                            return;
//                        }
//                        currentState = State.READ_CHUNK_DELIMITER;
//                        // fall-through
//                    }
//                    case READ_CHUNK_DELIMITER: {
//                        final int wIdx = buffer.writerIndex();
//                        int rIdx = buffer.readerIndex();
//                        while (wIdx > rIdx) {
//                            byte next = buffer.getByte(rIdx++);
//                            if (next == HttpConstants.LF) {
//                                currentState = State.READ_CHUNK_SIZE;
//                                break;
//                            }
//                        }
//                        buffer.readerIndex(rIdx);
//                        return;
//                    }
//                    case READ_CHUNK_FOOTER: try {
//                        LastHttpContent trailer = readTrailingHeaders(buffer);
//                        if (trailer == null) {
//                            return;
//                        }
//                        out.add(trailer);
//                        resetNow();
//                        return;
//                    } catch (Exception e) {
//                        out.add(invalidChunk(buffer, e));
//                        return;
//                    }
//                    case BAD_MESSAGE: {
//                        // Keep discarding until disconnection.
//                        buffer.skipBytes(buffer.readableBytes());
//                        break;
//                    }
//                    case UPGRADED: {
//                        int readableBytes = buffer.readableBytes();
//                        if (readableBytes > 0) {
//                            // Keep on consuming as otherwise we may trigger an DecoderException,
//                            // other handler will replace this codec with the upgraded protocol codec to
//                            // take the traffic over at some point then.
//                            // See https://github.com/netty/netty/issues/2173
//                            out.add(buffer.readBytes(readableBytes));
//                        }
//                        break;
//                    }
                    }

            }

            return CLEAN;
        } finally {
            compactOrClear(src);
        }
    }

    private void resetStatus() {
        currentState = State.SKIP_CONTROL_CHARS;
        lineInProgress.delete(0, lineInProgress.length());
        headerName = null;
        headerValue = null;
        request = null;
        body = null;
        bodyPos = 0;
        httpHandler = null; 
    }

    private boolean readHeaders() throws BadRequestException {
        String line = getNextLine(4096);
        if (line == null) {
            return false;
        }
        if (line.length() > 0) {
            do {
                char firstChar = line.charAt(0);
                if (headerName != null && (firstChar == ' ' || firstChar == '\t')) {
                    headerValue = headerValue + ' ' + trim(line);
                    if (headerValue.length() > 4096) {
                        throw new BadRequestException();
                    }
                } else {
                    if (headerName != null) {
                        if (request.headers().size()>20) {
                            throw new BadRequestException();
                        }
                        request.headers().add(new DefaultHttpHeader(headerName, headerValue));
                    }
                    int colonPos = line.indexOf(':');
                    if (colonPos<=0) {
                        throw new BadRequestException();
                    }
                    headerName = trim(line.substring(0, colonPos));
                    headerValue = trim(line.substring(colonPos+1));
                }

                line = getNextLine(4096);
                if (line == null) {
                    return false;
                }
            } while (line.length() > 0);
        }
        // Add the last header.
        if (headerName != null) {
            if (request.headers().size()>20) {
                throw new BadRequestException();
            }
            request.headers().add(new DefaultHttpHeader(headerName, headerValue));
        }
        // reset name and value fields
        headerName = null;
        headerValue = null;

        return true;
    }

    private boolean skipControlCharacters() {
        while (src.hasRemaining()) {
            int c = (src.get() & 0xFF);
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                src.position(src.position() - 1);
                return true;
            }
        }
        return false;
    }

    private String getNextLine(int maxLength) throws BadRequestException {
        while (src.hasRemaining()) {
            char c = (char) (src.get() & 0xFF);
            if (c == HttpUtils.CR) {
                continue;
            }
            if (c == HttpUtils.LF) {
                String result = lineInProgress.toString();
                lineInProgress.delete(0, lineInProgress.length());
                return result;
            }
            if (lineInProgress.length() >= maxLength) {
                throw new BadRequestException();
            }
            lineInProgress.append(c);
        }
        return null;
    }

}
