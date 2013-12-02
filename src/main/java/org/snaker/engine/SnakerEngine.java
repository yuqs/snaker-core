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
package org.snaker.engine;

import java.util.List;
import java.util.Map;

import org.snaker.engine.cfg.Configuration;
import org.snaker.engine.core.Execution;
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Task;
import org.snaker.engine.model.WorkModel;

/**
 * 流程引擎接口
 * @author yuqs
 * @version 1.0
 */
public interface SnakerEngine {
	/**
	 * 根据Configuration对象配置实现类
	 * @param context
	 */
	public SnakerEngine configure(Configuration config);
	
	/**
	 * 获取process服务
	 * @return
	 */
	public IProcessService process();
	
	/**
	 * 获取查询服务
	 */
	public IQueryService query();
	
	/**
	 * 根据流程定义ID启动流程实例
	 * @param processId 流程定义ID
	 * @return Order 流程实例
	 */
	public Order startInstanceById(String id);
	
	/**
	 * 根据流程定义ID，操作人ID启动流程实例
	 * @param processId 流程定义ID
	 * @param operator 操作人ID
	 * @return Order 流程实例
	 */
	public Order startInstanceById(String id, Long operator);
	
	/**
	 * 根据流程定义ID，操作人ID，参数列表启动流程实例
	 * @param processId 流程定义ID
	 * @param operator 操作人ID
	 * @param args 参数列表
	 * @return Order 流程实例
	 */
	public Order startInstanceById(String id, Long operator, Map<String, Object> args);
	
	/**
	 * 根据执行对象启动流程实例
	 * @param execution
	 * @return
	 */
	public Order startInstanceByExecution(Execution execution);
	
	/**
	 * 根据任务主键ID执行任务
	 * @param taskId 任务主键ID
	 * @return List<Task> 任务集合
	 */
	public List<Task> executeTask(String taskId);
	
	/**
	 * 根据任务主键ID，操作人ID执行任务
	 * @param taskId 任务主键ID
	 * @param operator 操作人主键ID
	 * @return List<Task> 任务集合
	 */
	public List<Task> executeTask(String taskId, Long operator);
	
	/**
	 * 根据任务主键ID，操作人ID，参数列表执行任务
	 * @param taskId 任务主键ID
	 * @param operator 操作人主键ID
	 * @param args 参数列表
	 * @return List<Task> 任务集合
	 */
	public List<Task> executeTask(String taskId, Long operator, Map<String, Object> args);
	
	/**
	 * 创建新的任务
	 * @param taskModel
	 * @param order
	 * @param args
	 */
	public List<Task> createTask(WorkModel taskModel, Order order, Map<String, Object> args);
	
	/**
	 * 根据任务主键ID，操作人ID提取任务
	 * 提取任务相当于预受理操作，仅仅标识此任务只能由此操作人处理
	 * @param taskId 任务主键ID
	 * @param operator 操作人主键ID
	 */
	public void takeTask(String taskId, Long operator);
	
	/**
	 * 终止指定ID的流程实例
	 * @param orderId
	 */
	public void terminateById(String orderId);
	
	/**
	 * 终止指定ID的流程实例
	 * @param orderId
	 * @param operator
	 */
	public void terminateById(String orderId, Long operator);
	
	/**
	 * 完成指定执行对象的流程实例
	 * @param order
	 * @param operator
	 */
	public void finishByExecution(Execution execution);
}
