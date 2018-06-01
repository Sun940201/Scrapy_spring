/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.rabbit.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.adapter.ReplyingMessageListener;
import org.springframework.amqp.rabbit.test.BrokerRunning;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.MultiValueMap;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class DirectMessageListenerContainerTests {

 private static final String Q1 = "testQ1";

 private static final String Q2 = "testQ2";

 @Rule
 public BrokerRunning brokerRunning = BrokerRunning.isRunningWithEmptyQueues(Q1, Q2);

 @After
 public void tearDown() {
  this.brokerRunning.removeTestQueues();
 }

 @Test
 public void testSimple() {
  CachingConnectionFactory cf = new CachingConnectionFactory("localhost");
  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  executor.setThreadNamePrefix("client-");
  executor.afterPropertiesSet();
  cf.setExecutor(executor);
  DirectMessageListenerContainer container = new DirectMessageListenerContainer(cf);
  container.setQueueNames(Q1, Q2);
  container.setConsumersPerQueue(2);
  container.setMessageListener(new MessageListenerAdapter((ReplyingMessageListener<String, String>) in -> {
   if ("foo".equals(in) || "bar".equals(in)) {
    return in.toUpperCase();
   }
   else {
    return null;
   }
  }));
  container.afterPropertiesSet();
  container.start();
  RabbitTemplate template = new RabbitTemplate(cf);
  assertEquals("FOO", template.convertSendAndReceive(Q1, "foo"));
  assertEquals("BAR", template.convertSendAndReceive(Q2, "bar"));
  container.stop();
 }

 @Test
 public void testAdvice() throws Exception {
  CachingConnectionFactory cf = new CachingConnectionFactory("localhost");
  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  executor.setThreadNamePrefix("client-");
  executor.afterPropertiesSet();
  cf.setExecutor(executor);
  DirectMessageListenerContainer container = new DirectMessageListenerContainer(cf);
  container.setQueueNames(Q1, Q2);
  container.setConsumersPerQueue(2);
  final CountDownLatch latch = new CountDownLatch(2);
  container.setMessageListener(m -> latch.countDown());
  final CountDownLatch adviceLatch = new CountDownLatch(2);
  MethodInterceptor advice = i -> {
   adviceLatch.countDown();
   return i.proceed();
  };
  container.setAdviceChain(advice);
  container.afterPropertiesSet();
  container.start();
  RabbitTemplate template = new RabbitTemplate(cf);
  template.convertAndSend(Q1, "foo");
  template.convertAndSend(Q1, "bar");
  assertTrue(latch.await(10, TimeUnit.SECONDS));
  assertTrue(adviceLatch.await(10, TimeUnit.SECONDS));
  container.stop();
 }

 @Test
 public void testQueueManagement() throws Exception {
  CachingConnectionFactory cf = new CachingConnectionFactory("localhost");
  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  executor.setThreadNamePrefix("client-");
  executor.afterPropertiesSet();
  cf.setExecutor(executor);
  DirectMessageListenerContainer container = new DirectMessageListenerContainer(cf);
  container.setConsumersPerQueue(2);
  container.setMessageListener(new MessageListenerAdapter((ReplyingMessageListener<String, String>) in -> {
   if ("foo".equals(in) || "bar".equals(in)) {
    return in.toUpperCase();
   }
   else {
    return null;
   }
  }));
  container.afterPropertiesSet();
  container.start();
  container.addQueueNames(Q1, Q2);
  assertTrue(consumersOnQueue(Q1, 2));
  assertTrue(consumersOnQueue(Q2, 2));
  RabbitTemplate template = new RabbitTemplate(cf);
  assertEquals("FOO", template.convertSendAndReceive(Q1, "foo"));
  assertEquals("BAR", template.convertSendAndReceive(Q2, "bar"));
  container.removeQueueNames(Q1, Q2, "junk");
  assertTrue(consumersOnQueue(Q1, 0));
  assertTrue(consumersOnQueue(Q2, 0));
  assertEquals(0, TestUtils.getPropertyValue(container, "consumers", List.class).size());
  container.stop();
 }

 @Test
 public void testAddRemoveConsumers() throws Exception {
  CachingConnectionFactory cf = new CachingConnectionFactory("localhost");
  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  executor.setThreadNamePrefix("client-");
  executor.afterPropertiesSet();
  cf.setExecutor(executor);
  DirectMessageListenerContainer container = new DirectMessageListenerContainer(cf);
  container.setQueueNames(Q1, Q2);
  container.setConsumersPerQueue(2);
  container.setMessageListener(new MessageListenerAdapter((ReplyingMessageListener<String, String>) in -> {
   if ("foo".equals(in) || "bar".equals(in)) {
    return in.toUpperCase();
   }
   else {
    return null;
   }
  }));
  container.afterPropertiesSet();
  container.start();
  RabbitTemplate template = new RabbitTemplate(cf);
  assertEquals("FOO", template.convertSendAndReceive(Q1, "foo"));
  assertEquals("BAR", template.convertSendAndReceive(Q2, "bar"));
  assertTrue(consumersOnQueue(Q1, 2));
  assertTrue(consumersOnQueue(Q2, 2));
  container.setConsumersPerQueue(1);
  assertTrue(consumersOnQueue(Q1, 1));
  assertTrue(consumersOnQueue(Q2, 1));
  container.setConsumersPerQueue(2);
  assertTrue(consumersOnQueue(Q1, 2));
  assertTrue(consumersOnQueue(Q2, 2));
  container.stop();
  assertTrue(consumersOnQueue(Q1, 0));
  assertTrue(consumersOnQueue(Q2, 0));
  assertEquals(0, TestUtils.getPropertyValue(container, "consumers", List.class).size());
  assertEquals(0, TestUtils.getPropertyValue(container, "consumersByQueue", MultiValueMap.class).size());
 }

 private boolean consumersOnQueue(String queue, int count) throws Exception {
  int n = 0;
  RabbitAdmin admin = this.brokerRunning.getAdmin();
  while (n++ < 100 && !admin.getQueueProperties(queue).get(RabbitAdmin.QUEUE_CONSUMER_COUNT).equals(count)) {
   Thread.sleep(100);
  }
  return admin.getQueueProperties(queue).get(RabbitAdmin.QUEUE_CONSUMER_COUNT).equals(count);
 }

}