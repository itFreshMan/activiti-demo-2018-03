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
	 * �������̶���
	 * a).���̲����
	 * b).��β���-���̶����(version)
	 */
	@Test
	public void a_deploy(){
		Deployment deployment = processEngine.getRepositoryService()//�����̶���Ͳ��������ص�Service
				.createDeployment()//����һ���������
				.name("demo����")//��Ӳ��������
				.addClasspathResource("diagrams/demo.bpmn")//��classpath����Դ�м��أ�һ��ֻ�ܼ���һ���ļ�
//				.addClasspathResource("diagrams/demo.png")//��classpath����Դ�м��أ�һ��ֻ�ܼ���һ���ļ�
				.deploy();//��ɲ���
		System.out.println("����ID��"+deployment.getId());//1
		System.out.println("�������ƣ�"+deployment.getName());//
	}
	
	/**
	 * ���̲����--zip
	 * a)һ��zip���԰������bpmn��xml
	 */
	@Test
	public void b_deploy_zip(){
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("diagrams/demo.zip");
		ZipInputStream zipInputStream = new ZipInputStream(in);
		Deployment deployment = processEngine.getRepositoryService()//�����̶���Ͳ��������ص�Service
				.createDeployment()//����һ���������
				.name("demo����")//��Ӳ��������
				.addZipInputStream(zipInputStream)//��classpath����Դ�м��أ�һ��ֻ�ܼ���һ���ļ�
				.deploy();//��ɲ���
		System.out.println("����ID��"+deployment.getId());//1
		System.out.println("�������ƣ�"+deployment.getName());//
	}
	
	/**��ѯ���̶���*/
	@Test
	public void c_findProcessDefinition(){
		List<ProcessDefinition> list = processEngine.getRepositoryService()//�����̶���Ͳ��������ص�Service
						.createProcessDefinitionQuery()//����һ�����̶���Ĳ�ѯ
						/**ָ����ѯ����,where����*/
//						.deploymentId(deploymentId)//ʹ�ò������ID��ѯ
//						.processDefinitionId(processDefinitionId)//ʹ�����̶���ID��ѯ
//						.processDefinitionKey(processDefinitionKey)//ʹ�����̶����key��ѯ
//						.processDefinitionNameLike(processDefinitionNameLike)//ʹ�����̶��������ģ����ѯ
						
						/**����*/
						.orderByProcessDefinitionVersion().asc()//���հ汾����������
//						.orderByProcessDefinitionName().desc()//�������̶�������ƽ�������
						
						/**���صĽ����*/
						.list();//����һ�������б���װ���̶���
//						.singleResult();//����Ωһ�����
//						.count();//���ؽ��������
//						.listPage(firstResult, maxResults);//��ҳ��ѯ
		if(list!=null && list.size()>0){
			for(ProcessDefinition pd:list){
				System.out.println("���̶���ID:"+pd.getId());//���̶����key+�汾+���������
				System.out.println("���̶��������:"+pd.getName());//��Ӧhelloworld.bpmn�ļ��е�name����ֵ
				System.out.println("���̶����key:"+pd.getKey());//��Ӧhelloworld.bpmn�ļ��е�id����ֵ
				System.out.println("���̶���İ汾:"+pd.getVersion());//�����̶����keyֵ��ͬ����ͬ�£��汾������Ĭ��1
				System.out.println("��Դ����bpmn�ļ�:"+pd.getResourceName());
				System.out.println("��Դ����png�ļ�:"+pd.getDiagramResourceName());
				System.out.println("�������ID��"+pd.getDeploymentId());
				System.out.println("#########################################################");
			}
		}			
	}

	
	/**
	 *
	 * �������̶���: ���ã�����
	 * �������ɾ�����̶���
	 * act_re_procdef.suspension_state_: 1:���2������
	 */
	@Test
	public void d_optProcessDefinition(){
		String processDefinitionId = "demo1:1:2504";
		//�������̶���ID��������,�����ͬʱ�Ѹ��������Ѿ�����������ʵ��Ҳȫ������
//		repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

		//�������̶���ID��������,�����ͬʱ�Ѹ��������Ѿ�����������ʵ��Ҳȫ������
//		repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
		
		
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		String deploymentId = processDefinition.getDeploymentId();
		
		//����ɾ��,��ɾ����������ִ�е�������Ϣ,�Լ���ʷ��Ϣ;
//		repositoryService.deleteDeployment(deploymentId, true);
	}
	
	/**
	 * ��������
	 * a).startById��startByKey������
	 * b).ִ�б� execution
	 */
	@Test
	public void e_start_processInstance(){
		String businessKey = "U_"+sdf.format(new Date());
		Map variables = new HashMap();
		variables.put("applicant", "JHS");//�����������̱���
		
		identityService.setAuthenticatedUserId("JHS"); //�������̷����� start_user_id
		
//		runtimeService.startProcessInstanceById("");
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY,businessKey,variables);
		System.out.println("����ʵ��ID:"+pi.getId());//����ʵ��ID    
		System.out.println("���̶���ID:"+pi.getProcessDefinitionId());//���̶���ID  
	}
	
	
	/**
	 * �鿴������task
	 * a).taskService.createTaskQuery() api
	 * b).�鿴����������������в����˱�,�����б�����
	 */
	@Test
	public void f_list_ru_tasks(){
		List<Task> taskList = taskService.createTaskQuery().processInstanceId("125001").orderByTaskId().asc().list();
		
/*		//�ҵ�����
		List<Task> assigneeTasks = taskService.createTaskQuery().taskAssignee("JHS").list();
		//�ҵ������� 
		List<Task> candidateTasks = taskService.createTaskQuery().taskCandidateUser("JHS").list();*/
		if(taskList!=null && taskList.size()>0){
			for(Task task : taskList){
				printTasks(task);
			}
		}
	}
	
	
	private void printTasks(Task task){
		System.out.println("########################################################");
		System.out.println("����ID:"+task.getId());
		System.out.println("��������:"+task.getName());
		System.out.println("����Ĵ���ʱ��:"+task.getCreateTime());
		System.out.println("����İ�����:"+task.getAssignee());
		System.out.println("����ʵ��ID��"+task.getProcessInstanceId());
		System.out.println("ִ�ж���ID:"+task.getExecutionId());
		System.out.println("���̶���ID:"+task.getProcessDefinitionId());
	
	}
	
	/**
	 * ǩ������
	 * a).���̲�������Ϣ
	 * b).ǩ�պ�������Ϣ
	 */
	@Test
	public void g_claim_task(){
		String taskId = "125006";
		String userId = "zzz";
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		
		printTasks(task);
		
		taskService.claim(taskId, userId);
		
		System.out.println("----------------------ǩ�պ�---------------------------");
		task = taskService.createTaskQuery().taskId(taskId).singleResult();
		printTasks(task);
		
	}
	
	
	/**
	 * ��ȡtask form����
	 */
	@Test
	public void h_open_taskPage(){
		String taskId = "62506";
		TaskFormData taskFormData = formService.getTaskFormData(taskId);
		System.out.println("########################################################");
		String formKey = taskFormData.getFormKey();
		System.out.println("formKey��"+formKey);

		List<FormProperty> listProperty = taskFormData.getFormProperties();
		if(!listProperty.isEmpty() && taskFormData.getFormProperties() != null) {
			FormProperty properties = taskFormData.getFormProperties().get(0);  
			System.out.println("�����ԡ�"+properties.getId()+"������"+properties.getName()+"������"+properties.getType().getInformation("values")+"��");
		}
	}
	
	/**
	 * �������
	 * a).�۲�ִ�б�
	 */
	@Test
	public void i_complete_task(){
		String taskId = "130005";
		Map variables = new HashMap();
		variables.put("approveUsers",Arrays.asList("boss1","boss2"));
		variables.put("pass","20"); //���صı��ʽ
		taskService.complete(taskId,variables); //���ر���
	}
	
	
	/**
	 * �鿴��ʷ�����
	 */
	@Test
	public void j_history_task(){
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()//������ʷ����ʵ����ѯ
				.processInstanceId("7501")
//				.finished() //finished������
				.list();
		
		if(list!=null && list.size()>0){
			for(HistoricTaskInstance hti:list){
				System.out.println("################################");
				System.out.println(hti.getId()+"    "+hti.getName()+"    "+hti.getProcessInstanceId()+"   "+hti.getStartTime()+"   "+hti.getEndTime()+"   "+hti.getDurationInMillis());
			}
		}
	}
	
	
	/**
	 * ���̱���
	 * a).�۲����̱�����;
	 * b). ���û������͡�javabean����
	 * 
	 * 1).���ס����ʵ����һ��ִ�е���tree of executions��ɣ�����local���ֲ�������, �ֲ�������ֻ�ڸ�ִ���пɼ�������ִ�������ϲ��򲻿ɼ���
	 * 2).������ִ��һ�������Գ��оֲ���������������Ϊ���������ʱ�䡣
	 * 3). ������ִ����������þֲ������� ���Ĭ������Ϊȫ�֣� ��������ʵ���е�����ִ�к����񶼿ɼ���(act_ru_variable��execution_id��process_inst_id_��ͬ)
	 */
	@Test
	public void k_ru_variables(){
		String processInstanceId = "112501";
		String executionId = "115006";
		String taskId = "115010";
		System.out.println("pass========"+runtimeService.getVariable(executionId, "pass"));

		
		runtimeService.setVariable(executionId, "flag", "11");//
		runtimeService.setVariableLocal(executionId, "xixi", "xx"); //һ�����̴��ڶ��executionʱ,
//		taskService.setVariable(taskId, "flag", "22");

		System.out.println(runtimeService.getVariable(executionId, "flag"));
		System.out.println(taskService.getVariable(taskId, "flag"));
		
	/*	System.out.println("-----------------------------");
		taskService.setVariableLocal(taskId, "flag", "33");
		System.out.println(runtimeService.getVariable(executionId, "flag"));
		System.out.println(taskService.getVariable(taskId, "flag"));
		*/
		//����javabean���Ͳ����� ����ʵ��ʵ����
		Person p = new Person();
		p.setId(20);
		p.setName("�仨");
		taskService.setVariable(taskId, "person", p);
		System.out.println(taskService.getVariable(taskId, "person"));

	}
	
	/**
	 * ��ʷ����
	 */
	@Test
	public void l_hi_variables(){
		String processInstanceId = "87501";
		List<HistoricVariableInstance> list = processEngine.getHistoryService()//
				.createHistoricVariableInstanceQuery()//����һ����ʷ�����̱�����ѯ����
//				.variableName("�������")
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
	 * ��ȡ--���̶���ͼ
	 * ��ȡ---���̶���xml
	 * @throws IOException 
	 */
	@Test
	public void z1_drawProcessDefPicture() throws IOException{
		String processDefinitionId = "demo1:11:102504";
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		if(pd != null){
			String deploymentId = pd.getDeploymentId();
			
			//���̶���ͼ
			String imageName = pd.getDiagramResourceName();
			InputStream isImage = repositoryService.getResourceAsStream(deploymentId,imageName);
			File image = new File("D:/diagram/"+imageName);
			FileUtils.copyInputStreamToFile(isImage, image);
			
			//���̶���xml
			String xmlName = pd.getResourceName();
			InputStream isXml = repositoryService.getResourceAsStream(deploymentId,xmlName);
			File xml = new File("D:/diagram/"+xmlName);
			FileUtils.copyInputStreamToFile(isXml, xml);
			
		}
	}
	/**
	 * ����-�������й켣ͼ
	 * @throws IOException 
	 */
	@Test
	public void z2_drawPicture() throws IOException{
		ProcessDiagramGeneratorExtend pen = new ProcessDiagramGeneratorExtend(repositoryService, runtimeService, historyService);
//		org.activiti.image.impl.ProcessDiagramGeneratorExtend pen = new org.activiti.image.impl.ProcessDiagramGeneratorExtend(repositoryService, runtimeService, historyService,processEngine.getProcessEngineConfiguration());
		String processInstanceId = "125001";
		ProcessInstance processInstance = processEngine.getRuntimeService()//
                .createProcessInstanceQuery()//��������ʵ����ѯ����
                .processInstanceId(processInstanceId)
                .singleResult();
		
		InputStream is = null;
		String fileName = null;
		if(processInstance == null){
			//�����ѽ���
			HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
			if(historicProcessInstance != null){
				is = pen.generateHistoryDiagram(processInstanceId);
				fileName = "history_";
			}
		}else{
			is = pen.generateDiagram(processInstanceId);
			fileName = "��";
		}
		
		if(is != null) {
			//��ͼƬ���ɵ�D�̵�Ŀ¼��
			File file = new File("D:/diagram/"+fileName+sdf.format(new Date())+".png");
			//����������ͼƬд��D����
			FileUtils.copyInputStreamToFile(is, file);
			
			System.out.println(file.getName()+"���ɳɹ�");
		}
		
	}
	
}
