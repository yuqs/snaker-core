/* Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snaker.engine.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snaker.engine.DBAccess;
import org.snaker.engine.IOrderService;
import org.snaker.engine.IProcessService;
import org.snaker.engine.IQueryService;
import org.snaker.engine.ITaskService;
import org.snaker.engine.SnakerEngine;
import org.snaker.engine.SnakerException;
import org.snaker.engine.access.transaction.TransactionInterceptor;
import org.snaker.engine.cfg.Configuration;
import org.snaker.engine.core.TaskService.TaskType;
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Process;
import org.snaker.engine.entity.Task;
import org.snaker.engine.helper.AssertHelper;
import org.snaker.engine.helper.DateHelper;
import org.snaker.engine.helper.JsonHelper;
import org.snaker.engine.helper.StringHelper;
import org.snaker.engine.model.CustomModel;
import org.snaker.engine.model.NodeModel;
import org.snaker.engine.model.ProcessModel;
import org.snaker.engine.model.StartModel;
import org.snaker.engine.model.TaskModel;
import org.snaker.engine.model.TransitionModel;
import org.snaker.engine.model.WorkModel;

/**
 * 基本的流程引擎实现类
 * @author yuqs
 * @version 1.0
 */
public class SnakerEngineImpl implements SnakerEngine {
	private static final Log log = LogFactory.getLog(SnakerEngineImpl.class);
	/**
	 * Snaker配置对象
	 */
	protected Configuration configuration;
	/**
	 * 服务上下文
	 */
	protected ServiceContext context;
	/**
	 * 流程定义业务类
	 */
	protected IProcessService processService;
	/**
	 * 流程实例业务类
	 */
	protected IOrderService orderService;
	/**
	 * 任务业务类
	 */
	protected ITaskService taskService;
	/**
	 * 查询业务类
	 */
	protected IQueryService queryService;
	
	/**
	 * 根据serviceContext上下文，查找processService、orderService、taskService服务
	 */
	@Override
	public SnakerEngine configure(Configuration config) {
		this.configuration = config;
		context = config.getContext();
		processService = context.find(IProcessService.class);
		queryService = context.find(IQueryService.class);
		orderService = context.find(IOrderService.class);
		taskService = context.find(ITaskService.class);
		/*
		 * 无spring环境，DBAccess的实现类通过服务上下文获取
		 */
		if(this.configuration.getApplicationContext() == null) {
			DBAccess access = context.find(DBAccess.class);
			AssertHelper.notNull(access);
			setDBAccess(access);
			TransactionInterceptor interceptor = context.find(TransactionInterceptor.class);
			//如果初始化配置时提供了访问对象，就对DBAccess进行初始化
			if(this.configuration.getAccessDBObject() != null) {
				interceptor.initialize(this.configuration.getAccessDBObject());
			}
		}
		return this;
	}
	
	/**
	 * 注入dbAccess
	 * @param access
	 */
	protected void setDBAccess(DBAccess access) {
		List<AccessService> services = context.findList(AccessService.class);
		for(AccessService service : services) {
			service.setAccess(access);
		}
		initializeProcess();
	}
	
	/**
	 * 初始化流程定义
	 */
	private void initializeProcess() {
		List<Process> list = processService.getAllProcess();
		if(list == null || list.isEmpty()) {
			log.warn("当前没有任何流程定义,请部署流程");
			return;
		}
		for(Process entity : list) {
			ModelContainer.pushEntity(entity.getId(), entity);
		}
		ModelContainer.cascadeReference();
	}

	/**
	 * 获取流程定义服务
	 */
	@Override
	public IProcessService process() {
		return this.processService;
	}

	/**
	 * 获取查询服务
	 */
	@Override
	public IQueryService query() {
		return queryService;
	}
	
	/**
	 * 根据流程定义ID启动流程实例
	 */
	@Override
	public Order startInstanceById(String id) {
		return startInstanceById(id, null);
	}

