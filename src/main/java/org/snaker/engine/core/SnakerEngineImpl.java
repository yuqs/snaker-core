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

import java.util.ArrayList;
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
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Process;
import org.snaker.engine.entity.Task;
import org.snaker.engine.helper.AssertHelper;
import org.snaker.engine.helper.DateHelper;
import org.snaker.engine.model.CustomModel;
import org.snaker.engine.model.NodeModel;
import org.snaker.engine.model.ProcessModel;
import org.snaker.engine.model.StartModel;
import org.snaker.engine.model.TaskModel;
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

	@Override
	public IProcessService process() {
		return this.processService;
	}

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
	public Order startInstanceById(String id, Long operator) {
		return startInstanceById(id, operator, null);
	}

	/**
	 * 根据流程定义ID，操作人ID，参数列表启动流程实例
	 */
	@Override
	public Order startInstanceById(String id, Long operator, Map<String, Object> args) {
		if(args == null) args = new HashMap<String, Object>();
		Process process = ModelContainer.getEntity(id);
		ProcessModel model = process.getModel();
		StartModel start = model.getStart();
		AssertHelper.notNull(start, "流程定义[id=" + id + "]没有开始节点");
		Order order = orderService.createOrder(process, operator, args);
		Execution execution = new Execution(this, process, order, args);
		execution.setOperator(operator);
		start.execute(execution);
		return order;
	}
	
	@Override
	public Order startInstanceByExecution(Execution execution) {
		Process process = execution.getProcess();
		ProcessModel model = process.getModel();
		StartModel start = model.getStart();
		AssertHelper.notNull(start, "流程定义[id=" + process.getId() + "]没有开始节点");
		Order order = orderService.createOrder(process, execution.getOperator(), execution.getArgs(), execution.getParentOrder().getId(), execution.getParentNodeName());
		Execution current = new Execution(this, process, order, execution.getArgs());
		current.setOperator(execution.getOperator());
		start.execute(current);
		return order;
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
	public List<Task> executeTask(String taskId, Long operator) {
		return executeTask(taskId, operator, null);
	}

	/**
	 * 根据任务主键ID，操作人ID，参数列表执行任务
	 */
	@Override
	public List<Task> executeTask(String taskId, Long operator, Map<String, Object> args) {
		if(args == null) args = new HashMap<String, Object>();
		Task task = taskService.getTask(taskId);
		if(!taskService.isAllowed(task, operator)) {
			throw new SnakerException("当前参与者[" + operator + "]不允许执行任务[taskId=" + taskId + "]");
		}
		Task lastTask = taskService.completeTask(task, operator);
		String orderId = lastTask.getOrderId();
		Order order = orderService.getOrder(orderId);
		Process process = ModelContainer.getEntity(order.getProcessId());
		ProcessModel model = process.getModel();
		NodeModel nodeModel = model.getNode(lastTask.getTaskName());
		order.setLastUpdator(operator);
		order.setLastUpdateTime(DateHelper.getTime());
		
		Execution execution = new Execution(this, process, order, args);
		execution.setOperator(operator);
		execution.setTaskId(taskId);
		nodeModel.execute(execution);
		return execution.getTasks();
	}

	/**
	 * 根据任务主键ID，操作人ID提取任务 提取任务相当于预受理操作，仅仅标识此任务只能由此操作人处理
	 */
	@Override
	public void takeTask(String taskId, Long operator) {
		Task task = taskService.getTask(taskId);
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
	public void terminateById(String orderId, Long operator) {
		orderService.terminate(orderId, operator);
	}
	
	@Override
	public void finishByExecution(Execution execution) {
		orderService.finish(execution);
	}

	@Override
	public List<Task> createTask(WorkModel model, Order order, Map<String, Object> args) {
		if(model instanceof TaskModel) {
			return taskService.createTask((TaskModel)model, order, args);
		} else if(model instanceof CustomModel) {
			Task task = taskService.createTask((CustomModel)model, order);
			List<Task> tasks = new ArrayList<Task>();
			tasks.add(task);
			return tasks;
		}
		return Collections.emptyList();
	}
}
