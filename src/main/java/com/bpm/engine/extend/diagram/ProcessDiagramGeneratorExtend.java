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
 * ����ͼ���ƹ���
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
     * ��ѯ����������ͼ
     * @param processInstanceId
     * @return
     * @throws IOException
     */
    public InputStream generateDiagram(String processInstanceId) throws IOException {
    	//��ʷ����
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
                if (historicActivityInstance.getEndTime() == null) {// �ڵ�����������
                    signRunningNode(
                            image, //
                            activity.getX(), activity.getY(), activity.getWidth(), activity.getHeight(),
                            historicActivityInstance.getActivityType());
                } else {// �ڵ��Ѿ�����
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
     * ��ѯ��ʷ����ͼ
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
     * ������нڵ�
     * @param image ԭʼͼƬ
     * @param x ���Ͻǽڵ�����Xλ��
     * @param y ���Ͻǽڵ�����Yλ��
     * @param width ��
     * @param height ��
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
     * @param image ԭʼͼƬ
     * @param x ���Ͻǽڵ�����Xλ��
     * @param y ���Ͻǽڵ�����Yλ��
     * @param width ��
     * @param height ��
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
     * @param x ���Ͻǽڵ�����Xλ��
     * @param y ���Ͻǽڵ�����Yλ��
     * @param width ��
     * @param height ��
     * @param graphics ��ͼ����
     * @param color �ڵ�߿���ɫ
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
     * ������ʷ����(�����й��Ļ���)���ҳ���Ӧ���Ѿ��ܹ�������
     * @param activityInstance
     * @return
     */
    private List<String> getHighLightedFlows(List<ActivityImpl> listActivity) {
    	List<String> flowIds = new ArrayList<String>();
    	for (ActivityImpl act : listActivity) {
    		String sourceid = (String) act.getId(); // ��ȡԴid
    		List<PvmTransition> outTransitions = act.getOutgoingTransitions();// ��ȡ��Դ�ڵ������������·
    		for (PvmTransition tr : outTransitions) {
    			PvmActivity ac = tr.getDestination();// ��ȡ��·���е�Ŀ��id
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