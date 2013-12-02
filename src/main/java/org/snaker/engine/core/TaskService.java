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
import java.util.List;
import java.util.Map;

import org.snaker.engine.ITaskService;
import org.snaker.engine.SnakerException;
import org.snaker.engine.entity.HistoryTask;
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Task;
import org.snaker.engine.entity.TaskActor;
import org.snaker.engine.helper.DateHelper;
import org.snaker.engine.helper.StringHelper;
import org.snaker.engine.model.CustomModel;
import org.snaker.engine.model.TaskModel;
import org.snaker.engine.model.TaskModel.PerformType;

/**
 * 任务执行业务类
 * @author yuqs
 * @version 1.0
 */
public class TaskService extends AccessService implements ITaskService {
	/**
	 * 参与类型
	 */
	public enum TaskType {
		Task, Custom;
	}
	/**
	 * 由DBAccess实现类更新task对象为完成状态
	 */
	@Override
	public Task completeTask(Task task) {
		return completeTask(task, null);
	}
	/**
	 * 由DBAccess实现类更新task对象为完成状态
	 */
	@Override
	public Task completeTask(Task task, Long operator) {
		HistoryTask history = new HistoryTask(task);
		history.setFinishTime(DateHelper.getTime());
		history.setTaskState(STATE_FINISH);
		history.setOperator(operator);
		if(history.getActorIds() == null) {
			List<TaskActor> actors = getTaskActorsByTaskId(task.getId());
			Long[] actorIds = new Long[actors.size()];
			for(int i = 0; i < actors.size(); i++) {
				actorIds[i] = actors.get(i).getActorId();
			}
			history.setActorIds(actorIds);
		}
		access().saveHistory(history);
		access().deleteTask(task);
		return task;
	}
	/**
	 * 由DBAccess实现类更新task对象为完成状态
	 */
	@Override
	public Task takeTask(Task task, Long operator) {
		task.setOperator(operator);
		task.setFinishTime(DateHelper.getTime());
		access().updateTask(task);
		return task;
	}

	/**
	 * 由DBAccess实现类分派task的参与者。关联taskActor对象
	 */
	@Override
	public void assignTask(String taskId, Long... actorIds) {
		if(actorIds == null) return;
		for(Long actorId : actorIds) {
			TaskActor taskActor = new TaskActor();
			taskActor.setTaskId(taskId);
			taskActor.setActorId(actorId);
			access().saveTaskActor(taskActor);
		}
	}

	/**
	 * 由DBAccess实现类根据taskId获取task对象
	 */
	@Override
	public Task getTask(String taskId) {
		return access().getTask(taskId);
	}

	/**
	 * 由DBAccess实现类创建task，并根据model类型决定是否分配参与者
	 * @param model 模型
	 * @param order 流程实例对象
	 * @param args 执行参数
	 * @return List<Task> 任务列表
	 */
	@Override
	public List<Task> createTask(TaskModel taskModel, Order order, Map<String, Object> args) {
		List<Task> tasks = new ArrayList<Task>();
		Long[] actors = null;
		if(args != null && !args.isEmpty()) {
			/**
			 * 分配任务给相关参与者
			 */
			String key = taskModel.getAssignee();
			actors = getTaskActors(args.get(key), key);
		}
		
		String expireTime = DateHelper.parseTime(args.get(taskModel.getExpireTime()));
		String type = taskModel.getPerformType();
		if(type == null || type.equalsIgnoreCase(TaskModel.TYPE_ANY)) {
			//任务执行方式为参与者中任何一个执行即可驱动流程继续流转，该方法只产生一个task
			Task task = createTask(taskModel, order, PerformType.ANY.ordinal(), expireTime, actors);
			tasks.add(task);
		} else {
			//任务执行方式为参与者中每个都要执行完才可驱动流程继续流转，该方法根据参与者个数产生对应的task数量
			for(Long actor : actors) {
				Task ftask = createTask(taskModel, order, PerformType.ALL.ordinal(), expireTime, actor);
				tasks.add(ftask);
			}
		}
		return tasks;
	}
	
