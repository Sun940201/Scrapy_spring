/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.amqp.rabbit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.answers.DoesNothing;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.test.BrokerRunning;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

public class RabbitAdminTests {

 @Rule
 public ExpectedException exception = ExpectedException.none();

 @Rule
 public BrokerRunning brokerIsRunning = BrokerRunning.isRunning();

 @Test
 public void testSettingOfNullConectionFactory() {
  ConnectionFactory connectionFactory = null;
  try {
   new RabbitAdmin(connectionFactory);
   fail("should have thrown IllegalArgumentException when ConnectionFactory is null.");
  }
  catch (IllegalArgumentException e) {
   assertEquals("ConnectionFactory must not be null", e.getMessage());
  }
 }

 @Test
 public void testNoFailOnStartupWithMissingBroker() throws Exception {
  SingleConnectionFactory connectionFactory = new SingleConnectionFactory("foo");
  connectionFactory.setPort(434343);
  GenericApplicationContext applicationContext = new GenericApplicationContext();
  applicationContext.getBeanFactory().registerSingleton("foo", new Queue("queue"));
  RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
  rabbitAdmin.setApplicationContext(applicationContext);
  rabbitAdmin.setAutoStartup(true);
  rabbitAdmin.afterPropertiesSet();
  connectionFactory.destroy();
 }

 @Test
 public void testFailOnFirstUseWithMissingBroker() throws Exception {
  SingleConnectionFactory connectionFactory = new SingleConnectionFactory("foo");
  connectionFactory.setPort(434343);
  GenericApplicationContext applicationContext = new GenericApplicationContext();
  applicationContext.getBeanFactory().registerSingleton("foo", new Queue("queue"));
  RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
  rabbitAdmin.setApplicationContext(applicationContext);
  rabbitAdmin.setAutoStartup(true);
  rabbitAdmin.afterPropertiesSet();
  exception.expect(IllegalArgumentException.class);
  rabbitAdmin.declareQueue();
  connectionFactory.destroy();
 }

 @Test
 public void testProperties() throws Exception {
  SingleConnectionFactory connectionFactory = new SingleConnectionFactory();
  connectionFactory.setHost("localhost");
  RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
  String queueName = "test.properties." + System.currentTimeMillis();
  try {
   rabbitAdmin.declareQueue(new Queue(queueName));
   new RabbitTemplate(connectionFactory).convertAndSend(queueName, "foo");
   int n = 0;
   while (n++ < 100 && messageCount(rabbitAdmin, queueName) == 0) {
    Thread.sleep(100);
   }
   assertTrue("Message count = 0", n < 100);
   Channel channel = connectionFactory.createConnection().createChannel(false);
   DefaultConsumer consumer = new DefaultConsumer(channel);
   channel.basicConsume(queueName, true, consumer);
   n = 0;
   while (n++ < 100 && messageCount(rabbitAdmin, queueName) > 0) {
    Thread.sleep(100);
   }
   assertTrue("Message count > 0", n < 100);
   Properties props = rabbitAdmin.getQueueProperties(queueName);
   assertNotNull(props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
   assertEquals(1, props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
   channel.close();
  }
  finally {
   rabbitAdmin.deleteQueue(queueName);
   connectionFactory.destroy();
  }
 }

 private int messageCount(RabbitAdmin rabbitAdmin, String queueName) {
  Properties props = rabbitAdmin.getQueueProperties(queueName);
  assertNotNull(props);
  assertNotNull(props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
  return Integer.valueOf((Integer) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
 }

 @Test
 public void testTemporaryLogs() throws Exception {
  SingleConnectionFactory connectionFactory = new SingleConnectionFactory();
  connectionFactory.setHost("localhost");
  RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
  try {
   ApplicationContext ctx = mock(ApplicationContext.class);
   Map<String, Queue> queues = new HashMap<String, Queue>();
   queues.put("nonDurQ", new Queue("testq.nonDur", false, false, false));
   queues.put("adQ", new Queue("testq.ad", true, false, true));
   queues.put("exclQ", new Queue("testq.excl", true, true, false));
   queues.put("allQ", new Queue("testq.all", false, true, true));
   when(ctx.getBeansOfType(Queue.class)).thenReturn(queues);
   Map<String, Exchange> exchanges = new HashMap<String, Exchange>();
   exchanges.put("nonDurEx", new DirectExchange("testex.nonDur", false, false));
   exchanges.put("adEx", new DirectExchange("testex.ad", true, true));
   exchanges.put("allEx", new DirectExchange("testex.all", false, true));
   when(ctx.getBeansOfType(Exchange.class)).thenReturn(exchanges);
   rabbitAdmin.setApplicationContext(ctx);
   rabbitAdmin.afterPropertiesSet();
   Log logger = spy(TestUtils.getPropertyValue(rabbitAdmin, "logger", Log.class));
   when(logger.isInfoEnabled()).thenReturn(true);
   doAnswer(new DoesNothing()).when(logger).info(anyString());
   new DirectFieldAccessor(rabbitAdmin).setPropertyValue("logger", logger);
   connectionFactory.createConnection().close(); // force declarations
   ArgumentCaptor<String> log = ArgumentCaptor.forClass(String.class);
   verify(logger, times(7)).info(log.capture());
   List<String> logs = log.getAllValues();
   Collections.sort(logs);
   assertThat(logs.get(0), Matchers.containsString("(testex.ad) durable:true, auto-delete:true"));
   assertThat(logs.get(1), Matchers.containsString("(testex.all) durable:false, auto-delete:true"));
   assertThat(logs.get(2), Matchers.containsString("(testex.nonDur) durable:false, auto-delete:false"));
   assertThat(logs.get(3), Matchers.containsString("(testq.ad) durable:true, auto-delete:true, exclusive:false"));
   assertThat(logs.get(4), Matchers.containsString("(testq.all) durable:false, auto-delete:true, exclusive:true"));
   assertThat(logs.get(5), Matchers.containsString("(testq.excl) durable:true, auto-delete:false, exclusive:true"));
   assertThat(logs.get(6), Matchers.containsString("(testq.nonDur) durable:false, auto-delete:false, exclusive:false"));
  }
  finally {
   cleanQueuesAndExchanges(rabbitAdmin);
   connectionFactory.destroy();
  }
 }

 private void cleanQueuesAndExchanges(RabbitAdmin rabbitAdmin) {
  rabbitAdmin.deleteQueue("testq.nonDur");
  rabbitAdmin.deleteQueue("testq.ad");
  rabbitAdmin.deleteQueue("testq.excl");
  rabbitAdmin.deleteQueue("testq.all");
  rabbitAdmin.deleteExchange("testex.nonDur");
  rabbitAdmin.deleteExchange("testex.ad");
  rabbitAdmin.deleteExchange("testex.all");
 }

}