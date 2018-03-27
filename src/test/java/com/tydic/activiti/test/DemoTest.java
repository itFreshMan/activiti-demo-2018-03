package com.tydic.activiti.test;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipInputStream;

import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.bpm.engine.extend.diagram.ProcessDiagramGeneratorExtend;
import com.tydic.activiti.dto.Person;


public class DemoTest {
	
	ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
	RepositoryService repositoryService = processEngine.getRepositoryService();
	RuntimeService runtimeService = processEngine.getRuntimeService();
	IdentityService identityService = processEngine.getIdentityService();
	TaskService taskService = processEngine.getTaskService();
	FormService formService = processEngine.getFormService();
	HistoryService historyService = processEngine.getHistoryService();
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
	
	public static final String PROCESS_KEY="demo1";
	
	/**
	 * 部署流程定义
	 * a).流程部署表
	 * b).多次部署-流程定义表(version)
	 */
	@Test
	public void a_deploy(){
		Deployment deployment = processEngine.getRepositoryService()//与流程定义和部署对象相关的Service
				.createDeployment()//创建一个部署对象
				.name("demo入门")//添加部署的名称
				.addClasspathResource("diagrams/demo.bpmn")//从classpath的资源中加载，一次只能加载一个文件
//				.addClasspathResource("diagrams/demo.png")//从classpath的资源中加载，一次只能加载一个文件
				.deploy();//完成部署
		System.out.println("部署ID："+deployment.getId());//1
		System.out.println("部署名称："+deployment.getName());//
	}
	
	/**
	 * 流程部署表--zip
	 * a)一个zip可以包含多个bpmn，xml
	 */
	@Test
	public void b_deploy_zip(){
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("diagrams/demo.zip");
		ZipInputStream zipInputStream = new ZipInputStream(in);
		Deployment deployment = processEngine.getRepositoryService()//与流程定义和部署对象相关的Service
				.createDeployment()//创建一个部署对象
				.name("demo入门")//添加部署的名称
				.addZipInputStream(zipInputStream)//从classpath的资源中加载，一次只能加载一个文件
				.deploy();//完成部署
		System.out.println("部署ID："+deployment.getId());//1
		System.out.println("部署名称："+deployment.getName());//
	}
	
	/**查询流程定义*/
	@Test
	public void c_findProcessDefinition(){
		List<ProcessDefinition> list = processEngine.getRepositoryService()//与流程定义和部署对象相关的Service
						.createProcessDefinitionQuery()//创建一个流程定义的查询
						/**指定查询条件,where条件*/
//						.deploymentId(deploymentId)//使用部署对象ID查询
//						.processDefinitionId(processDefinitionId)//使用流程定义ID查询
//						.processDefinitionKey(processDefinitionKey)//使用流程定义的key查询
//						.processDefinitionNameLike(processDefinitionNameLike)//使用流程定义的名称模糊查询
						
						/**排序*/
						.orderByProcessDefinitionVersion().asc()//按照版本的升序排列
//						.orderByProcessDefinitionName().desc()//按照流程定义的名称降序排列
						
						/**返回的结果集*/
						.list();//返回一个集合列表，封装流程定义
//						.singleResult();//返回惟一结果集
//						.count();//返回结果集数量
//						.listPage(firstResult, maxResults);//分页查询
		if(list!=null && list.size()>0){
			for(ProcessDefinition pd:list){
				System.out.println("流程定义ID:"+pd.getId());//流程定义的key+版本+随机生成数
				System.out.println("流程定义的名称:"+pd.getName());//对应helloworld.bpmn文件中的name属性值
				System.out.println("流程定义的key:"+pd.getKey());//对应helloworld.bpmn文件中的id属性值
				System.out.println("流程定义的版本:"+pd.getVersion());//当流程定义的key值相同的相同下，版本升级，默认1
				System.out.println("资源名称bpmn文件:"+pd.getResourceName());
				System.out.println("资源名称png文件:"+pd.getDiagramResourceName());
				System.out.println("部署对象ID："+pd.getDeploymentId());
				System.out.println("#########################################################");
			}
		}			
	}

	
	/**
	 *
	 * 操作流程定义: 慎用！！！
	 * 激活，挂起，删除流程定义
	 * act_re_procdef.suspension_state_: 1:激活，2：挂起
	 */
	@Test
	public void d_optProcessDefinition(){
		String processDefinitionId = "demo1:1:2504";
		//根据流程定义ID激活流程,激活的同时把该流程下已经启动的流程实例也全部激活
//		repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

		//根据流程定义ID激活流程,激活的同时把该流程下已经启动的流程实例也全部激活
//		repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
		
		
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		String deploymentId = processDefinition.getDeploymentId();
		
		//级联删除,会删除当下正在执行的流程信息,以及历史信息;
//		repositoryService.deleteDeployment(deploymentId, true);
	}
	
