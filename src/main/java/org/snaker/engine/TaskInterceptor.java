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

import org.snaker.engine.entity.Task;

/**
 * 任务拦截器，对产生的任务结果进行拦截
 * 实现类根据实际情况可对产生的任务进行短信提醒、邮件提醒、日志记录等
 * @author yuqs
 * @version 1.0
 */
public interface TaskInterceptor {
	/**
	 * 拦截方法，参数为产生的task对象
	 * @param task
	 */
	public void intercept(List<Task> tasks);
}