	/**
	 * 根据流程定义ID，操作人ID启动流程实例
	 */
	@Override
	public Order startInstanceById(String id, String operator) {
		return startInstanceById(id, operator, null);
	}

	/**
	 * 根据流程定义ID，操作人ID，参数列表启动流程实例
	 */
	@Override
	public Order startInstanceById(String id, String operator, Map<String, Object> args) {
		if(args == null) args = new HashMap<String, Object>();
		Process process = ModelContainer.getEntity(id);
		AssertHelper.notNull(process, "指定的流程定义[id=" + id + "]不存在");
		Execution execution = execute(process, operator, args, null, null);
		
		if(process.getModel() != null) {
			StartModel start = process.getModel().getStart();
			AssertHelper.notNull(start, "指定的流程定义[id=" + id + "]没有开始节点");
			start.execute(execution);
		}
		
		return execution.getOrder();
	}
	
	/**
	 * 根据父执行对象启动子流程实例（用于启动子流程）
	 */
	@Override
	public Order startInstanceByExecution(Execution execution) {
		Process process = execution.getProcess();
		StartModel start = process.getModel().getStart();
		AssertHelper.notNull(start, "流程定义[id=" + process.getId() + "]没有开始节点");
		
		Execution current = execute(process, execution.getOperator(), execution.getArgs(), 
				execution.getParentOrder().getId(), execution.getParentNodeName());
		start.execute(current);
		return current.getOrder();
	}
	
	/**
	 * 创建流程实例，并返回执行对象
	 * @param process 流程定义
	 * @param operator 操作人
	 * @param args 参数列表
	 * @param parentId 父流程实例id
	 * @param parentNodeName 启动子流程的父流程节点名称
	 * @return Execution
	 */
	private Execution execute(Process process, String operator, Map<String, Object> args, String parentId, String parentNodeName) {
		Order order = orderService.createOrder(process, operator, args, parentId, parentNodeName);
		Execution current = new Execution(this, process, order, args);
		current.setOperator(operator);
		return current;
	}

	/**
	 * 根据任务主键ID执行任务
	 */
	@Override
	public List<Task> executeTask(String taskId) {
		return executeTask(taskId, null);
	}

	/**
	 * 根据任务主键ID，操作人ID执行任务
	 */
	@Override
	public List<Task> executeTask(String taskId, String operator) {
		return executeTask(taskId, operator, null);
	}

	/**
	 * 根据任务主键ID，操作人ID，参数列表执行任务
	 */
	@Override
	public List<Task> executeTask(String taskId, String operator, Map<String, Object> args) {
		/*
		 * 完成任务，并且构造执行对象
		 */
		Execution execution = execute(taskId, operator, args);
		ProcessModel model = execution.getProcess().getModel();
		if(model != null) {
			NodeModel nodeModel = model.getNode(execution.getTask().getTaskName());
			/*
			 * 将执行对象交给该任务对应的节点模型执行
			 */
			nodeModel.execute(execution);
		}
		return execution.getTasks();
	}
	
	/**
	 * 根据任务主键ID，操作人ID，参数列表执行任务，并且根据nodeName跳转到任意节点
	 * 1、nodeName为null时，则驳回至上一步处理
	 * 2、nodeName不为null时，则任意跳转，即动态创建转移
	 */
	@Override
	public List<Task> executeAndJumpTask(String taskId, String operator, Map<String, Object> args, String nodeName) {
		Execution execution = execute(taskId, operator, args);
		ProcessModel model = execution.getProcess().getModel();
		AssertHelper.notNull(model, "当前任务未找到流程定义模型");
		if(StringHelper.isEmpty(nodeName)) {
			Task newTask = taskService.rejectTask(model, execution.getTask());
			execution.addTask(newTask);
		} else {
			NodeModel nodeModel = model.getNode(nodeName);
			AssertHelper.notNull(nodeModel, "根据节点名称[" + nodeName + "]无法找到节点模型");
			//动态创建转移对象，由转移对象执行execution实例
			TransitionModel tm = new TransitionModel();
			tm.setTarget(nodeModel);
			tm.setEnabled(true);
			tm.execute(execution);
		}

		return execution.getTasks();
	}
	
