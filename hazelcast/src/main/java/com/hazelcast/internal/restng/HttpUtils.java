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

import static com.hazelcast.util.StringUtil.bytesToString;
import static com.hazelcast.util.StringUtil.stringToBytes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.hazelcast.nio.Protocols;

public final class HttpUtils {

    private static final Set<String> HTTP_PREFIXES = Arrays.stream(HttpMethod.values())
            .map(m -> m.name().substring(0, Protocols.PROTOCOL_LENGTH))
            .collect(Collectors.toSet());

    public static final char CR = '\r';
    public static final char LF = '\n';
    public static final String CR_LF = "\r\n";
    
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String AUTHORIZATION = "Authorization";
    
    public static final String CONTENT_TYPE_PLAIN_TEXT = "text/plain";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    public static final String URI_BASE_MANCENTER="/hazelcast/rest/mancenter";
    public static final String URI_BASE_CLUSTER_MANAGEMENT = "/hazelcast/rest/management/cluster";
    
    public static final byte[] EMPTY = new byte[0];

    public static String trimQueryParams(String uri) {
        if (uri == null) {
            return null;
        }
        int pos = uri.indexOf('?');
        return pos > -1 ? uri.substring(0, pos - 1) : uri;
    }

    /**
     * Returns an {@link HttpMethod} instance or {@code null} if the given request doesn't use one of big 7 methods.
     *
     * @param req
     * @return
     */
    public static HttpMethod getHttpMethod(HttpRequest req) {
        if (req == null || req.method()==null) {
            return null;
        }
        String method = req.method();
        for (HttpMethod m: HttpMethod.values()) {
            if (method.equals(m.name())) {
                return m;
            }
        }
        return null;
    }

    public static HttpResponse createOkTextResponse(HttpRequest req, String body) {
        DefaultHttpResponse response = new DefaultHttpResponse(req, WellKnownHttpStatus.OK_200,
                body != null ? stringToBytes(body) : null);
        response.headers().add(new DefaultHttpHeader(CONTENT_TYPE, CONTENT_TYPE_PLAIN_TEXT));
        return response;
    }

    public static HttpResponse createServerErrorTextResponse(HttpRequest req, String body) {
        DefaultHttpResponse response = new DefaultHttpResponse(req, WellKnownHttpStatus.INTERNAL_SERVER_ERROR_500,
                body != null ? stringToBytes(body) : null);
        response.headers().add(new DefaultHttpHeader(CONTENT_TYPE, CONTENT_TYPE_PLAIN_TEXT));
        return response;
    }

    public static HttpResponse createBadRequestTextResponse(HttpRequest req, String body) {
        DefaultHttpResponse response = new DefaultHttpResponse(req, WellKnownHttpStatus.BAD_REQUEST_400,
                body != null ? stringToBytes(body) : null);
        response.headers().add(new DefaultHttpHeader(CONTENT_TYPE, CONTENT_TYPE_PLAIN_TEXT));
        return response;
    }

    public static String bodyAsString(HttpRequest req) {
        byte[] body = req.body();
        return body == null ? null : bytesToString(body);
    }
    /**
     * Returns an {@link HttpProtocol} instance or {@code null} if the given request doesn't use one of HTTP/1.0 or HTTP/1.1
     * protocols.
     *
     * @param req
     * @return
     */
    public static HttpProtocol getHttpProtocol(HttpRequest req) {
        if (req == null || req.protocol()==null) {
            return null;
        }
        String protocol = req.method();
        for (HttpProtocol p: HttpProtocol.values()) {
            if (protocol.equals(p.protocol())) {
                return p;
            }
        }
        return null;
    }

    public static boolean startsAsHttpMethod(String protocol) {
        return HTTP_PREFIXES.contains(protocol);
    }

    public static enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, TRACE, OPTIONS;
    }

    public static enum HttpProtocol {
        HTTP_1_0("HTTP/1.0"),
        HTTP_1_1("HTTP/1.1");

        private final String protocol;

        private HttpProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String protocol() {
            return protocol;
        }
    }

    public static enum WellKnownHttpStatus implements HttpStatus {
        // Succesfull 2xx 
        OK_200(200, "OK"),
        CREATED_201(201, "Created"),
        ACCEPTED_202(202, "Accepted"),
        NO_CONTENT_204(204, "No Content", false),

        // Redirection 3xx
        MOVED_PERMANENTLY_301(301, "Moved Permanently"),
        FOUND_302(301, "Found"),

        // Client Error 4xx
        BAD_REQUEST_400(400, "Bad Request"),
        UNAUTHORIZED_401(401, "Unauthorized"),
        PAYMENT_REQUIRED_402(402, "Payment Required"),
        FORBIDDEN_403(403, "Unauthorized", true),
        NOT_FOUND_404(404, "Not Found", true),
        METHOD_NOT_ALLOWED_405(405, "Method Not Allowed"),
        REQUEST_TIMEOUT_408(408, "Request Timeout"),
        GONE_410(410, "Gone"),
        REQUEST_ENTITY_TOO_LARGE_413(413, "Request Entity Too Large"),
        REQUEST_URI_TOO_LONG_414(414, "Request-URI Too Long"),
        UNSUPPORTED_MEDIA_TYPE_415(415, "Unsupported Media Type"),

        // Server Error 5xx
        INTERNAL_SERVER_ERROR_500(500, "Internal Server Error", true),
        NOT_IMPLEMENTED_501(501, "Not Implemented", true),
        SERVICE_UNAVAILABLE_503(503, "Service Unavailable", true),
        HTTP_VERSION_NOT_SUPPORTED_505(505, "HTTP Version Not Supported");

        private final int code;
        private final String description;
        private final boolean includeBody;

        private WellKnownHttpStatus(int code, String description, boolean includeBody) {
            this.code = code;
            this.description = description;
            this.includeBody = includeBody;
        }
        private WellKnownHttpStatus(int code, String description) {
            this(code, description, false);
        }

        public String toString() {
            return code + " " + description;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public boolean includeBody() {
            return includeBody;
        }
    }

    private HttpUtils() {
    }
}
