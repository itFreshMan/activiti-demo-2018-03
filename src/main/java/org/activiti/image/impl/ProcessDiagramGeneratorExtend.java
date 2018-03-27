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
 * 流程图绘制工具
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
		
		
		//注意HIGHLIGHT_COLOR ，为protected属性， 所以本类必须放在同一package目录， 或者重写此类
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
	 * 流程图
	 *
	 * @param processInstanceId
	 * @return
	 * @throws IOException
	 */
	public InputStream generateDiagram(String processInstanceId, boolean runtimeFlag) throws IOException {
		//获取历史流程实例
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		//获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
		
		ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
		
		//高亮线路id集合
		List<String> highLightedFlows = getHighLightedFlows(definition, historicActivityInstanceList);
		
		//只画flow线,流程节点绘制--手动完成;----api自带的绘制功能，不太友好，故自己实现;
		InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel, "png", new ArrayList<String>(), highLightedFlows, "宋体", "宋体", "宋体", null, 1.0);
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
	 * 绘制--运行中流程图
	 *
	 * @param processInstanceId
	 * @return
	 * @throws IOException
	 */
	public InputStream generateDiagramRuntime(String processInstanceId) throws IOException {
		return generateDiagram(processInstanceId, true);
	}
	
	/**
	 * 查询历史流程图
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
	 * 获取需要高亮的线
	 *
	 * @param processDefinitionEntity
	 * @param historicActivityInstances
	 * @return
	 */
	private List<String> getHighLightedFlows(ProcessDefinitionEntity processDefinitionEntity, List<HistoricActivityInstance> historicActivityInstances) {
		if(1<2){
			  List<String> highFlows = new ArrayList<String>();// 用以保存高亮的线flowId
		        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {// 对历史流程节点进行遍历
		            ActivityImpl activityImpl = processDefinitionEntity
		                    .findActivity(historicActivityInstances.get(i)
		                            .getActivityId());// 得到节点定义的详细信息
		            List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();// 用以保存后需开始时间相同的节点
		            ActivityImpl sameActivityImpl1 = processDefinitionEntity
		                    .findActivity(historicActivityInstances.get(i + 1)
		                            .getActivityId());
		            // 将后面第一个节点放在时间相同节点的集合里
		            sameStartTimeNodes.add(sameActivityImpl1);
		            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
		                HistoricActivityInstance activityImpl1 = historicActivityInstances
		                        .get(j);// 后续第一个节点
		                HistoricActivityInstance activityImpl2 = historicActivityInstances
		                        .get(j + 1);// 后续第二个节点
		                if (activityImpl1.getStartTime().equals(
		                        activityImpl2.getStartTime())) {
		                    // 如果第一个节点和第二个节点开始时间相同保存
		                    ActivityImpl sameActivityImpl2 = processDefinitionEntity
		                            .findActivity(activityImpl2.getActivityId());
		                    sameStartTimeNodes.add(sameActivityImpl2);
		                } else {
		                    // 有不相同跳出循环
		                    break;
		                }
		            }
		            List<PvmTransition> pvmTransitions = activityImpl
		                    .getOutgoingTransitions();// 取出节点的所有出去的线
		            for (PvmTransition pvmTransition : pvmTransitions) {
		                // 对所有的线进行遍历
		                ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition
		                        .getDestination();
		                // 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
		                if (sameStartTimeNodes.contains(pvmActivityImpl)) {
		                    highFlows.add(pvmTransition.getId());
		                }
		            }
		        }
		        return highFlows;
		}
		Set<String> flowIdSet = new HashSet<>();// 用以保存高亮的线flowId
		List<ActivityImpl> listActivity = new ArrayList();
		for(HistoricActivityInstance historicActivityInstance : historicActivityInstances){
			String activityId = historicActivityInstance.getActivityId();
			listActivity.add( processDefinitionEntity.findActivity(activityId));
		}
	
		
		
		for (ActivityImpl src : listActivity) {
			List<PvmTransition> incomingTransitions = src.getIncomingTransitions();// 来源线路
			if (incomingTransitions.size() == 1) {
				flowIdSet.add(incomingTransitions.get(0).getId());
			}
			
			List<PvmTransition> outTransitions = src.getOutgoingTransitions();// 获取从源节点出来的所有线路
			if (outTransitions.size() == 1) {
				String destid = outTransitions.get(0).getDestination().getId();
				for(HistoricActivityInstance historicActivityInstance : historicActivityInstances){
					if(historicActivityInstance.getEndTime() != null && historicActivityInstance.getActivityId().equals(destid)){
						flowIdSet.add(outTransitions.get(0).getId());
					}
				}
			}
			
			String sourceid = (String) src.getId(); // 获取源id
			for (PvmTransition transition : outTransitions) {
				String transitionId = transition.getId();
				if(flowIdSet.contains(transitionId)){
					continue;
				}
				PvmActivity dest = transition.getDestination();// 获取线路所有的目标id
				String endid = dest.getId();
				if (ChooseRighLine(listActivity, sourceid, endid)) {
					flowIdSet.add(transitionId);
				}
			}
		}
		
		//如果存在打回的情况：
		
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
	 * 标记运行节点
	 *
	 * @param image        原始图片
	 * @param x            左上角节点坐在X位置
	 * @param y            左上角节点坐在Y位置
	 * @param width        宽
	 * @param height       高
	 * @param activityType 节点类型
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
	 * 标记历史节点
	 *
	 * @param image        原始图片
	 * @param x            左上角节点坐在X位置
	 * @param y            左上角节点坐在Y位置
	 * @param width        宽
	 * @param height       高
	 * @param activityType 节点类型
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
	 * 绘制节点边框
	 *
	 * @param x            左上角节点坐在X位置
	 * @param y            左上角节点坐在Y位置
	 * @param width        宽
	 * @param height       高
	 * @param graphics     绘图对象
	 * @param color        节点边框颜色
	 * @param activityType 节点类型
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
	 * 绘制任务
	 */
	protected static void drawTask(int x, int y, int width, int height, Graphics2D graphics) {
		RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, 20, 20);
		graphics.draw(rect);
	}
	
	/**
	 * 绘制网关
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
	 * 绘制任务
	 */
	protected static void drawEvent(int x, int y, int width, int height, Graphics2D graphics) {
		Double circle = new Double(x, y, width, height);
		graphics.draw(circle);
	}
	
	/**
	 * 绘制子流程
	 */
	protected static void drawSubProcess(int x, int y, int width, int height, Graphics2D graphics) {
		RoundRectangle2D rect = new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 5, 5);
		graphics.draw(rect);
	}
	
}

