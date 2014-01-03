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
package test.withdraw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.snaker.engine.entity.Order;
import org.snaker.engine.entity.Task;
import org.snaker.engine.helper.StreamHelper;
import org.snaker.engine.test.TestSnakerBase;

/**
 * @author yuqs
 * @version 1.0
 */
public class TestWithdraw extends TestSnakerBase {
	@Before
	public void before() {
		processId = engine.process().deploy(StreamHelper
						.getStreamFromClasspath("test/withdraw/process.snaker"));
	}
	
	@Test
	public void test() {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("task1.assignee", new String[]{"1","2"});
		Order order = engine.startInstanceById(processId, "1", args);
		List<Task> tasks = engine.query().getActiveTasksByActors("1");
		Task t = null;
		for(Task task : tasks) {		
			t = task;
			System.out.println("xxx:"+task.getDisplayName());
			engine.executeTask(task.getId(), "1", args);
		}
		
		Task t2 = null;
		List<Task> tasks2 = engine.query().getActiveTasksByActors("2");
		for(Task task2 : tasks2) {		
			t2 = task2;
			System.out.println("sssssss:"+task2.getDisplayName());
			engine.executeTask(task2.getId(), "2", args);
		}
		
		engine.withdrawTask(t2.getId(), "1");
		engine.executeTask(t2.getId(), "1", args);
//		List<Task> tasksvv = engine.query().getActiveTasksByActors("1");
//		for(Task task : tasksvv) {		
//			engine.executeTask(task.getId(), "1", args);
//		}
		
		//engine.executeTask(t2.getId(), "1", args);
		
//		List<Task> tasks3 = engine.query().getActiveTasks(order.getId());
//		for(Task task3 : tasks3){
//			System.out.println("vvvvvvv:"+task3.getDisplayName());
//		}
		
	}
}
