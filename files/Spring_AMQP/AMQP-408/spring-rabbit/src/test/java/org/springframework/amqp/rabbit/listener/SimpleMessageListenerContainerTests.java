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
package org.springframework.amqp.rabbit.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ChannelProxy;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;


/**
 * @author David Syer
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
public class SimpleMessageListenerContainerTests {

 @Rule
 public ExpectedException expectedException = ExpectedException.none();

 @Test
 public void testInconsistentTransactionConfiguration() throws Exception {
  final SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory("localhost");
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(singleConnectionFactory);
  container.setMessageListener(new MessageListenerAdapter(this));
  container.setQueueNames("foo");
  container.setChannelTransacted(false);
  container.setAcknowledgeMode(AcknowledgeMode.NONE);
  container.setTransactionManager(new TestTransactionManager());
  expectedException.expect(IllegalStateException.class);
  container.afterPropertiesSet();
  singleConnectionFactory.destroy();
 }

 @Test
 public void testInconsistentAcknowledgeConfiguration() throws Exception {
  final SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory("localhost");
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(singleConnectionFactory);
  container.setMessageListener(new MessageListenerAdapter(this));
  container.setQueueNames("foo");
  container.setChannelTransacted(true);
  container.setAcknowledgeMode(AcknowledgeMode.NONE);
  expectedException.expect(IllegalStateException.class);
  container.afterPropertiesSet();
  singleConnectionFactory.destroy();
 }

 @Test
 public void testDefaultConsumerCount() throws Exception {
  final SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory("localhost");
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(singleConnectionFactory);
  container.setMessageListener(new MessageListenerAdapter(this));
  container.setQueueNames("foo");
  container.setAutoStartup(false);
  container.afterPropertiesSet();
  assertEquals(1, ReflectionTestUtils.getField(container, "concurrentConsumers"));
  singleConnectionFactory.destroy();
 }

 @Test
 public void testLazyConsumerCount() throws Exception {
  final SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory("localhost");
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(singleConnectionFactory) {
   @Override
   protected void doStart() throws Exception {
    // do nothing
   }
  };
  container.start();
  assertEquals(1, ReflectionTestUtils.getField(container, "concurrentConsumers"));
  container.stop();
  singleConnectionFactory.destroy();
 }

 /*
  * txSize = 2; 4 messages; should get 2 acks (#2 and #4)
  */
 @SuppressWarnings("unchecked")
 @Test
 public void testTxSizeAcks() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel = mock(Channel.class);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel);
  final AtomicReference<Consumer> consumer = new AtomicReference<Consumer>();
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    consumer.set((Consumer) invocation.getArguments()[6]);
    consumer.get().handleConsumeOk("1");
    return null;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
  final CountDownLatch latch = new CountDownLatch(2);
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    latch.countDown();
    return null;
   }
  }).when(channel).basicAck(anyLong(), anyBoolean());

  final List<Message> messages = new ArrayList<Message>();
  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  container.setTxSize(2);
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
    messages.add(message);
   }
  });
  container.start();
  BasicProperties props = new BasicProperties();
  byte[] payload = "baz".getBytes();
  Envelope envelope = new Envelope(1L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  envelope = new Envelope(2L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  envelope = new Envelope(3L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  envelope = new Envelope(4L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  assertTrue(latch.await(5, TimeUnit.SECONDS));
  assertEquals(4, messages.size());
  Executors.newSingleThreadExecutor().execute(new Runnable() {

   @Override
   public void run() {
    container.stop();
   }
  });
  consumer.get().handleCancelOk("1");
  verify(channel, times(2)).basicAck(anyLong(), anyBoolean());
  verify(channel).basicAck(2, true);
  verify(channel).basicAck(4, true);
  container.stop();
 }

 /*
  * txSize = 2; 3 messages; should get 2 acks (#2 and #3)
  * after timeout.
  */
 @SuppressWarnings("unchecked")
 @Test
 public void testTxSizeAcksWIthShortSet() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel = mock(Channel.class);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel);
  final AtomicReference<Consumer> consumer = new AtomicReference<Consumer>();
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    consumer.set((Consumer) invocation.getArguments()[6]);
    consumer.get().handleConsumeOk("1");
    return null;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
  final CountDownLatch latch = new CountDownLatch(2);
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    latch.countDown();
    return null;
   }
  }).when(channel).basicAck(anyLong(), anyBoolean());

  final List<Message> messages = new ArrayList<Message>();
  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  container.setTxSize(2);
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
    messages.add(message);
   }
  });
  container.start();
  BasicProperties props = new BasicProperties();
  byte[] payload = "baz".getBytes();
  Envelope envelope = new Envelope(1L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  envelope = new Envelope(2L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  envelope = new Envelope(3L, false, "foo", "bar");
  consumer.get().handleDelivery("1", envelope, props, payload);
  assertTrue(latch.await(5, TimeUnit.SECONDS));
  assertEquals(3, messages.size());
  Executors.newSingleThreadExecutor().execute(new Runnable() {

   @Override
   public void run() {
    container.stop();
   }
  });
  consumer.get().handleCancelOk("1");
  verify(channel, times(2)).basicAck(anyLong(), anyBoolean());
  verify(channel).basicAck(2, true);
  // second set was short
  verify(channel).basicAck(3, true);
  container.stop();
 }

 @SuppressWarnings("unchecked")
 @Test
 public void testConsumerArgs() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel = mock(Channel.class);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel);
  final AtomicReference<Consumer> consumer = new AtomicReference<Consumer>();
  final AtomicReference<Map<?, ?>> args = new AtomicReference<Map<?,?>>();
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    consumer.set((Consumer) invocation.getArguments()[6]);
    consumer.get().handleConsumeOk("foo");
    args.set((Map<?, ?>) invocation.getArguments()[5]);
    return null;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), any(Map.class), any(Consumer.class));

  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
   }
  });
  container.setConsumerArguments(Collections. <String, Object> singletonMap("x-priority", Integer.valueOf(10)));
  container.afterPropertiesSet();
  container.start();
  verify(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), any(Map.class), any(Consumer.class));
  assertTrue(args.get() != null);
  assertEquals(10, args.get().get("x-priority"));
  consumer.get().handleCancelOk("foo");
  container.stop();
 }

 @Test
 public void testChangeQueues() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel1 = mock(Channel.class);
  Channel channel2 = mock(Channel.class);
  when(channel1.isOpen()).thenReturn(true);
  when(channel2.isOpen()).thenReturn(true);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel1, channel2);
  List<Consumer> consumers = new ArrayList<Consumer>();
  AtomicInteger consumerTag = new AtomicInteger();
  CountDownLatch latch1 = new CountDownLatch(1);
  CountDownLatch latch2 = new CountDownLatch(2);
  setupMockConsume(channel1, consumers, consumerTag, latch1);
  setUpMockCancel(channel1, consumers);
  setupMockConsume(channel2, consumers, consumerTag, latch2);
  setUpMockCancel(channel2, consumers);

  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
   }
  });
  container.afterPropertiesSet();
  container.start();
  assertTrue(latch1.await(10, TimeUnit.SECONDS));
  container.addQueueNames("bar");
  assertTrue(latch2.await(10, TimeUnit.SECONDS));
  container.stop();
  verify(channel1).basicCancel("0");
  verify(channel2).basicCancel("1");
  verify(channel2).basicCancel("2");
 }

 @Test
 public void testChangeQueuesSimple() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  List<?> queues = TestUtils.getPropertyValue(container, "queueNames", List.class);
  assertEquals(1, queues.size());
  container.addQueues(new AnonymousQueue(), new AnonymousQueue());
  assertEquals(3, queues.size());
  container.removeQueues(new Queue("foo"));
  assertEquals(2, queues.size());
 }

 @Test
 public void testAddQueuesAndStartInCycle() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel1 = mock(Channel.class);
  when(channel1.isOpen()).thenReturn(true);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel1);

  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
   }
  });
  container.afterPropertiesSet();

  for (int i = 0; i < 10; i++) {
   System.out.println(i);
   container.addQueueNames("foo" + i);
   if (!container.isRunning()) {
    container.start();
   }
  }
 }

 @SuppressWarnings("unchecked")
 protected void setupMockConsume(Channel channel, final List<Consumer> consumers, final AtomicInteger consumerTag,
   final CountDownLatch latch) throws IOException {
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    Consumer cons = (Consumer) invocation.getArguments()[6];
    consumers.add(cons);
    cons.handleConsumeOk(String.valueOf(consumerTag.getAndIncrement()));
    latch.countDown();
    return null;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
 }

 protected void setUpMockCancel(Channel channel, final List<Consumer> consumers) throws IOException {
  final Executor exec = Executors.newCachedThreadPool();
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    final String consTag = (String) invocation.getArguments()[0];
    exec.execute(new Runnable() {

     @Override
     public void run() {
      consumers.get(Integer.parseInt(consTag)).handleCancelOk(consTag);
     }
    });
    return null;
   }
  }).when(channel).basicCancel(anyString());
 }

 @SuppressWarnings("unchecked")
 @Test
 public void testWithConnectionPerListenerThread() throws Exception {
  com.rabbitmq.client.ConnectionFactory mockConnectionFactory = mock(com.rabbitmq.client.ConnectionFactory.class);
  com.rabbitmq.client.Connection mockConnection1 = mock(com.rabbitmq.client.Connection.class);
  com.rabbitmq.client.Connection mockConnection2 = mock(com.rabbitmq.client.Connection.class);
  Channel mockChannel1 = mock(Channel.class);
  Channel mockChannel2 = mock(Channel.class);

  when(mockConnectionFactory.newConnection((ExecutorService) null))
   .thenReturn(mockConnection1)
   .thenReturn(mockConnection2)
   .thenReturn(null);
  when(mockConnection1.createChannel()).thenReturn(mockChannel1).thenReturn(null);
  when(mockConnection2.createChannel()).thenReturn(mockChannel2).thenReturn(null);
  when(mockChannel1.isOpen()).thenReturn(true);
  when(mockConnection1.isOpen()).thenReturn(true);
  when(mockChannel2.isOpen()).thenReturn(true);
  when(mockConnection2.isOpen()).thenReturn(true);

  CachingConnectionFactory ccf = new CachingConnectionFactory(mockConnectionFactory);
  ccf.setCacheMode(CacheMode.CONNECTION);

  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(ccf);
  container.setConcurrentConsumers(2);
  container.setQueueNames("foo");
  container.afterPropertiesSet();

  CountDownLatch latch1 = new CountDownLatch(2);
  CountDownLatch latch2 = new CountDownLatch(2);
  doAnswer(messageToConsumer(mockChannel1, container, false, latch1))
  .when(mockChannel1).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
  doAnswer(messageToConsumer(mockChannel2, container, false, latch1))
   .when(mockChannel2).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
  doAnswer(messageToConsumer(mockChannel1, container, true, latch2)).when(mockChannel1).basicCancel(anyString());
  doAnswer(messageToConsumer(mockChannel2, container, true, latch2)).when(mockChannel2).basicCancel(anyString());

  container.start();
  assertTrue(latch1.await(10, TimeUnit.SECONDS));
  Set<?> consumers = TestUtils.getPropertyValue(container, "consumers", Map.class).keySet();
  container.stop();
  assertTrue(latch2.await(10, TimeUnit.SECONDS));

  waitForConsumersToStop(consumers);
  Set<?> openConnections = TestUtils.getPropertyValue(ccf, "openConnections", Set.class);
  assertEquals(1, openConnections.size());
 }

 @SuppressWarnings("unchecked")
 @Test
 public void testConsumerCancel() throws Exception {
  ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  Connection connection = mock(Connection.class);
  Channel channel = mock(Channel.class);
  when(connectionFactory.createConnection()).thenReturn(connection);
  when(connection.createChannel(false)).thenReturn(channel);
  final AtomicReference<Consumer> consumer = new AtomicReference<Consumer>();
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    consumer.set((Consumer) invocation.getArguments()[6]);
    consumer.get().handleConsumeOk("foo");
    return null;
   }
  }).when(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));

  final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
  container.setQueueNames("foo");
  container.setMessageListener(new MessageListener() {

   @Override
   public void onMessage(Message message) {
   }
  });
  container.afterPropertiesSet();
  container.start();
  verify(channel).basicConsume(anyString(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class));
  Log logger = spy(TestUtils.getPropertyValue(container, "logger", Log.class));
  final CountDownLatch latch = new CountDownLatch(1);
  doAnswer(new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    if (((String) invocation.getArguments()[0]).startsWith("Consumer raised exception")) {
     latch.countDown();
    }
    return invocation.callRealMethod();
   }
  }).when(logger).warn(any());
  new DirectFieldAccessor(container).setPropertyValue("logger", logger);
  consumer.get().handleCancel("foo");
  assertTrue(latch.await(10, TimeUnit.SECONDS));
  container.stop();
 }

 private Answer<Object> messageToConsumer(final Channel mockChannel, final SimpleMessageListenerContainer container,
   final boolean cancel, final CountDownLatch latch) {
  return new Answer<Object>() {

   @Override
   public Object answer(InvocationOnMock invocation) throws Throwable {
    Set<?> consumers = TestUtils.getPropertyValue(container, "consumers", Map.class).keySet();
    for (Object consumer : consumers) {
     ChannelProxy channel = TestUtils.getPropertyValue(consumer, "channel", ChannelProxy.class);
     if (channel != null && channel.getTargetChannel() == mockChannel) {
      Consumer rabbitConsumer = TestUtils.getPropertyValue(consumer, "consumer", Consumer.class);
      if (cancel) {
       rabbitConsumer.handleCancelOk((String) invocation.getArguments()[0]);
      }
      else {
       rabbitConsumer.handleConsumeOk("foo");
      }
      latch.countDown();
     }
    }
    return null;
   }
  };

 }

 private void waitForConsumersToStop(Set<?> consumers) throws Exception {
  int n = 0;
  boolean stillUp = true;
  while (stillUp && n++ < 1000) {
   stillUp = false;
   for (Object consumer : consumers) {
    stillUp |= TestUtils.getPropertyValue(consumer, "consumer") != null;
   }
   Thread.sleep(10);
  }
  assertFalse(stillUp);
 }

 @SuppressWarnings("serial")
 private class TestTransactionManager extends AbstractPlatformTransactionManager {

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
  }

  @Override
  protected Object doGetTransaction() throws TransactionException {
   return new Object();
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
  }

 }
}