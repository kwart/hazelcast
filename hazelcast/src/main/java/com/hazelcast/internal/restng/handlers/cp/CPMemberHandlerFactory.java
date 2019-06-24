package com.hazelcast.internal.restng.handlers.cp;

import static com.hazelcast.config.RestEndpointGroup.CLUSTER_WRITE;
import static com.hazelcast.internal.restng.HttpUtils.createBadRequestTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.createOkTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.createServerErrorTextResponse;
import static com.hazelcast.internal.restng.HttpUtils.trimQueryParams;
import static com.hazelcast.internal.restng.HttpUtils.WellKnownHttpStatus.METHOD_NOT_ALLOWED_405;

import org.kohsuke.MetaInfServices;

import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.restng.AbstractHttpHandler;
import com.hazelcast.internal.restng.DefaultHttpResponse;
import com.hazelcast.internal.restng.HttpHandler;
import com.hazelcast.internal.restng.HttpHandlerFactory;
import com.hazelcast.internal.restng.HttpRequest;
import com.hazelcast.internal.restng.HttpResponse;
import com.hazelcast.internal.restng.HttpUtils;

@MetaInfServices
public class CPMemberHandlerFactory implements HttpHandlerFactory {

    @Override
    public String uri() {
        return HttpUtils.URI_BASE_CP_SUBSYSTEM + "/members";
    }

    @Override
    public HttpHandler create(Node node) {
        return new Handler(node, uri());
    }

    @Override
    public boolean uriIsPrefix() {
        return true;
    }

    private static class Handler extends AbstractHttpHandler {

        public Handler(Node node, String factoryUri) {
            super(node, factoryUri);
        }

        @Override
        public RestEndpointGroup getRestEndpointGroup(HttpRequest req) {
            return CLUSTER_WRITE;
        }

        @Override
        protected HttpResponse doGet(HttpRequest req) {
            return new DefaultHttpResponse(req, METHOD_NOT_ALLOWED_405);
        }

        @Override
        protected HttpResponse doPost(HttpRequest req) {
            CPSubsystem cpSubsystem = node.getNodeEngine().getHazelcastInstance().getCPSubsystem();
            String actionStr = trimQueryParams(trimFactoryUri(req));
            try {
                if (actionStr.startsWith("remove/")) {
                    String uuid = actionStr.substring("remove/".length());
                    cpSubsystem.getCPSubsystemManagementService().removeCPMember(uuid).get();
                } else if ("promote".equals(actionStr)) {
                    if (cpSubsystem.getLocalCPMember() == null) {
                        cpSubsystem.getCPSubsystemManagementService().promoteToCPMember().get();
                    }
                } else {
                    return createBadRequestTextResponse(req,
                            "Unexpected action in uri. One of 'promote'/'remove' is expected. Provided: " + actionStr);
                }
                return createOkTextResponse(req, null);
            } catch (Exception e) {
                logger.warning("Problem during processing CP-subsystem management action", e);
                return createServerErrorTextResponse(req, "Problem occured during CP-subsystem management action.");
            }
        }
    }
}
