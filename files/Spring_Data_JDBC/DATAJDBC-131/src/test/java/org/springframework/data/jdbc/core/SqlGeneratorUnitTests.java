/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.jdbc.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.mapping.model.DefaultNamingStrategy;
import org.springframework.data.jdbc.mapping.model.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentProperty;
import org.springframework.data.jdbc.mapping.model.NamingStrategy;
import org.springframework.data.mapping.PropertyPath;

/**
 * Unit tests for the {@link SqlGenerator}.
 * 
 * @author Jens Schauder
 * @author Greg Turnquist
 */
public class SqlGeneratorUnitTests {

 private SqlGenerator sqlGenerator;

 @Before
 public void setUp() {

  NamingStrategy namingStrategy = new PrefixingNamingStrategy();
  JdbcMappingContext context = new JdbcMappingContext(namingStrategy);
  JdbcPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DummyEntity.class);
  this.sqlGenerator = new SqlGenerator(context, persistentEntity, new SqlGeneratorSource(context));
 }

 @Test // DATAJDBC-112
 public void findOne() {

  String sql = sqlGenerator.getFindOne();

  SoftAssertions softAssertions = new SoftAssertions();
  softAssertions.assertThat(sql) //
    .startsWith("SELECT") //
    .contains("DummyEntity.x_id AS x_id,") //
    .contains("DummyEntity.x_name AS x_name,") //
    .contains("ref.x_l1id AS ref_x_l1id") //
    .contains("ref.x_content AS ref_x_content").contains(" FROM DummyEntity") //
    // 1-N relationships do not get loaded via join
    .doesNotContain("Element AS elements");
  softAssertions.assertAll();
 }

 @Test // DATAJDBC-112
 public void cascadingDeleteFirstLevel() {

  String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM ReferencedEntity WHERE DummyEntity = :rootId");
 }

 @Test // DATAJDBC-112
 public void cascadingDeleteAllSecondLevel() {

  String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref.further", DummyEntity.class));

  assertThat(sql).isEqualTo(
    "DELETE FROM SecondLevelReferencedEntity WHERE ReferencedEntity IN (SELECT x_l1id FROM ReferencedEntity WHERE DummyEntity = :rootId)");
 }

 @Test // DATAJDBC-112
 public void deleteAll() {

  String sql = sqlGenerator.createDeleteAllSql(null);

  assertThat(sql).isEqualTo("DELETE FROM DummyEntity");
 }

 @Test // DATAJDBC-112
 public void cascadingDeleteAllFirstLevel() {

  String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM ReferencedEntity WHERE DummyEntity IS NOT NULL");
 }

 @Test // DATAJDBC-112
 public void cascadingDeleteSecondLevel() {

  String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref.further", DummyEntity.class));

  assertThat(sql).isEqualTo(
    "DELETE FROM SecondLevelReferencedEntity WHERE ReferencedEntity IN (SELECT x_l1id FROM ReferencedEntity WHERE DummyEntity IS NOT NULL)");
 }

 @SuppressWarnings("unused")
 static class DummyEntity {

  @Id Long id;
  String name;
  ReferencedEntity ref;
  Set<Element> elements;
 }

 @SuppressWarnings("unused")
 static class ReferencedEntity {

  @Id Long l1id;
  String content;
  SecondLevelReferencedEntity further;
 }

 @SuppressWarnings("unused")
 static class SecondLevelReferencedEntity {

  @Id Long l2id;
  String something;
 }

 static class Element {
  @Id Long id;
  String content;
 }

 private static class PrefixingNamingStrategy extends DefaultNamingStrategy {

  @Override
  public String getColumnName(JdbcPersistentProperty property) {
   return "x_" + super.getColumnName(property);
  }

 }
}