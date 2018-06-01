/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.core.event;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.core.event.AnnotatedEventHandlerInvoker.EventHandlerMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

/**
 * Unit tests for {@link AnnotatedEventHandlerInvoker}.
 * 
 * @author Oliver Gierke
 */
public class AnnotatedEventHandlerInvokerUnitTests {

 /**
  * @see DATAREST-582
  */
 @Test
 @SuppressWarnings("unchecked")
 public void doesNotDiscoverMethodsOnProxyClasses() {

  ProxyFactory factory = new ProxyFactory();
  factory.setTarget(new Sample());
  factory.setProxyTargetClass(true);

  AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
  invoker.postProcessAfterInitialization(factory.getProxy(), "proxy");

  MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod> methods = (MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod>) ReflectionTestUtils
    .getField(invoker, "handlerMethods");

  assertThat(methods.get(BeforeCreateEvent.class), hasSize(1));
 }

 @RepositoryEventHandler
 static class Sample {

  @HandleBeforeCreate
  public void method(Sample sample) {}
 }
}