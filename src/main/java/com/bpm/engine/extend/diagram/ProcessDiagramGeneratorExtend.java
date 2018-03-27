package com.bpm.engine.extend.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.io.FilenameUtils;

/**
 * 流程图绘制工具
 */
public class ProcessDiagramGeneratorExtend {

    private static List<String> taskType = new ArrayList<String>();
    private static List<String> eventType = new ArrayList<String>();
    private static List<String> gatewayType = new ArrayList<String>();
    private static List<String> subProcessType = new ArrayList<String>();

    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private HistoryService historyService;

    public ProcessDiagramGeneratorExtend(RepositoryService repositoryService, RuntimeService runtimeService,
            HistoryService historyService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        init();
    }

    protected static void init() {
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
        
        
    }

    private static Color RUNNING_COLOR = Color.RED;
    private static Color HISTORY_COLOR = Color.GREEN;
    private static Stroke THICK_BORDER_STROKE = new BasicStroke(3.0f);

    /**
     * 查询运行中流程图
     * @param processInstanceId
     * @return
     * @throws IOException
     */
    public InputStream generateDiagram(String processInstanceId) throws IOException {
    	//历史环节
    	List<ActivityImpl> listActivity = new ArrayList<ActivityImpl>();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                .getDeployedProcessDefinition(processDefinitionId);
        String diagramResourceName = definition.getDiagramResourceName();
        String deploymentId = definition.getDeploymentId();
        InputStream originDiagram = repositoryService.getResourceAsStream(deploymentId, diagramResourceName);
        BufferedImage image = ImageIO.read(originDiagram);

        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();
        for (HistoricActivityInstance historicActivityInstance : activityInstances) {
            String historicActivityId = historicActivityInstance.getActivityId();
            ActivityImpl activity = definition.findActivity(historicActivityId);
            listActivity.add(activity);
            if (activity != null) {
                if (historicActivityInstance.getEndTime() == null) {// 节点正在运行中
                    signRunningNode(
                            image, //
                            activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
                            historicActivityInstance.getActivityType());
                } else {// 节点已经结束
                    signHistoryNode(
                            image, //
                            activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
                            historicActivityInstance.getActivityType());
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String formatName = getDiagramExtension(diagramResourceName);
        ImageIO.write(image, formatName, out);
        return new ByteArrayInputStream(out.toByteArray());

    }

    /**
     * 查询历史流程图
     * @param processInstanceId
     * @return
     * @throws IOException
     */
    public InputStream generateHistoryDiagram(String processInstanceId) throws IOException {
    	HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                .getDeployedProcessDefinition(processDefinitionId);
        String diagramResourceName = definition.getDiagramResourceName();
        String deploymentId = definition.getDeploymentId();
        InputStream originDiagram = repositoryService.getResourceAsStream(deploymentId, diagramResourceName);
        BufferedImage image = ImageIO.read(originDiagram);

        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();
        for (HistoricActivityInstance historicActivityInstance : activityInstances) {
            String historicActivityId = historicActivityInstance.getActivityId();
            ActivityImpl activity = definition.findActivity(historicActivityId);
            if (activity != null) {
                signHistoryNode(
                        image, //
                        activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
                        historicActivityInstance.getActivityType());
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String formatName = getDiagramExtension(diagramResourceName);
        ImageIO.write(image, formatName, out);
        return new ByteArrayInputStream(out.toByteArray());

    }
    
    private static String getDiagramExtension(String diagramResourceName) {
        return FilenameUtils.getExtension(diagramResourceName);
    }

    /**
     * 标记运行节点
     * @param image 原始图片
     * @param x 左上角节点坐在X位置
     * @param y 左上角节点坐在Y位置
     * @param width 宽
     * @param height 高
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
     * @param image 原始图片
     * @param x 左上角节点坐在X位置
     * @param y 左上角节点坐在Y位置
     * @param width 宽
     * @param height 高
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
     * @param x 左上角节点坐在X位置
     * @param y 左上角节点坐在Y位置
     * @param width 宽
     * @param height 高
     * @param graphics 绘图对象
     * @param color 节点边框颜色
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

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }
    
    /**
     * 根据历史环节(即运行过的环节)查找出对应的已经跑过的连线
     * @param activityInstance
     * @return
     */
    private List<String> getHighLightedFlows(List<ActivityImpl> listActivity) {
    	List<String> flowIds = new ArrayList<String>();
    	for (ActivityImpl act : listActivity) {
    		String sourceid = (String) act.getId(); // 获取源id
    		List<PvmTransition> outTransitions = act.getOutgoingTransitions();// 获取从源节点出来的所有线路
    		for (PvmTransition tr : outTransitions) {
    			PvmActivity ac = tr.getDestination();// 获取线路所有的目标id
    			String endid = ac.getId();    			
    			if (ChooseRighLine(listActivity, sourceid, endid)) {
    				flowIds.add(tr.getId());
    			}
    		}
    	}
    	return flowIds;
    }
    
    private boolean ChooseRighLine(List<ActivityImpl> activity, String sourceid, String endid){
  		boolean flag=false;
  		List<ActivityImpl> prehistoryNodesList = new ArrayList<ActivityImpl>();
  		List<ActivityImpl> sufhistoryNodesList = new ArrayList<ActivityImpl>();
  		prehistoryNodesList = activity.subList(0, activity.size()-1);
  		sufhistoryNodesList = activity.subList(1,activity.size());
  		for(int i=0;i<prehistoryNodesList.size();i++){
  			if(prehistoryNodesList.get(i).getId().equals(sourceid) &&
  					sufhistoryNodesList.get(i).getId().equals(endid))
  				flag=true;
  		}
  		return flag;
  	}
}