	/**
	 * 开启流程
	 * a).startById和startByKey的区别
	 * b).执行表 execution
	 */
	@Test
	public void e_start_processInstance(){
		String businessKey = "U_"+sdf.format(new Date());
		Map variables = new HashMap();
		variables.put("applicant", "JHS");//设置申请流程变量
		
		identityService.setAuthenticatedUserId("JHS"); //设置流程发起人 start_user_id
		
//		runtimeService.startProcessInstanceById("");
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY,businessKey,variables);
		System.out.println("流程实例ID:"+pi.getId());//流程实例ID    
		System.out.println("流程定义ID:"+pi.getProcessDefinitionId());//流程定义ID  
	}
	
	
	/**
	 * 查看运行中task
	 * a).taskService.createTaskQuery() api
	 * b).查看运行中任务表，运行中参与人表,运行中变量表
	 */
	@Test
	public void f_list_ru_tasks(){
		List<Task> taskList = taskService.createTaskQuery().processInstanceId("125001").orderByTaskId().asc().list();
		
/*		//我的任务
		List<Task> assigneeTasks = taskService.createTaskQuery().taskAssignee("JHS").list();
		//我的组任务 
		List<Task> candidateTasks = taskService.createTaskQuery().taskCandidateUser("JHS").list();*/
		if(taskList!=null && taskList.size()>0){
			for(Task task : taskList){
				printTasks(task);
			}
		}
	}
	
	
	private void printTasks(Task task){
		System.out.println("########################################################");
		System.out.println("任务ID:"+task.getId());
		System.out.println("任务名称:"+task.getName());
		System.out.println("任务的创建时间:"+task.getCreateTime());
		System.out.println("任务的办理人:"+task.getAssignee());
		System.out.println("流程实例ID："+task.getProcessInstanceId());
		System.out.println("执行对象ID:"+task.getExecutionId());
		System.out.println("流程定义ID:"+task.getProcessDefinitionId());
	
	}
	
	/**
	 * 签收任务
	 * a).流程参与人信息
	 * b).签收后任务信息
	 */
	@Test
	public void g_claim_task(){
		String taskId = "125006";
		String userId = "zzz";
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		
		printTasks(task);
		
		taskService.claim(taskId, userId);
		
		System.out.println("----------------------签收后---------------------------");
		task = taskService.createTaskQuery().taskId(taskId).singleResult();
		printTasks(task);
		
	}
	
	
	/**
	 * 获取task form属性
	 */
	@Test
	public void h_open_taskPage(){
		String taskId = "62506";
		TaskFormData taskFormData = formService.getTaskFormData(taskId);
		System.out.println("########################################################");
		String formKey = taskFormData.getFormKey();
		System.out.println("formKey："+formKey);

		List<FormProperty> listProperty = taskFormData.getFormProperties();
		if(!listProperty.isEmpty() && taskFormData.getFormProperties() != null) {
			FormProperty properties = taskFormData.getFormProperties().get(0);  
			System.out.println("表单属性【"+properties.getId()+"】、【"+properties.getName()+"】、【"+properties.getType().getInformation("values")+"】");
		}
	}
	
	/**
	 * 完成任务
	 * a).观察执行表
	 */
	@Test
	public void i_complete_task(){
		String taskId = "130005";
		Map variables = new HashMap();
		variables.put("approveUsers",Arrays.asList("boss1","boss2"));
		variables.put("pass","20"); //网关的表达式
		taskService.complete(taskId,variables); //本地变量
	}
	
	
	/**
	 * 查看历史任务表
	 */
	@Test
	public void j_history_task(){
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()//创建历史任务实例查询
				.processInstanceId("7501")
//				.finished() //finished的作用
				.list();
		
		if(list!=null && list.size()>0){
			for(HistoricTaskInstance hti:list){
				System.out.println("################################");
				System.out.println(hti.getId()+"    "+hti.getName()+"    "+hti.getProcessInstanceId()+"   "+hti.getStartTime()+"   "+hti.getEndTime()+"   "+hti.getDurationInMillis());
			}
		}
	}
	
	
	/**
	 * 流程变量
	 * a).观察流程变量表;
	 * b). 设置基础类型、javabean类型
	 * 
	 * 1).请记住流程实例由一颗执行的树tree of executions组成，设置local（局部）变量, 局部变量将只在该执行中可见，而对执行树的上层则不可见。
	 * 2).任务与执行一样，可以持有局部变量，其生存期为任务持续的时间。
	 * 3). 任务与执行如果不设置局部变量， 则会默认设置为全局， 整个流程实例中的所有执行和任务都可见。(act_ru_variable中execution_id和process_inst_id_相同)
	 */
	@Test
	public void k_ru_variables(){
		String processInstanceId = "112501";
		String executionId = "115006";
		String taskId = "115010";
		System.out.println("pass========"+runtimeService.getVariable(executionId, "pass"));

		
		runtimeService.setVariable(executionId, "flag", "11");//
		runtimeService.setVariableLocal(executionId, "xixi", "xx"); //一个流程存在多个execution时,
//		taskService.setVariable(taskId, "flag", "22");

		System.out.println(runtimeService.getVariable(executionId, "flag"));
		System.out.println(taskService.getVariable(taskId, "flag"));
		
	/*	System.out.println("-----------------------------");
		taskService.setVariableLocal(taskId, "flag", "33");
		System.out.println(runtimeService.getVariable(executionId, "flag"));
		System.out.println(taskService.getVariable(taskId, "flag"));
		*/
		//设置javabean类型参数， 必须实现实例化
		Person p = new Person();
		p.setId(20);
		p.setName("翠花");
		taskService.setVariable(taskId, "person", p);
		System.out.println(taskService.getVariable(taskId, "person"));

	}
	
	/**
	 * 历史变量
	 */
	@Test
	public void l_hi_variables(){
		String processInstanceId = "87501";
		List<HistoricVariableInstance> list = processEngine.getHistoryService()//
				.createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询对象
//				.variableName("请假天数")
				.processInstanceId(processInstanceId)
				.list();
		if(list!=null && list.size()>0){
			for(HistoricVariableInstance hvi:list){
				System.out.println(hvi.getId()+"   "+hvi.getProcessInstanceId()+"   "+hvi.getVariableName()+"   "+hvi.getVariableTypeName()+"    "+hvi.getValue());
				System.out.println("###############################################");
			}
		}
	}
	
	
	
	/**
	 * 获取--流程定义图
	 * 获取---流程定义xml
	 * @throws IOException 
	 */
	@Test
	public void z1_drawProcessDefPicture() throws IOException{
		String processDefinitionId = "demo1:11:102504";
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		if(pd != null){
			String deploymentId = pd.getDeploymentId();
			
			//流程定义图
			String imageName = pd.getDiagramResourceName();
			InputStream isImage = repositoryService.getResourceAsStream(deploymentId,imageName);
			File image = new File("D:/diagram/"+imageName);
			FileUtils.copyInputStreamToFile(isImage, image);
			
			//流程定义xml
			String xmlName = pd.getResourceName();
			InputStream isXml = repositoryService.getResourceAsStream(deploymentId,xmlName);
			File xml = new File("D:/diagram/"+xmlName);
			FileUtils.copyInputStreamToFile(isXml, xml);
			
		}
	}
	/**
	 * 绘制-流程运行轨迹图
	 * @throws IOException 
	 */
	@Test
	public void z2_drawPicture() throws IOException{
		ProcessDiagramGeneratorExtend pen = new ProcessDiagramGeneratorExtend(repositoryService, runtimeService, historyService);
//		org.activiti.image.impl.ProcessDiagramGeneratorExtend pen = new org.activiti.image.impl.ProcessDiagramGeneratorExtend(repositoryService, runtimeService, historyService,processEngine.getProcessEngineConfiguration());
		String processInstanceId = "125001";
		ProcessInstance processInstance = processEngine.getRuntimeService()//
                .createProcessInstanceQuery()//创建流程实例查询对象
                .processInstanceId(processInstanceId)
                .singleResult();
		
		InputStream is = null;
		String fileName = null;
		if(processInstance == null){
			//流程已结束
			HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
			if(historicProcessInstance != null){
				is = pen.generateHistoryDiagram(processInstanceId);
				fileName = "history_";
			}
		}else{
			is = pen.generateDiagram(processInstanceId);
			fileName = "—";
		}
		
		if(is != null) {
			//将图片生成到D盘的目录下
			File file = new File("D:/diagram/"+fileName+sdf.format(new Date())+".png");
			//将输入流的图片写到D盘下
			FileUtils.copyInputStreamToFile(is, file);
			
			System.out.println(file.getName()+"生成成功");
		}
		
	}
	
}
