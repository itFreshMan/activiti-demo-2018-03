package com.tydic.activiti.listener;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;


public class UserTaskListener implements TaskListener {

	@Override
	public void notify(DelegateTask delegateTask) {
		String taskDefKey = delegateTask.getTaskDefinitionKey();
		String processDefId = delegateTask.getProcessDefinitionId();
		String processDefKey = processDefId.split(":")[0];
	
		String businessKey = delegateTask.getExecution().getProcessBusinessKey();
		System.out.println(this.getClass().getName()+"=================ִ�е���"+taskDefKey+"��,��Ӧҵ������Ϊ��"+businessKey+"��");
		
		
		if("apply".equals(taskDefKey)){
//			delegateTask.setAssignee("_listener");
		}

	}

}
