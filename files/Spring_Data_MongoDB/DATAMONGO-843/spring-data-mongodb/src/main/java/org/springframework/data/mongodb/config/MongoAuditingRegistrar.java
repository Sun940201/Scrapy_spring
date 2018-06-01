/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.mongodb.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.auditing.config.AnnotationAuditingConfiguration;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory;
import org.springframework.data.mongodb.core.mapping.event.AuditingEventListener;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable {@link EnableMongoAuditing} annotation.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
class MongoAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAnnotation()
  */
 @Override
 protected Class<? extends Annotation> getAnnotation() {
  return EnableMongoAuditing.class;
 }

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
  */
 @Override
 public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

  Assert.notNull(annotationMetadata, "AnnotationMetadata must not be null!");
  Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

  registerIsNewStrategyFactoryIfNecessary(registry);
  super.registerBeanDefinitions(annotationMetadata, registry);
 }

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAuditHandlerBeanDefinitionBuilder(org.springframework.data.auditing.config.AnnotationAuditingConfiguration)
  */
 @Override
 protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AnnotationAuditingConfiguration configuration) {

  Assert.notNull(configuration, "AnnotationAuditingConfiguration must not be null!");

  return configureDefaultAuditHandlerAttributes(configuration,
    BeanDefinitionBuilder.rootBeanDefinition(IsNewAwareAuditingHandler.class)).addConstructorArgReference(
    BeanNames.IS_NEW_STRATEGY_FACTORY);
 }

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerAuditListener(org.springframework.beans.factory.config.BeanDefinition, org.springframework.beans.factory.support.BeanDefinitionRegistry)
  */
 @Override
 protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
   BeanDefinitionRegistry registry) {

  Assert.notNull(auditingHandlerDefinition, "BeanDefinition must not be null!");
  Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

  registerInfrastructureBeanWithId(BeanDefinitionBuilder.rootBeanDefinition(AuditingEventListener.class)
    .addConstructorArgValue(auditingHandlerDefinition).getRawBeanDefinition(),
    AuditingEventListener.class.getName(), registry);
 }

 /**
  * @param registry, the {@link BeanDefinitionRegistry} to use to register an {@link IsNewStrategyFactory} to.
  */
 private void registerIsNewStrategyFactoryIfNecessary(BeanDefinitionRegistry registry) {

  if (!registry.containsBeanDefinition(BeanNames.IS_NEW_STRATEGY_FACTORY)) {
   registry.registerBeanDefinition(BeanNames.IS_NEW_STRATEGY_FACTORY,
     BeanDefinitionBuilder.rootBeanDefinition(MappingContextIsNewStrategyFactory.class)
       .addConstructorArgReference(BeanNames.MAPPING_CONTEXT).getBeanDefinition());
  }
 }
}