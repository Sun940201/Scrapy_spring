/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.amqp.rabbit.listener.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import org.junit.Before;
import org.junit.Test;

import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.test.MessageTestUtils;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import com.rabbitmq.client.Channel;

/**
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MessagingMessageListenerAdapterTests {

 private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

 private final SampleBean sample = new SampleBean();


 @Before
 public void setup() {
  initializeFactory(factory);
 }

 @Test
 public void buildMessageWithStandardMessage() throws Exception {
  Message<String> result = MessageBuilder.withPayload("Response")
    .setHeader("foo", "bar")
    .setHeader(AmqpHeaders.TYPE, "msg_type")
    .setHeader(AmqpHeaders.REPLY_TO, "reply")
    .build();

  Channel session = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("echo", Message.class);
  org.springframework.amqp.core.Message replyMessage = listener.buildMessage(session, result);

  assertNotNull("reply should never be null", replyMessage);
  assertEquals("Response", new String(replyMessage.getBody()));
  assertEquals("type header not copied", "msg_type", replyMessage.getMessageProperties().getType());
  assertEquals("replyTo header not copied", "reply", replyMessage.getMessageProperties().getReplyTo());
  assertEquals("custom header not copied", "bar", replyMessage.getMessageProperties().getHeaders().get("foo"));
 }

 @Test
 public void exceptionInListener() {
  org.springframework.amqp.core.Message message = MessageTestUtils.createTextMessage("foo");
  Channel channel = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("fail", String.class);

  try {
   listener.onMessage(message, channel);
   fail("Should have thrown an exception");
  }
  catch (ListenerExecutionFailedException ex) {
   assertEquals(IllegalArgumentException.class, ex.getCause().getClass());
   assertEquals("Expected test exception", ex.getCause().getMessage());
  }
  catch (Exception ex) {
   fail("Should not have thrown another exception");
  }
 }

 @Test
 public void exceptionInInvocation() {
  org.springframework.amqp.core.Message message = MessageTestUtils.createTextMessage("foo");
  Channel channel = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("wrongParam", Integer.class);

  try {
   listener.onMessage(message, channel);
   fail("Should have thrown an exception");
  }
  catch (ListenerExecutionFailedException ex) {
   assertEquals(org.springframework.messaging.converter.MessageConversionException.class,
     ex.getCause().getClass());
  }
  catch (Exception ex) {
   fail("Should not have thrown another exception");
  }
 }

 @Test
 public void genericMessageTest1() throws Exception {
  org.springframework.amqp.core.Message message = MessageTestUtils.createTextMessage("\"foo\"");
  Channel channel = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("withGenericMessageAnyType", Message.class);
  listener.setMessageConverter(new Jackson2JsonMessageConverter());
  message.getMessageProperties().setContentType("application/json");
  listener.onMessage(message, channel);
  assertEquals(String.class, this.sample.payload.getClass());
  message = org.springframework.amqp.core.MessageBuilder
    .withBody("{ \"foo\" : \"bar\" }".getBytes())
    .andProperties(message.getMessageProperties())
    .build();
  listener.onMessage(message, channel);
  assertEquals(LinkedHashMap.class, this.sample.payload.getClass());
 }

 @Test
 public void genericMessageTest2() throws Exception {
  org.springframework.amqp.core.Message message = MessageTestUtils.createTextMessage("{ \"foo\" : \"bar\" }");
  Channel channel = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("withGenericMessageFooType", Message.class);
  listener.setMessageConverter(new Jackson2JsonMessageConverter());
  message.getMessageProperties().setContentType("application/json");
  listener.onMessage(message, channel);
  assertEquals(Foo.class, this.sample.payload.getClass());
 }


 @Test
 public void genericMessageTest3() throws Exception {
  org.springframework.amqp.core.Message message = MessageTestUtils.createTextMessage("{ \"foo\" : \"bar\" }");
  Channel channel = mock(Channel.class);
  MessagingMessageListenerAdapter listener = getSimpleInstance("withNonGenericMessage", Message.class);
  listener.setMessageConverter(new Jackson2JsonMessageConverter());
  message.getMessageProperties().setContentType("application/json");
  listener.onMessage(message, channel);
  assertEquals(LinkedHashMap.class, this.sample.payload.getClass());
 }

 protected MessagingMessageListenerAdapter getSimpleInstance(String methodName, Class<?>... parameterTypes) {
  Method m = ReflectionUtils.findMethod(SampleBean.class, methodName, parameterTypes);
  return createInstance(m);
 }

 protected MessagingMessageListenerAdapter createInstance(Method m) {
  MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter(null, m);
  adapter.setHandlerMethod(new HandlerAdapter(factory.createInvocableHandlerMethod(sample, m)));
  return adapter;
 }

 private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
  factory.setBeanFactory(new StaticListableBeanFactory());
  factory.afterPropertiesSet();
 }

 private static class SampleBean {

  private Object payload;

  SampleBean() {
   super();
  }

  @SuppressWarnings("unused")
  public Message<String> echo(Message<String> input) {
   return MessageBuilder.withPayload(input.getPayload())
     .setHeader(AmqpHeaders.TYPE, "reply")
     .build();
  }

  @SuppressWarnings("unused")
  public void fail(String input) {
   throw new IllegalArgumentException("Expected test exception");
  }

  @SuppressWarnings("unused")
  public void wrongParam(Integer i) {
   throw new IllegalArgumentException("Should not have been called");
  }

  @SuppressWarnings("unused")
  public void withGenericMessageAnyType(Message<?> message) {
   this.payload = message.getPayload();
  }

  @SuppressWarnings("unused")
  public void withFoo(Foo foo) {
   this.payload = foo;
  }

  @SuppressWarnings("unused")
  public void withGenericMessageFooType(Message<Foo> message) {
   this.payload = message.getPayload();
  }

  @SuppressWarnings("unused")
  public void withNonGenericMessage(@SuppressWarnings("rawtypes") Message message) {
   this.payload = message.getPayload();
  }

 }

 private static class Foo {

  private String foo;

  @SuppressWarnings("unused")
  public String getFoo() {
   return this.foo;
  }

  @SuppressWarnings("unused")
  public void setFoo(String foo) {
   this.foo = foo;
  }

 }

}