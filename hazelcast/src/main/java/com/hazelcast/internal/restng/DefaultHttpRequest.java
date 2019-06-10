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

package com.hazelcast.internal.restng;

import static com.hazelcast.util.StringUtil.lowerCaseInternal;

import java.util.ArrayList;
import java.util.List;

import com.hazelcast.util.StringUtil;

/**
 *
 */
public class DefaultHttpRequest implements HttpRequest {

    private final String method;
    private final String uri;
    private final String protocol;
    private final List<HttpHeader> headers = new ArrayList<>();
    private volatile byte[] body;
    private volatile AuthenticationStatus authenticationStatus;
    private volatile boolean processingDone;

    public DefaultHttpRequest(String method, String uri, String protocol) {
        this.method = method;
        this.uri = uri;
        this.protocol = protocol;
    }

    public DefaultHttpRequest(String requestLine) throws BadRequestException {
        String[] requestLineParts = requestLine.split("\\s+");
        if (requestLineParts.length != 3) {
            throw new BadRequestException();
        }
        this.method = requestLineParts[0];
        this.uri = requestLineParts[1];
        this.protocol = requestLineParts[2];
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public List<HttpHeader> headers() {
        return headers;
    }

    @Override
    public byte[] body() {
        return body;
    }

    @Override
    public String getHeaderValue(String name) {
        for (HttpHeader header : headers) {
            if (StringUtil.equalsIgnoreCase(header.name(), name)) {
                return header.value();
            }
        }
        return null;
    }

    @Override
    public String[] getHeaderValues(String name) {
        List<String> values = new ArrayList<>();
        for (HttpHeader header : headers) {
            if (StringUtil.equalsIgnoreCase(header.name(), name)) {
                values.add(header.value());
            }
        }
        return values.toArray(new String[values.size()]);
    }

    @Override
    public long contentLength() {
        String headerVal = getHeaderValue(HttpUtils.CONTENT_LENGTH);
        if (headerVal != null) {
            return Long.parseLong(headerVal);
        }
        return 0;
    }

    @Override
    public void body(byte[] body) {
        this.body = body;
    }

    @Override
    public AuthenticationStatus authenticationStatus() {
        return authenticationStatus;
    }

    @Override
    public void authenticationStatus(AuthenticationStatus authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }

    @Override
    public boolean processingDone() {
        return processingDone;
    }

    @Override
    public void flagProcessingDone() {
        processingDone = true;
    }

}
