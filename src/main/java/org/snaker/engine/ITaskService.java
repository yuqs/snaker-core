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

import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Task;
import org.snaker.engine.entity.TaskActor;
import org.snaker.engine.model.CustomModel;
import org.snaker.engine.model.TaskModel;

/**
 * 任务业务类，包括以下服务：
 * 1、创建任务
 * 2、根据任务ID获取任务对象
 * 3、对指定任务分配参与者
 * 4、完成任务
 * @author yuqs
 * @version 1.0
 */
public interface ITaskService {
	/**
	 * 完成指定的任务，主要更新任务状态、完成时间、处理人
	 * @param task
	 * @param operator
	 * @return
	 */
	Task completeTask(Task task);
	/**
	 * 完成指定的任务，主要更新任务状态、完成时间、处理人
	 * @param task
	 * @param operator
	 * @return
	 */
	Task completeTask(Task task, Long operator);
	
	/**
	 * 提取指定的任务，只更新处理人字段标识参与者
	 * @param task
	 * @param operator
	 * @return
	 */
	Task takeTask(Task task, Long operator);
	
	/**
	 * 根据taskId获取所有该任务的参与者集合
	 * @param taskId
	 * @return
	 */
	List<TaskActor> getTaskActorsByTaskId(String taskId);
	
	/**
	 * 根据taskId、operator，判断操作人operator是否允许执行任务
	 * @param taskId
	 * @param operator
	 * @return
	 */
	boolean isAllowed(Task task, Long operator);

	/**
	 * 对指定的任务分配参与者。参与者可以为用户、部门、角色
	 * @param taskId
	 * @param actorId
	 */
	void assignTask(String taskId, Long... actorIds);
	
	/**
	 * 根据任务编号获取任务实例
	 * @param taskId
	 * @return
	 */
	Task getTask(String taskId);
	
	/**
	 * 创建新的任务
	 * @param taskModel
	 * @param order
	 * @param args
	 */
	List<Task> createTask(TaskModel taskModel, Order order, Map<String, Object> args);
	
	/**
	 * 创建新的任务 
	 * @param customModel
	 * @param order
	 * @return
	 */
	Task createTask(CustomModel customModel, Order order);
	
	/**
	 * 保存任务对象
	 * @param task
	 */
	void saveTask(Task task);
	
	/**
	 * 创建新的任务对象
	 * @return
	 */
	Task newTask();
}
