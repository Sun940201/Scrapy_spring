/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/**
 * Unit tests for {@link BasePathAwareHandlerMapping}.
 * 
 * @author Oliver Gierke
 */
public class AugmentingHandlerMappingUnitTests {

 @Configuration
 static class Config {

  @Bean
  SampleController sampleController() {
   return new SampleController();
  }
 }

 @Test
 @SuppressWarnings("deprecation")
 public void augmentsRequestMappingsWithBaseUriFromConfiguration() {

  RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();
  configuration.setBaseUri("api");

  BasePathAwareHandlerMapping mapping = new BasePathAwareHandlerMapping(configuration);
  mapping.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
  mapping.afterPropertiesSet();

  Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

  for (RequestMappingInfo info : handlerMethods.keySet()) {
   assertThat(info.getPatternsCondition().getPatterns(), hasItem(startsWith("/api")));
  }
 }

 @Controller
 static class SampleController {

  @RequestMapping("/foo")
  void someMethod() {}
 }
}