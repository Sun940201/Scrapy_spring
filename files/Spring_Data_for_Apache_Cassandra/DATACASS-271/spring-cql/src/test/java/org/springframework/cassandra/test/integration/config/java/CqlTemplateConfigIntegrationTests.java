/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.cassandra.test.integration.config.java;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cassandra.config.java.AbstractCqlTemplateConfiguration;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.cassandra.support.RandomKeySpaceName;
import org.springframework.cassandra.test.integration.AbstractEmbeddedCassandraIntegrationTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.datastax.driver.core.Session;

/**
 * @author Matthews T. Adams
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class CqlTemplateConfigIntegrationTests extends AbstractEmbeddedCassandraIntegrationTest {

 public static final String KEYSPACE_NAME = RandomKeySpaceName.create();

 @Configuration
 public static class Config extends AbstractCqlTemplateConfiguration {

  @Override
  protected String getKeyspaceName() {
   return "system";
  }

  @Override
  protected int getPort() {
   return cassandraEnvironment.getPort();
  }
 }

 Session session;
 ConfigurableApplicationContext context;

 @Before
 public void setUp() {
  this.context = new AnnotationConfigApplicationContext(Config.class);
  this.session = context.getBean(Session.class);
 }

 @After
 public void tearDown() {
  context.close();
 }

 @Test
 public void test() {

  CqlTemplate cqlTemplate = context.getBean(CqlTemplate.class);
  assertThat(cqlTemplate.describeRing().size(), is(greaterThan(0)));
 }
}