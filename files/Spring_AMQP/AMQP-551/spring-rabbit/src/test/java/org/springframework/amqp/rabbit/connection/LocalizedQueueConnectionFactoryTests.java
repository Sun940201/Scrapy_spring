/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.rabbit.connection;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.beans.DirectFieldAccessor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;


/**
 *
 * @author Gary Russell
 */
public class LocalizedQueueConnectionFactoryTests {

 private final Map<String, Connection> connections = new HashMap<String, Connection>();

 private final Map<String, Channel> channels = new HashMap<String, Channel>();

 private final Map<String, Consumer> consumers = new HashMap<String, Consumer>();

 private final Map<String, String> consumerTags = new HashMap<String, String>();

 private final CountDownLatch latch1 = new CountDownLatch(1);

 private final CountDownLatch latch2 = new CountDownLatch(2);

 @SuppressWarnings("unchecked")
 @Test
 public void testFailOver() throws Exception {
  ConnectionFactory defaultConnectionFactory = mockCF("localhost:1234");
  String rabbit1 = "localhost:1235";
  String rabbit2 = "localhost:1236";
  String[] addresses = new String[] { rabbit1, rabbit2 };
  String[] adminUris = new String[] { "http://localhost:11235", "http://localhost:11236" };
  String[] nodes = new String[] { "rabbit@foo", "rabbit@bar" };
  String vhost = "/";
  String username = "guest";
  String password = "guest";
  final AtomicBoolean firstServer = new AtomicBoolean(true);
  LocalizedQueueConnectionFactory lqcf = new LocalizedQueueConnectionFactory(defaultConnectionFactory, addresses,
    adminUris, nodes, vhost, username, password, false, null) {

   private final String[] nodes = new String[] { "rabbit@foo", "rabbit@bar" };


   @Override
   protected Client createClient(String adminUri, String username, String password)
     throws MalformedURLException, URISyntaxException {
    return doCreateClient(adminUri, username, password, firstServer.get() ? nodes[0] : nodes[1]);
   }


   @Override
   protected ConnectionFactory createConnectionFactory(String address, String node) throws Exception {
    return mockCF(address);
   }

  };
  Log logger = spy(TestUtils.getPropertyValue(lqcf, "logger", Log.class));
  new DirectFieldAccessor(lqcf).setPropertyValue("logger", logger);
  when(logger.isInfoEnabled()).thenReturn(true);
  doAnswer(new CallsRealMethods()).when(logger).debug(anyString());
  ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(lqcf);
  container.setQueueNames("q");
  container.afterPropertiesSet();
  container.start();
  assertTrue(latch1.await(10, TimeUnit.SECONDS));
  Channel channel = this.channels.get(rabbit1);
  assertNotNull(channel);
  verify(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(),
    anyBoolean(), anyMap(),
    Matchers.any(Consumer.class));
  verify(logger, atLeast(1)).info(captor.capture());
  assertTrue(assertLog(captor.getAllValues(), "Queue: q is on node: rabbit@foo at: localhost:1235"));

  // Fail rabbit1 and verify the container switches to rabbit2

  firstServer.set(false);
  when(channel.isOpen()).thenReturn(false);
  when(this.connections.get(rabbit1).isOpen()).thenReturn(false);
  this.consumers.get(rabbit1).handleCancel(consumerTags.get(rabbit1));
  assertTrue(latch2.await(10, TimeUnit.SECONDS));
  channel = this.channels.get(rabbit2);
  assertNotNull(channel);
  verify(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(),
    anyBoolean(), anyMap(),
    Matchers.any(Consumer.class));
  container.stop();
  verify(logger, atLeast(1)).info(captor.capture());
  assertTrue(assertLog(captor.getAllValues(), "Queue: q is on node: rabbit@bar at: localhost:1236"));
 }

 private boolean assertLog(List<String> logRows, String expected) {
  for (String log : logRows) {
   if (log.contains(expected)) {
    return true;
   }
  }
  return false;
 }

 private Client doCreateClient(String uri, String username, String password, String node) {
  Client client = mock(Client.class);
  QueueInfo queueInfo = new QueueInfo();
  queueInfo.setNode(node);
  when(client.getQueue("/", "q")).thenReturn(queueInfo);
  return client;
 }

 @Test
 public void test2Queues() throws Exception {
  try {
   String rabbit1 = "localhost:1235";
   String rabbit2 = "localhost:1236";
   String[] addresses = new String[] { rabbit1, rabbit2 };
   String[] adminUris = new String[] { "http://localhost:11235", "http://localhost:11236" };
   String[] nodes = new String[] { "rabbit@foo", "rabbit@bar" };
   String vhost = "/";
   String username = "guest";
   String password = "guest";
   LocalizedQueueConnectionFactory lqcf = new LocalizedQueueConnectionFactory(mockCF("localhost:1234"),
     addresses, adminUris, nodes, vhost, username, password, false, null);
   lqcf.getTargetConnectionFactory("[foo, bar]");
  }
  catch (IllegalArgumentException e) {
   assertThat(e.getMessage(), containsString("Cannot use LocalizedQueueConnectionFactory with more than one queue: [foo, bar]"));
  }
 }

 @SuppressWarnings("unchecked")
 private ConnectionFactory mockCF(final String address) throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel = mock(Channel.class);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel);
  when(connection.isOpen()).thenReturn(true);
  when(channel.isOpen()).thenReturn(true);
  doAnswer(new Answer<String>() {

   @Override
   public String answer(InvocationOnMock invocation) throws Throwable {
    String tag = UUID.randomUUID().toString();
    consumers.put(address, (Consumer) invocation.getArguments()[6]);
    consumerTags.put(address, tag);
    latch1.countDown();
    latch2.countDown();
    return tag;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(),
    any(Consumer.class));
  when(connectionFactory.getHost()).thenReturn(address);
  this.connections.put(address, connection);
  this.channels.put(address, channel);
  return connectionFactory;
 }

}