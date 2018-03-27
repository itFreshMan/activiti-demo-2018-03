package org.activiti.image.impl;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D.Double;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ����ͼ���ƹ���
 */
public class ProcessDiagramGeneratorExtend {
	private static Color RUNNING_COLOR = Color.RED;
	private static Color HISTORY_COLOR = Color.GREEN;
	private static Stroke THICK_BORDER_STROKE = new BasicStroke(3.0f);
	
	private static List<String> taskType = new ArrayList<String>();
	private static List<String> eventType = new ArrayList<String>();
	private static List<String> gatewayType = new ArrayList<String>();
	private static List<String> subProcessType = new ArrayList<String>();
	
	static {
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_MANUAL);
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_RECEIVE);
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_SCRIPT);
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_SEND);
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_SERVICE);
		taskType.add(BpmnXMLConstants.ELEMENT_TASK_USER);
		
		gatewayType.add(BpmnXMLConstants.ELEMENT_GATEWAY_EXCLUSIVE);
		gatewayType.add(BpmnXMLConstants.ELEMENT_GATEWAY_INCLUSIVE);
		gatewayType.add(BpmnXMLConstants.ELEMENT_GATEWAY_EVENT);
		gatewayType.add(BpmnXMLConstants.ELEMENT_GATEWAY_PARALLEL);
		
		eventType.add("intermediateTimer");
		eventType.add("intermediateMessageCatch");
		eventType.add("intermediateSignalCatch");
		eventType.add("intermediateSignalThrow");
		eventType.add("messageStartEvent");
		eventType.add("startTimerEvent");
		eventType.add(BpmnXMLConstants.ELEMENT_ERROR);
		eventType.add(BpmnXMLConstants.ELEMENT_EVENT_START);
		eventType.add("errorEndEvent");
		eventType.add(BpmnXMLConstants.ELEMENT_EVENT_END);
		
		subProcessType.add(BpmnXMLConstants.ELEMENT_SUBPROCESS);
		subProcessType.add(BpmnXMLConstants.ELEMENT_CALL_ACTIVITY);
		
		
		//ע��HIGHLIGHT_COLOR ��Ϊprotected���ԣ� ���Ա���������ͬһpackageĿ¼�� ������д����
		DefaultProcessDiagramCanvas.HIGHLIGHT_COLOR = HISTORY_COLOR;
	}
	
	private ProcessEngineConfiguration processEngineConfiguration;
	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private HistoryService historyService;
	
	private ProcessDiagramGenerator diagramGenerator;
	
	public ProcessDiagramGeneratorExtend(RepositoryService repositoryService, RuntimeService runtimeService,
	                                     HistoryService historyService, ProcessEngineConfiguration processEngineConfiguration) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.historyService = historyService;
		this.processEngineConfiguration = processEngineConfiguration;
		
		diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
	}
	
	
	/**
	 * ����ͼ
	 *
	 * @param processInstanceId
	 * @return
	 * @throws IOException
	 */
	public InputStream generateDiagram(String processInstanceId, boolean runtimeFlag) throws IOException {
		//��ȡ��ʷ����ʵ��
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		//��ȡ����ͼ
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
		
		ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
		
		//������·id����
		List<String> highLightedFlows = getHighLightedFlows(definition, historicActivityInstanceList);
		
		//ֻ��flow��,���̽ڵ����--�ֶ����;----api�Դ��Ļ��ƹ��ܣ���̫�Ѻã����Լ�ʵ��;
		InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel, "png", new ArrayList<String>(), highLightedFlows, "����", "����", "����", null, 1.0);
		BufferedImage image = ImageIO.read(imageStream);
		
		for (HistoricActivityInstance historicActivityInstance : historicActivityInstanceList) {
			String historicActivityId = historicActivityInstance.getActivityId();
			ActivityImpl activity = definition.findActivity(historicActivityId);
			if (historicActivityInstance.getEndTime() != null) {
				signHistoryNode(
						image, //
						activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
						historicActivityInstance.getActivityType());
			} else {
				if (runtimeFlag) {
					signRunningNode(
							image, //
							activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
							historicActivityInstance.getActivityType());
				}
			}
		}
		
		
		String diagramResourceName = definition.getDiagramResourceName();
		String formatName = getDiagramExtension(diagramResourceName);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, formatName, out);
		return new ByteArrayInputStream(out.toByteArray());
	}
	
	/**
	 * ����--����������ͼ
	 *
	 * @param processInstanceId
	 * @return
	 * @throws IOException
	 */
	public InputStream generateDiagramRuntime(String processInstanceId) throws IOException {
		return generateDiagram(processInstanceId, true);
	}
	
	/**
	 * ��ѯ��ʷ����ͼ
	 *
	 * @param processInstanceId
	 * @return
	 * @throws IOException
	 */
	public InputStream generateHistoryDiagram(String processInstanceId) throws IOException {
		return generateDiagram(processInstanceId, false);
	}
	
	private static String getDiagramExtension(String diagramResourceName) {
		return FilenameUtils.getExtension(diagramResourceName);
	}
	
	/**
	 * ��ȡ��Ҫ��������
	 *
	 * @param processDefinitionEntity
	 * @param historicActivityInstances
	 * @return
	 */
	private List<String> getHighLightedFlows(ProcessDefinitionEntity processDefinitionEntity, List<HistoricActivityInstance> historicActivityInstances) {
		if(1<2){
			  List<String> highFlows = new ArrayList<String>();// ���Ա����������flowId
		        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {// ����ʷ���̽ڵ���б���
		            ActivityImpl activityImpl = processDefinitionEntity
		                    .findActivity(historicActivityInstances.get(i)
		                            .getActivityId());// �õ��ڵ㶨�����ϸ��Ϣ
		            List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();// ���Ա�����迪ʼʱ����ͬ�Ľڵ�
		            ActivityImpl sameActivityImpl1 = processDefinitionEntity
		                    .findActivity(historicActivityInstances.get(i + 1)
		                            .getActivityId());
		            // �������һ���ڵ����ʱ����ͬ�ڵ�ļ�����
		            sameStartTimeNodes.add(sameActivityImpl1);
		            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
		                HistoricActivityInstance activityImpl1 = historicActivityInstances
		                        .get(j);// ������һ���ڵ�
		                HistoricActivityInstance activityImpl2 = historicActivityInstances
		                        .get(j + 1);// �����ڶ����ڵ�
		                if (activityImpl1.getStartTime().equals(
		                        activityImpl2.getStartTime())) {
		                    // �����һ���ڵ�͵ڶ����ڵ㿪ʼʱ����ͬ����
		                    ActivityImpl sameActivityImpl2 = processDefinitionEntity
		                            .findActivity(activityImpl2.getActivityId());
		                    sameStartTimeNodes.add(sameActivityImpl2);
		                } else {
		                    // �в���ͬ����ѭ��
		                    break;
		                }
		            }
		            List<PvmTransition> pvmTransitions = activityImpl
		                    .getOutgoingTransitions();// ȡ���ڵ�����г�ȥ����
		            for (PvmTransition pvmTransition : pvmTransitions) {
		                // �����е��߽��б���
		                ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition
		                        .getDestination();
		                // ���ȡ�����ߵ�Ŀ��ڵ����ʱ����ͬ�Ľڵ��������ߵ�id�����и�����ʾ
		                if (sameStartTimeNodes.contains(pvmActivityImpl)) {
		                    highFlows.add(pvmTransition.getId());
		                }
		            }
		        }
		        return highFlows;
		}
		Set<String> flowIdSet = new HashSet<>();// ���Ա����������flowId
		List<ActivityImpl> listActivity = new ArrayList();
		for(HistoricActivityInstance historicActivityInstance : historicActivityInstances){
			String activityId = historicActivityInstance.getActivityId();
			listActivity.add( processDefinitionEntity.findActivity(activityId));
		}
	
		
		
		for (ActivityImpl src : listActivity) {
			List<PvmTransition> incomingTransitions = src.getIncomingTransitions();// ��Դ��·
			if (incomingTransitions.size() == 1) {
				flowIdSet.add(incomingTransitions.get(0).getId());
			}
			
			List<PvmTransition> outTransitions = src.getOutgoingTransitions();// ��ȡ��Դ�ڵ������������·
			if (outTransitions.size() == 1) {
				String destid = outTransitions.get(0).getDestination().getId();
				for(HistoricActivityInstance historicActivityInstance : historicActivityInstances){
					if(historicActivityInstance.getEndTime() != null && historicActivityInstance.getActivityId().equals(destid)){
						flowIdSet.add(outTransitions.get(0).getId());
					}
				}
			}
			
			String sourceid = (String) src.getId(); // ��ȡԴid
			for (PvmTransition transition : outTransitions) {
				String transitionId = transition.getId();
				if(flowIdSet.contains(transitionId)){
					continue;
				}
				PvmActivity dest = transition.getDestination();// ��ȡ��·���е�Ŀ��id
				String endid = dest.getId();
				if (ChooseRighLine(listActivity, sourceid, endid)) {
					flowIdSet.add(transitionId);
				}
			}
		}
		
		//������ڴ�ص������
		
		return new ArrayList<>(flowIdSet);
	}
	
	private boolean ChooseRighLine(List<ActivityImpl> activity, String sourceid, String endid) {
		boolean flag = false;
		List<ActivityImpl> prehistoryNodesList = new ArrayList<ActivityImpl>();
		List<ActivityImpl> sufhistoryNodesList = new ArrayList<ActivityImpl>();
		prehistoryNodesList = activity.subList(0, activity.size() - 1);
		sufhistoryNodesList = activity.subList(1, activity.size());
		for (int i = 0; i < prehistoryNodesList.size(); i++) {
			if (prehistoryNodesList.get(i).getId().equals(sourceid) &&
					sufhistoryNodesList.get(i).getId().equals(endid))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * ������нڵ�
	 *
	 * @param image        ԭʼͼƬ
	 * @param x            ���Ͻǽڵ�����Xλ��
	 * @param y            ���Ͻǽڵ�����Yλ��
	 * @param width        ��
	 * @param height       ��
	 * @param activityType �ڵ�����
	 */
	private static void signRunningNode(BufferedImage image, int x, int y, int width, int height, String activityType) {
		Color nodeColor = RUNNING_COLOR;
		Graphics2D graphics = image.createGraphics();
		try {
			drawNodeBorder(x, y, width, height, graphics, nodeColor, activityType);
		} finally {
			graphics.dispose();
			
		}
	}
	
	/**
	 * �����ʷ�ڵ�
	 *
	 * @param image        ԭʼͼƬ
	 * @param x            ���Ͻǽڵ�����Xλ��
	 * @param y            ���Ͻǽڵ�����Yλ��
	 * @param width        ��
	 * @param height       ��
	 * @param activityType �ڵ�����
	 */
	private static void signHistoryNode(BufferedImage image, int x, int y, int width, int height, String activityType) {
		Color nodeColor = HISTORY_COLOR;
		Graphics2D graphics = image.createGraphics();
		try {
			drawNodeBorder(x, y, width, height, graphics, nodeColor, activityType);
		} finally {
			graphics.dispose();
		}
	}
	
	/**
	 * ���ƽڵ�߿�
	 *
	 * @param x            ���Ͻǽڵ�����Xλ��
	 * @param y            ���Ͻǽڵ�����Yλ��
	 * @param width        ��
	 * @param height       ��
	 * @param graphics     ��ͼ����
	 * @param color        �ڵ�߿���ɫ
	 * @param activityType �ڵ�����
	 */
	protected static void drawNodeBorder(int x, int y, int width, int height, Graphics2D graphics, Color color,
	                                     String activityType) {
		graphics.setPaint(color);
		graphics.setStroke(THICK_BORDER_STROKE);
		if (taskType.contains(activityType)) {
			drawTask(x, y, width, height, graphics);
		} else if (gatewayType.contains(activityType)) {
			drawGateway(x, y, width, height, graphics);
		} else if (eventType.contains(activityType)) {
			drawEvent(x, y, width, height, graphics);
		} else if (subProcessType.contains(activityType)) {
			drawSubProcess(x, y, width, height, graphics);
		}
	}
	
	/**
	 * ��������
	 */
	protected static void drawTask(int x, int y, int width, int height, Graphics2D graphics) {
		RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, 20, 20);
		graphics.draw(rect);
	}
	
	/**
	 * ��������
	 */
	protected static void drawGateway(int x, int y, int width, int height, Graphics2D graphics) {
		Polygon rhombus = new Polygon();
		rhombus.addPoint(x, y + (height / 2));
		rhombus.addPoint(x + (width / 2), y + height);
		rhombus.addPoint(x + width, y + (height / 2));
		rhombus.addPoint(x + (width / 2), y);
		graphics.draw(rhombus);
	}
	
	/**
	 * ��������
	 */
	protected static void drawEvent(int x, int y, int width, int height, Graphics2D graphics) {
		Double circle = new Double(x, y, width, height);
		graphics.draw(circle);
	}
	
	/**
	 * ����������
	 */
	protected static void drawSubProcess(int x, int y, int width, int height, Graphics2D graphics) {
		RoundRectangle2D rect = new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 5, 5);
		graphics.draw(rect);
	}
	
}

