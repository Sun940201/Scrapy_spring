/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.cassandra.test.integration.config.xml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cassandra.test.integration.AbstractKeyspaceCreatingIntegrationTest;
import org.springframework.cassandra.test.integration.config.IntegrationTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.datastax.driver.core.Session;

/**
 * @author Matthews T. Adams
 * @author Oliver Gierke
 */
public class XmlConfigTest extends AbstractKeyspaceCreatingIntegrationTest {

 public static final String KEYSPACE = "xmlconfigtest";

 Session session;
 ConfigurableApplicationContext context;
 
 public XmlConfigTest() {
  super(KEYSPACE);
 }
 
 @Before
 public void setUp() {
  
  this.context = new ClassPathXmlApplicationContext("XmlConfigTest-context.xml", getClass());
  this.session = context.getBean(Session.class);
 }
 
 @After
 public void tearDown() {
  context.close();
 }

 @Test
 public void test() {
  IntegrationTestUtils.assertSession(session);
  IntegrationTestUtils.assertKeyspaceExists(KEYSPACE, session);
 }
}