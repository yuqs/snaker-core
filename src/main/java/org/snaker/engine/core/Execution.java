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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.snaker.engine.SnakerEngine;
import org.snaker.engine.SnakerException;
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Process;
import org.snaker.engine.entity.Task;
import org.snaker.engine.model.ProcessModel;

/**
 * 流程执行过程中所传递的执行对象，其中包含流程定义、流程模型、流程实例对象、执行参数、返回的任务列表
 * @author yuqs
 * @version 1.0
 */
public class Execution implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3730741790729624400L;
	/**
	 * SnakerEngine holder
	 */
	private SnakerEngine engine;
	/**
	 * 流程定义对象
	 */
	private Process process;
	/**
	 * 流程实例对象
	 */
	private Order order;
	/**
	 * 父流程实例
	 */
	private Order parentOrder;
	/**
	 * 父流程实例节点名称
	 */
	private String parentNodeName;
	/**
	 * 子流程实例节点名称
	 */
	private String childOrderId;
	/**
	 * 执行参数
	 */
	private Map<String, Object> args;
	/**
	 * 操作人
	 */
	private Long operator;
	/**
	 * 任务主键ID
	 */
	private String taskId;
	/**
	 * 返回的任务列表
	 */
	private List<Task> tasks = new ArrayList<Task>();
	/**
	 * 是否已合并
	 * 针对join节点的处理
	 */
	private boolean isMerged = false;
	
	/**
	 * 用于产生子流程执行对象使用
	 * @param execution
	 * @param process
	 * @param parentNodeName
	 */
	Execution(Execution execution, Process process, String parentNodeName) {
		if(execution == null || process == null || parentNodeName == null) {
			throw new SnakerException("构造Execution对象失败，请检查execution、process、parentNodeName是否为空");
		}
		this.engine = execution.getEngine();
		this.process = process;
		this.args = execution.getArgs();
		this.parentOrder = execution.getOrder();
		this.parentNodeName = parentNodeName;
		this.operator = execution.getOperator();
	}
	
	/**
	 * 构造函数，接收流程定义、流程实例对象、执行参数
	 * @param process
	 * @param order
	 * @param args
	 */
	public Execution(SnakerEngine engine, Process process, Order order, Map<String, Object> args) {
		if(process == null || order == null || process.getModel() == null) {
			throw new SnakerException("构造Execution对象失败，请检查process、order、model是否为空");
		}
		this.engine = engine;
		this.process = process;
		this.order = order;
		this.args = args;
	}
	
	/**
	 * 根据当前执行对象execution、子流程定义process、当前节点名称产生子流程的执行对象
	 * @param execution
	 * @param process
	 * @param parentNodeName
	 * @return
	 */
	public Execution createSubExecution(Execution execution, Process process, String parentNodeName) {
		return new Execution(execution, process, parentNodeName);
	}
	
	/**
	 * 获取流程定义对象
	 * @return
	 */
	public Process getProcess() {
		return process;
	}
	
	/**
	 * 获取流程模型对象
	 * @return
	 */
	public ProcessModel getModel() {
		return process.getModel();
	}
	
	/**
	 * 获取流程实例对象
	 * @return
	 */
	public Order getOrder() {
		return order;
	}
	
	/**
	 * 获取执行参数
	 * @return
	 */
	public Map<String, Object> getArgs() {
		return args;
	}
	
	/**
	 * 返回任务结果集
	 * @return
	 */
	public List<Task> getTasks() {
		return tasks;
	}
	
	/**
	 * 添加任务集合
	 * @param tasks
	 */
	public void addTasks(List<Task> tasks) {
		this.tasks.addAll(tasks);
	}

	/**
	 * 返回当前操作人ID
	 * @return
	 */
	public Long getOperator() {
		return operator;
	}

	/**
	 * 设置当前操作人ID
	 * @param operator
	 */
	public void setOperator(Long operator) {
		this.operator = operator;
	}

	/**
	 * 返回任务ID
	 * @return
	 */
	public String getTaskId() {
		return taskId;
	}

	/**
	 * 设置任务ID
	 * @param taskId
	 */
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	/**
	 * 判断是否已经成功合并
	 * @return
	 */
	public boolean isMerged() {
		return isMerged;
	}

	/**
	 * 设置是否为已合并
	 * @param isMerged
	 */
	public void setMerged(boolean isMerged) {
		this.isMerged = isMerged;
	}

	/**
	 * 获取引擎
	 * @return
	 */
	public SnakerEngine getEngine() {
		return engine;
	}

	public Order getParentOrder() {
		return parentOrder;
	}

	public String getParentNodeName() {
		return parentNodeName;
	}

	public String getChildOrderId() {
		return childOrderId;
	}

	public void setChildOrderId(String childOrderId) {
		this.childOrderId = childOrderId;
	}
}
