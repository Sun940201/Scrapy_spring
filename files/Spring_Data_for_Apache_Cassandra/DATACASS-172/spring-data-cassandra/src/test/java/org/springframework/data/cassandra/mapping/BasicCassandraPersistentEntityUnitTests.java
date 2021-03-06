/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.cassandra.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link BasicCassandraPersistentEntity}.
 *
 * @author Alex Shvid
 * @author Matthew T. Adams
 * @author John Blum
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicCassandraPersistentEntityUnitTests {

 @Mock ApplicationContext context;

 @Test
 public void subclassInheritsAtDocumentAnnotation() {

  BasicCassandraPersistentEntity<Notification> entity = new BasicCassandraPersistentEntity<Notification>(
    ClassTypeInformation.from(Notification.class));
  assertThat(entity.getTableName().toCql()).isEqualTo("messages");
 }

 @Test
 public void evaluatesSpELExpression() {

  BasicCassandraPersistentEntity<Area> entity = new BasicCassandraPersistentEntity<Area>(
    ClassTypeInformation.from(Area.class));
  entity.setApplicationContext(context);
  assertThat(entity.getTableName().toCql()).isEqualTo("a123");
 }

 @Test
 public void tableAllowsReferencingSpringBean() {

  TableNameHolderThingy bean = new TableNameHolderThingy();
  bean.tableName = "my_user_line";

  when(context.getBean("tableNameHolderThingy")).thenReturn(bean);
  when(context.containsBean("tableNameHolderThingy")).thenReturn(true);

  BasicCassandraPersistentEntity<UserLine> entity = new BasicCassandraPersistentEntity<UserLine>(
    ClassTypeInformation.from(UserLine.class));
  entity.setApplicationContext(context);

  assertThat(entity.getTableName().toCql()).isEqualTo(bean.tableName);
 }

 @Test
 public void setForceQuoteCallsSetTableName() {
  BasicCassandraPersistentEntity<Message> entitySpy = spy(
    new BasicCassandraPersistentEntity<Message>(ClassTypeInformation.from(Message.class)));

  entitySpy.tableName = CqlIdentifier.cqlId("Messages", false);

  assertThat(entitySpy.forceQuote).isNull();

  entitySpy.setForceQuote(true);

  assertThat(entitySpy.forceQuote).isTrue();

  verify(entitySpy, times(1)).setTableName(isA(CqlIdentifier.class));
 }

 @Test
 public void setForceQuoteDoesNothing() {
  BasicCassandraPersistentEntity<Message> entitySpy = spy(
    new BasicCassandraPersistentEntity<Message>(ClassTypeInformation.from(Message.class)));

  entitySpy.forceQuote = true;
  entitySpy.setForceQuote(true);

  assertThat(entitySpy.forceQuote).isTrue();

  verify(entitySpy, never()).setTableName(isA(CqlIdentifier.class));
 }

 @Table("messages")
 static class Message {}

 static class Notification extends Message {}

 @Table("#{'a123'}")
 static class Area {}

 @Table("#{tableNameHolderThingy.tableName}")
 static class UserLine {}

 static class TableNameHolderThingy {

  String tableName;

  public String getTableName() {
   return tableName;
  }
 }
}