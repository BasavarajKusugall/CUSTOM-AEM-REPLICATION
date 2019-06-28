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

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationQueue;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Component(service=Servlet.class,
           property={
                   Constants.SERVICE_DESCRIPTION + "=Replication Agent Listing Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.resourceTypes="+ "custom/replication/status",
                   "sling.servlet.selectors ="+ "active",
                   "sling.servlet.extensions=" + "json"
           })
public class ActiveReplicationAgentTrafficServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUid = 1L;
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";


    @Reference
    AgentManager agentManager;
    @Override
    protected void doGet(final SlingHttpServletRequest request,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        String agentId = request.getParameter("agentId");
        String responseMsg = StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(agentId)){
            Map<String, Agent> agentsMap = agentManager.getAgents();
            if (!agentsMap.isEmpty()){
                Agent agent = agentsMap.get(agentId);
                if (null != agent){
                    List<ReplicationQueue.Entry> entryList = agent.getQueue().entries();
                    if (!entryList.isEmpty()){
                        int queue = entryList.size();
                        responseMsg = "Queue is blocked "+queue+" - pending";
                    }else {
                        responseMsg = "Queue is idle";
                    }
                }else{
                    responseMsg = "Not found replication agentId("+agentId+")";
                }

            }else {
                responseMsg = "No agents found !";
            }
        }
        resp.getWriter().println(responseMsg);

    }
}
