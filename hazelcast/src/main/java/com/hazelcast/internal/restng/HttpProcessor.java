package com.hazelcast.internal.restng;

import static com.hazelcast.internal.restng.AuthenticationStatus.FAILED;
import static com.hazelcast.internal.restng.AuthenticationStatus.IN_PROGRESS;
import static com.hazelcast.internal.restng.AuthenticationStatus.PASSED;
import static com.hazelcast.util.ExceptionUtil.rethrow;

import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.hazelcast.config.AdvancedNetworkConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.config.RestServerEndpointConfig;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.cluster.impl.ConfigMismatchException;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus;
import com.hazelcast.internal.restng.handlers.HttpHandler404;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.Connection;
import com.hazelcast.security.Credentials;
import com.hazelcast.security.SecurityContext;
import com.hazelcast.spi.TaskScheduler;

public class HttpProcessor {

    private static final ILogger LOGGER = Logger.getLogger(HttpProcessor.class.getName());

    private static final HttpHandler HANDLER_404 = new HttpHandler404();
    private final Map<String, HttpHandler> uriToHandlers;
    private final Map<String, HttpHandler> uriPrefixToHandlers;

    private TaskScheduler taskScheduler;
    private SecurityContext securityContext;

    private HttpProcessor(Node node) {
        this.securityContext = node.securityContext;
        this.taskScheduler = node.nodeEngine.getExecutionService().getGlobalTaskScheduler();
        this.uriToHandlers = new ConcurrentHashMap<>();
        this.uriPrefixToHandlers = new ConcurrentHashMap<>();
        LOGGER.fine("Registering HttpHandlerFactory instances");
        registerHttpHandlers(node);
    }

    private void registerHttpHandlers(Node node) {
        ServiceLoader<HttpHandlerFactory> serviceLoader = ServiceLoader.load(HttpHandlerFactory.class);
        for (HttpHandlerFactory factory : serviceLoader) {
            boolean uriIsPrefix = factory.uriIsPrefix();
            String uri = cleanUpUri(factory.uri(), uriIsPrefix);
            if (LOGGER.isFineEnabled()) {
                LOGGER.fine("Registering factory for " + (uriIsPrefix ? "URI: " : "URI prefix: ") + uri);
            }
            if (uriIsPrefix) {
                if (isUriOverlap(uri, uriPrefixToHandlers.keySet())) {
                    throw new ConfigMismatchException(
                            "HttpHandlerFactory " + factory.getClass().getName() + " has an URI overlap with another factory");
                }
                uriPrefixToHandlers.put(uri, factory.create(node));
            } else {
                if (uriToHandlers.get(uri) != null) {
                    throw new ConfigMismatchException(
                            "HttpHandlerFactory " + factory.getClass().getName() + " has an URI overlap with another factory");
                }
                uriToHandlers.put(uri, factory.create(node));
            }
        }
    }

    public static HttpProcessor create(Node node) {
        if (!isRestEnabled(node.getConfig())) {
            return null;
        }
        return new HttpProcessor(node);
    }

    private static boolean isUriOverlap(String uri, Set<String> registeredUris) {
        for (String registeredUri : registeredUris) {
            if (uri.startsWith(registeredUri) || registeredUri.startsWith(uri)) {
                return true;
            }
        }
        return false;
    }

