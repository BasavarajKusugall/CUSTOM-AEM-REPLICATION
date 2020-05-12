package com.aem.custom.core.servlets;


import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;

@Component(service = ParticipantStepChooser.class,immediate = true,property = {
        Constants.SERVICE_DESCRIPTION+"=An example implementation of a dynamic participant chooser."
})

public class InitiatorParticipantChooser implements ParticipantStepChooser {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public String getParticipant(WorkItem arg0, WorkflowSession arg1,
                                 MetaDataMap arg2) throws WorkflowException {

        String initiator = arg0.getWorkflow().getInitiator();
        logger.info("Assigning Dynamic Participant Step work item to {}",initiator);

        return initiator;
    }
}