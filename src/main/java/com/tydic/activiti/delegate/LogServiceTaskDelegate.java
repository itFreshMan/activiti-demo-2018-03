package com.tydic.activiti.delegate;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class LogServiceTaskDelegate implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		
		String businessKey = execution.getProcessBusinessKey();
//		String activitiId = execution.getCurrentActivityId(); 
		String activitiName = execution.getCurrentActivityName();
		System.out.println(this.getClass().getName()+"=================执行到【"+activitiName+"】,对应业务主键为【"+businessKey+"】");
		
	}

}
