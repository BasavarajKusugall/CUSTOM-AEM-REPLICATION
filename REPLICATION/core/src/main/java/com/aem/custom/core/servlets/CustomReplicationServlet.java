/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.aem.custom.core.servlets;

import com.day.cq.replication.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import static com.aem.custom.core.servlets.ActiveReplicationAgentTrafficServlet.CONTENT_TYPE_APPLICATION_JSON;


@Component(service=Servlet.class,immediate = true,
           property={
                   Constants.SERVICE_DESCRIPTION + "=Custom Replication Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_POST,
                   "sling.servlet.resourceTypes="+ "custom/replication/service",
                   "sling.servlet.selectors ="+ "replicate",
                   "sling.servlet.extensions=" + "json"
           })
public class CustomReplicationServlet extends SlingAllMethodsServlet {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUid = 1L;

    @Reference
    Replicator replicator;
    @Reference
    AgentManager agentManager;
    protected void doPost(final SlingHttpServletRequest request,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        String pathSelected [] = (String[]) request.getParameterMap().get("pathSelected");
        String agentSelected  =  request.getParameter("agentSelected");
        String agentId  =  request.getParameter("agentId");
        final Map<String, Agent> agents = agentManager.getAgents();
        ResourceResolver resourceResolver = request.getResourceResolver();
        if (StringUtils.isNotEmpty(agentSelected) && StringUtils.isNotEmpty(agentId)  && null != pathSelected && pathSelected.length >0){
            Resource resource = resourceResolver.getResource(agentSelected);
            if (null != resource){
                boolean isValidAgent = validateAgentId(resource.getPath(), agentId);
                replicate(pathSelected, agentId, resourceResolver, isValidAgent);

            }


        }


    }

    private void replicate(String[] pathSelected, String agentId, ResourceResolver resourceResolver, boolean isValidAgent) {
        if (isValidAgent){
            Session session = resourceResolver.adaptTo(Session.class);
            ReplicationOptions opts = new ReplicationOptions();

            AgentIdFilter repAgentFilter = new AgentIdFilter(agentId);
            opts.setFilter(repAgentFilter);
            for (String path: pathSelected){
                try {
                    replicator.replicate(session, ReplicationActionType.ACTIVATE, path, opts);
                } catch (ReplicationException e) {
                    LOGGER.error(e.getMessage(),e);
                }
            }
        }
    }

    private boolean validateAgentId(String path,String agentId){
        return StringUtils.contains(path,agentId);

    }
}