	/**
	 * 由自定义模型创建任务
	 * @param customModel
	 * @param order
	 * @return
	 */
	@Override
	public Task createTask(CustomModel customModel, Order order) {
		Task task = newTask();
		task.setOrderId(order.getId());
		task.setTaskName(customModel.getName());
		task.setDisplayName(customModel.getDisplayName());
		task.setCreateTime(DateHelper.getTime());
		task.setTaskType(TaskType.Custom.ordinal());
		saveTask(task);
		return task;
	}
	
	/**
	 * 由任务模型创建任务
	 * @param taskModel 任务模型
	 * @param order 流程实例对象
	 * @param type 任务类型
	 * @param expireTime 期望完成时间
	 * @param actors 任务参与者集合
	 * @return
	 */
	private Task createTask(TaskModel taskModel, Order order, int performType, String expireTime, Long... actors) {
		Task task = newTask();
		task.setOrderId(order.getId());
		task.setTaskName(taskModel.getName());
		task.setDisplayName(taskModel.getDisplayName());
		task.setCreateTime(DateHelper.getTime());
		task.setActionUrl(taskModel.getUrl());
		task.setExpireTime(expireTime);
		task.setPerformType(performType);
		task.setTaskType(TaskType.Task.ordinal());
		saveTask(task);
		assignTask(task.getId(), actors);
		task.setActorIds(actors);
		return task;
	}
	
	/**
	 * 由DBAccess实现类持久化task对象
	 */
	@Override
	public void saveTask(Task task) {
		access().saveTask(task);
	}

	/**
	 * 新建task对象，并设置初始化状态、类型
	 */
	@Override
	public Task newTask() {
		Task task = new Task();
		task.setId(StringHelper.getPrimaryKey());//uuid
		return task;
	}

	/**
	 * 根据taskmodel指定的assignee属性，从args中取值
	 * 将取到的值处理为Long[]类型。
	 * @param actors
	 * @param key
	 * @return
	 */
	private Long[] getTaskActors(Object actors, String key) {
		if(actors == null) return null;
		Long[] results = null;
		if(actors instanceof String) {
			//如果值为字符串类型，则使用逗号,分隔，并解析为Long类型
			String[] actorStrs = ((String)actors).split(",");
			results = new Long[actorStrs.length];
			try {
				for(int i = 0; i < actorStrs.length; i++) {
					results[i] = Long.parseLong(actorStrs[i]);
				}
			} catch(RuntimeException e) {
				throw new SnakerException("任务参与者ID解析失败，请检查参数是否合法[" + key + "].", e.getCause());
			}
			return results;
		} else if(actors instanceof Long) {
			//如果为Long类型，则返回1个元素的Long[]
			results = new Long[1];
			results[0] = (Long)actors;
			return results;
		} else if(actors instanceof Long[]) {
			//如果为Long[]类型，则直接返回
			return (Long[])actors;
		} else {
			//其它类型，抛出不支持的类型异常
			throw new SnakerException("任务参与者对象[" + actors + "]类型不支持.合法参数示例:Long,new Long[]{},'10000,20000'");
		}
	}

	/**
	 * 根据taskId返回所有的任务参与对象集合
	 */
	@Override
	public List<TaskActor> getTaskActorsByTaskId(String taskId) {
		return access().getTaskActorsByTaskId(taskId);
	}

	/**
	 * 判断当前操作人operator是否允许执行taskId指定的任务
	 */
	@Override
	public boolean isAllowed(Task task, Long operator) {
		if(task.getOperator() != null && task.getOperator().longValue() > 0L && operator != null) {
			return operator.longValue() == task.getOperator().longValue();
		}
		List<TaskActor> actors = getTaskActorsByTaskId(task.getId());
		if(actors == null || actors.isEmpty()) return true;
		if(operator == null) return false;
		boolean isAllowed = false;
		for(TaskActor actor : actors) {
			if(actor.getActorId().longValue() == operator.longValue()) {
				isAllowed = true;
				break;
			}
		}
		return isAllowed;
	}
}
