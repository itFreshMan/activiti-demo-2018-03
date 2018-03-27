package com.tydic.activiti.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.TransitionImpl;

public class FlowTakeListener implements ExecutionListener{

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		
		String processKey = execution.getProcessDefinitionId().split(":")[0];
		String activityId = execution.getCurrentActivityId();
		String activitiName = execution.getCurrentActivityName();
		String procInstId = execution.getProcessInstanceId();
		String businessKey = execution.getProcessBusinessKey();
		String executionId = execution.getId();

		String eventName = execution.getEventName();
		if("take".equals(eventName)) {
			TransitionImpl transition = (TransitionImpl) ((ExecutionEntity)execution).getEventSource();
			System.out.println("==flow【"+transition.getId()+"】触发");

		}
		System.out.println(this.getClass().getName()+"=================触发【"+eventName+"】事件,流程实例id【"+procInstId+"】");


		
	}

}
