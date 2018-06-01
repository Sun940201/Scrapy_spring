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

package org.springframework.integration.hazelcast.inbound;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;

/**
 * Hazelcast Distributed Queue Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @since 1.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class HazelcastDistributedQueueEventDrivenInboundChannelAdapterTests {

 @Autowired
 private PollableChannel edQueueChannel1;

 @Autowired
 private PollableChannel edQueueChannel2;

 @Autowired
 private PollableChannel edQueueChannel3;

 @Resource
 private IQueue<HazelcastIntegrationTestUser> edDistributedQueue1;

 @Resource
 private IQueue<HazelcastIntegrationTestUser> edDistributedQueue2;

 @Resource
 private IQueue<HazelcastIntegrationTestUser> edDistributedQueue3;

 @Test
 public void testEventDrivenForOnlyADDEDEntryEvent() {
  edDistributedQueue1.add(new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
  Message<?> msg = edQueueChannel1.receive(2_000);
  Assert.assertNotNull(msg);
  Assert.assertNotNull(msg.getPayload());
  Assert.assertTrue(msg.getPayload() instanceof ItemEvent);
  Assert.assertEquals(EntryEventType.ADDED.toString(),
    ((ItemEvent<?>) msg.getPayload()).getEventType().toString());
  Assert.assertEquals(1,
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getId());
  Assert.assertEquals("TestName1",
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getName());
  Assert.assertEquals("TestSurname1",
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getSurname());
 }

 @Test
 public void testEventDrivenForOnlyREMOVEDEntryEvent() {
  HazelcastIntegrationTestUser user = new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2");
  edDistributedQueue2.add(user);
  edDistributedQueue2.remove(user);
  Message<?> msg = edQueueChannel2.receive(2_000);
  Assert.assertNotNull(msg);
  Assert.assertNotNull(msg.getPayload());
  Assert.assertTrue(msg.getPayload() instanceof ItemEvent);
  Assert.assertEquals(EntryEventType.REMOVED.toString(),
    ((ItemEvent<?>) msg.getPayload()).getEventType().toString());
  Assert.assertEquals(2,
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getId());
  Assert.assertEquals("TestName2",
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getName());
  Assert.assertEquals("TestSurname2",
    ((HazelcastIntegrationTestUser) ((ItemEvent<?>) msg.getPayload()).getItem()).getSurname());
 }

 @Test
 public void testEventDrivenForALLEntryEvent() {
  HazelcastIntegrationTestUser user = new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1");
  edDistributedQueue3.add(user);
  Message<?> msg = edQueueChannel3.receive(2_000);
  verify(msg, EntryEventType.ADDED);

  edDistributedQueue3.remove(user);
  msg = edQueueChannel3.receive(2_000);
  verify(msg, EntryEventType.REMOVED);

  user = new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2");
  edDistributedQueue3.add(user);
  msg = edQueueChannel3.receive(2_000);
  verify(msg, EntryEventType.ADDED);
 }

 private void verify(Message<?> msg, EntryEventType type) {
  Assert.assertNotNull(msg);
  Assert.assertNotNull(msg.getPayload());
  Assert.assertTrue(msg.getPayload() instanceof ItemEvent);
  Assert.assertEquals(type.toString(), ((ItemEvent<?>) msg.getPayload())
    .getEventType().toString());
 }

}