    public HttpHandler getHandler(HttpRequest request) {
        HttpHandler handler = uriToHandlers.get(cleanUpUri(request.uri(), false));
        if (handler != null) {
            return handler;
        }
        for (Map.Entry<String, HttpHandler> e : uriPrefixToHandlers.entrySet()) {
            if (request.uri().startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return HANDLER_404;
    }

    public void startAuthentication(HttpRequest request, Channel channel, HttpHandler httpHandler) {
        RestEndpointGroup group = httpHandler.getRestEndpointGroup(request);
        if (securityContext == null || group == null) {
            request.authenticationStatus(PASSED);
            return ;
        }
        Credentials credentials = null;
        for (HttpAuthenticationMechanism mechanism : Mechanisms.REGISTRY) {
            try {
                credentials = mechanism.getCredentials(request);
            } catch (Exception e) {
                LOGGER.fine("Credentials retrival failed", e);
            }
            if (credentials != null) {
                break;
            }
        }
        if (credentials==null) {
            request.authenticationStatus(FAILED);
            return ;
        }
        request.authenticationStatus(IN_PROGRESS);
        Connection connection = (Connection) channel.attributeMap().get(Connection.class);
        try {
            LoginContext lc = group.isManagementAction() ? securityContext.createMemberLoginContext(credentials, connection)
                    : securityContext.createClientLoginContext(credentials, connection);
            taskScheduler.execute(new AuthenticationTask(request, lc, channel));
        } catch (LoginException e) {
            LOGGER.severe("Unable to create login context", e);
            throw rethrow(e);
        }
    }

    public void submit(HttpRequest request, Channel channel, HttpHandler httpHandler) {
        if (request.authenticationStatus()==AuthenticationStatus.PASSED) {
            taskScheduler.execute(new RequestHandlingTask(request, channel, httpHandler));
        } else {
            DefaultHttpResponse response = new DefaultHttpResponse(request, WellKnownHttpStatus.UNAUTHORIZED_401);
            for (HttpAuthenticationMechanism mechanism : Mechanisms.REGISTRY) {
                try {
                    String realmName = httpHandler.getRestEndpointGroup(request).isManagementAction() 
                            ? "Hazelcast Management"
                            : "Hazelcast Data";
                    mechanism.addChallenge(response, realmName);
                } catch (Exception e) {
                    LOGGER.fine("Adding an authentication challenge to the HTTP response failed", e);
                }
            }
            channel.write(new ResponseOutboundFrame(response));
        }
    }

    private static String cleanUpUri(String uri, boolean withTrailingSlash) {
        int pos = uri.indexOf('?');
        int uriLength = uri.length();
        if (pos < 0) {
            pos = uriLength;
        }
        while (pos > 0 && uri.charAt(pos - 1) == '/') {
            pos--;
        }
        if (pos < uriLength) {
            uri = uri.substring(0, pos);
        }
        if (withTrailingSlash) {
            uri = uri + "/";
        }
        return pos < uriLength ? uri.substring(0, pos) : uri;
    }

    private static boolean isRestEnabled(Config config) {
        AdvancedNetworkConfig advConfig = config.getAdvancedNetworkConfig();
        if (advConfig != null && advConfig.isEnabled()) {
            RestServerEndpointConfig restEndpointConfig = advConfig.getRestEndpointConfig();
            return restEndpointConfig != null && restEndpointConfig.isEnabledAndNotEmpty();
        } else {
            RestApiConfig restApiConfig = config.getNetworkConfig().getRestApiConfig();
            return restApiConfig != null && restApiConfig.isEnabledAndNotEmpty();
        }
    }

    static class RequestHandlingTask implements Runnable {
        private final HttpRequest request;
        private final Channel channel;
        private final HttpHandler httpHandler;

        public RequestHandlingTask(HttpRequest request, Channel channel, HttpHandler httpHandler) {
            this.request = request;
            this.channel = channel;
            this.httpHandler = httpHandler;
        }

        @Override
        public void run() {
            HttpResponse response;
            try {
                response = httpHandler.handle(request);
            } catch (Exception e) {
                LOGGER.warning("HTTP request handling failed", e);
                response = new DefaultHttpResponse(request, WellKnownHttpStatus.INTERNAL_SERVER_ERROR_500);
            }
            channel.write(new ResponseOutboundFrame(response));
        }
    }

    static class AuthenticationTask implements Runnable {
        private final HttpRequest request;
        private final LoginContext lc;
        private final Channel channel;

        public AuthenticationTask(HttpRequest request, LoginContext lc, Channel channel) {
            this.request = request;
            this.lc = lc;
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                lc.login();
                request.authenticationStatus(PASSED);
            } catch (Exception e) {
                LOGGER.fine("Authentication failed", e);
                request.authenticationStatus(FAILED);
            } finally {
                channel.inboundPipeline().wakeup();
            }
        }
    }

    private static class Mechanisms {
        static final Set<HttpAuthenticationMechanism> REGISTRY = Collections.newSetFromMap(new ConcurrentHashMap<>());
        static {
            ServiceLoader<HttpAuthenticationMechanism> serviceLoader = ServiceLoader.load(HttpAuthenticationMechanism.class);
            for (HttpAuthenticationMechanism mechanism : serviceLoader) {
                if (LOGGER.isFineEnabled()) {
                    LOGGER.fine("Registering AuthenticationMechanism : " + mechanism.name());
                }
                REGISTRY.add(mechanism);
            }
        }
    }
}
