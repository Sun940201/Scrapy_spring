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

package org.springframework.cloud.stream.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} for creating channels for fields annotated with
 * {@link org.springframework.cloud.stream.annotation.Input} and
 * {@link org.springframework.cloud.stream.annotation.Output}.
 *
 * @author Marius Bogoevici
 */
public class DirectChannelFactoryBean implements FactoryBean<DirectChannel>, BeanNameAware, BeanFactoryAware, ApplicationContextAware {

 private DirectChannel directChannel;

 private String beanName;

 private BeanFactory beanFactory;

 private ApplicationContext applicationContext;

 @Override
 public void setBeanName(String beanName) {
  this.beanName = beanName;
 }

 @Override
 public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
  Assert.notNull(beanFactory, "'beanFactory' must not be null");
  this.beanFactory = beanFactory;
 }

 @Override
 public void setApplicationContext(ApplicationContext applicationContext) {
  Assert.notNull(beanFactory, "'applicationContext' must not be null");
  this.applicationContext = applicationContext;
 }

 @Override
 public synchronized DirectChannel getObject() throws Exception {
  if (directChannel == null) {
   directChannel = new DirectChannel();
  }
  directChannel.setBeanName(beanName);
  directChannel.setBeanFactory(beanFactory);
  directChannel.setApplicationContext(applicationContext);
  return directChannel;
 }

 @Override
 public Class<?> getObjectType() {
  return DirectChannel.class;
 }

 @Override
 public boolean isSingleton() {
  return true;
 }
}