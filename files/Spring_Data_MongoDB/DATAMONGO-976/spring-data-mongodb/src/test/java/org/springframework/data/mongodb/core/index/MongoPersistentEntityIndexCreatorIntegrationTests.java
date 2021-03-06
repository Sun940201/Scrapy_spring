/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mongodb.core.index;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link MongoPersistentEntityIndexCreator}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MongoPersistentEntityIndexCreatorIntegrationTests {

 @Autowired
 @Qualifier("mongo1")
 MongoOperations templateOne;

 @Autowired
 @Qualifier("mongo2")
 MongoOperations templateTwo;

 @After
 public void cleanUp() {
  templateOne.dropCollection(SampleEntity.class);
  templateTwo.dropCollection(SampleEntity.class);
 }

 @Test
 public void createsIndexForConfiguredMappingContextOnly() {

  List<IndexInfo> indexInfo = templateOne.indexOps(SampleEntity.class).getIndexInfo();
  assertThat(indexInfo, hasSize(greaterThan(0)));
  assertThat(indexInfo, Matchers.<IndexInfo> hasItem(hasProperty("name", is("prop"))));

  indexInfo = templateTwo.indexOps("sampleEntity").getIndexInfo();
  assertThat(indexInfo, hasSize(0));
 }
}