	/**
	 * 根据流程实例ID，操作人ID，参数列表按照节点模型model创建新的自由任务
	 */
	@Override
	public List<Task> createFreeTask(String orderId, String operator, Map<String, Object> args, WorkModel model) {
		Order order = orderService.getOrder(orderId);
		order.setLastUpdator(operator);
		order.setLastUpdateTime(DateHelper.getTime());
		Process process = ModelContainer.getEntity(order.getProcessId());
		Execution execution = new Execution(this, process, order, args);
		execution.setOperator(operator);
		return createTask(model, execution);
	}
	
	/**
	 * 根据任务主键ID，操作人ID，参数列表完成任务，并且构造执行对象
	 * @param taskId 任务id
	 * @param operator 操作人
	 * @param args 参数列表
	 * @return Execution
	 */
	private Execution execute(String taskId, String operator, Map<String, Object> args) {
		if(args == null) args = new HashMap<String, Object>();
		Task task = finishTask(taskId, operator, args);
		Order order = orderService.getOrder(task.getOrderId());
		order.setLastUpdator(operator);
		order.setLastUpdateTime(DateHelper.getTime());
		Process process = ModelContainer.getEntity(order.getProcessId());
		Execution execution = new Execution(this, process, order, args);
		execution.setOperator(operator);
		execution.setTask(task);
		return execution;
	}
	
	/**
	 * 根据任务主键ID，操作人ID完成任务
	 */
	@Override
	public Task finishTask(String taskId, String operator, Map<String, Object> args) {
		Task task = taskService.getTask(taskId);
		task.setVariable(JsonHelper.toJson(args));
		AssertHelper.notNull(task, "指定的任务[id=" + taskId + "]不存在");
		if(!taskService.isAllowed(task, operator)) {
			throw new SnakerException("当前参与者[" + operator + "]不允许执行任务[taskId=" + taskId + "]");
		}
		taskService.completeTask(task, operator);
		return task;
	}
	
	@Override
	public Task withdrawTask(String taskId, String operator) {
		return taskService.withdrawTask(taskId, operator);
	}

	@Override
	public void takeTask(String taskId, String operator) {
		Task task = taskService.getTask(taskId);
		AssertHelper.notNull(task, "指定的任务[id=" + taskId + "]不存在");
		if(!taskService.isAllowed(task, operator)) {
			throw new SnakerException("当前参与者[" + operator + "]不允许提取任务[taskId=" + taskId + "]");
		}
		taskService.takeTask(task, operator);
	}
	
	@Override
	public void terminateById(String orderId) {
		terminateById(orderId, null);
	}
	
	@Override
	public void terminateById(String orderId, String operator) {
		List<Task> tasks = queryService.getActiveTasks(orderId);
		for(Task task : tasks) {
			taskService.completeTask(task, operator);
		}
		orderService.terminate(orderId, operator);
	}
	
	@Override
	public void finishInstanceById(String orderId) {
		Order order = orderService.getOrder(orderId);
		orderService.finish(order);
	}

	@Override
	public List<Task> createTask(WorkModel model, Execution execution) {
		if(model instanceof TaskModel) {
			return taskService.createTask((TaskModel)model, execution);
		} else if(model instanceof CustomModel) {
			return taskService.createTask((CustomModel)model, execution);
		}
		return Collections.emptyList();
	}
	
	@Override
	public void addTaskActor(String taskId, String... actors) {
		Task task = taskService.getTask(taskId);
		AssertHelper.notNull(task, "指定的任务[id=" + taskId + "]不存在");
		if(task.getTaskType().intValue() == TaskType.Task.ordinal()) {
			taskService.addTaskActor(task, actors);
		}
	}
}
