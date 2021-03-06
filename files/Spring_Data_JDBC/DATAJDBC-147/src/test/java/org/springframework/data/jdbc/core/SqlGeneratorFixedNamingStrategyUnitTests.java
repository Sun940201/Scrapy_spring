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

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.mapping.model.DefaultNamingStrategy;
import org.springframework.data.jdbc.mapping.model.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentProperty;
import org.springframework.data.jdbc.mapping.model.NamingStrategy;
import org.springframework.data.mapping.PropertyPath;

/**
 * Unit tests the {@link SqlGenerator} with a fixed {@link NamingStrategy} implementation containing a hard wired
 * schema, table, and property prefix.
 *
 * @author Greg Turnquist
 */
public class SqlGeneratorFixedNamingStrategyUnitTests {

 final NamingStrategy fixedCustomTablePrefixStrategy = new DefaultNamingStrategy() {

  @Override
  public String getSchema() {
   return "FixedCustomSchema";
  }

  @Override
  public String getTableName(Class<?> type) {
   return "FixedCustomTablePrefix_" + type.getSimpleName();
  }

  @Override
  public String getColumnName(JdbcPersistentProperty property) {
   return "FixedCustomPropertyPrefix_" + property.getName();
  }
 };

 final NamingStrategy upperCaseLowerCaseStrategy = new DefaultNamingStrategy() {

  @Override
  public String getTableName(Class<?> type) {
   return type.getSimpleName().toUpperCase();
  }

  @Override
  public String getColumnName(JdbcPersistentProperty property) {
   return property.getName().toLowerCase();
  }
 };

 @Test // DATAJDBC-107
 public void findOneWithOverriddenFixedTableName() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.getFindOne();

  SoftAssertions softAssertions = new SoftAssertions();
  softAssertions.assertThat(sql) //
    .startsWith("SELECT") //
    .contains(
      "FixedCustomSchema.FixedCustomTablePrefix_DummyEntity.FixedCustomPropertyPrefix_id AS FixedCustomPropertyPrefix_id,") //
    .contains(
      "FixedCustomSchema.FixedCustomTablePrefix_DummyEntity.FixedCustomPropertyPrefix_name AS FixedCustomPropertyPrefix_name,") //
    .contains("ref.FixedCustomPropertyPrefix_l1id AS ref_FixedCustomPropertyPrefix_l1id") //
    .contains("ref.FixedCustomPropertyPrefix_content AS ref_FixedCustomPropertyPrefix_content") //
    .contains("FROM FixedCustomSchema.FixedCustomTablePrefix_DummyEntity");
  softAssertions.assertAll();
 }

 @Test // DATAJDBC-107
 public void findOneWithUppercasedTablesAndLowercasedColumns() {

  SqlGenerator sqlGenerator = configureSqlGenerator(upperCaseLowerCaseStrategy);

  String sql = sqlGenerator.getFindOne();

  SoftAssertions softAssertions = new SoftAssertions();
  softAssertions.assertThat(sql) //
    .startsWith("SELECT") //
    .contains("DUMMYENTITY.id AS id,") //
    .contains("DUMMYENTITY.name AS name,") //
    .contains("ref.l1id AS ref_l1id") //
    .contains("ref.content AS ref_content") //
    .contains("FROM DUMMYENTITY");
  softAssertions.assertAll();
 }

 @Test // DATAJDBC-107
 public void cascadingDeleteFirstLevel() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_DummyEntity = :rootId");
 }

 @Test // DATAJDBC-107
 public void cascadingDeleteAllSecondLevel() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref.further", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_SecondLevelReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity IN "
    + "(SELECT FixedCustomPropertyPrefix_l1id " + "FROM FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_DummyEntity = :rootId)");
 }

 @Test // DATAJDBC-107
 public void deleteAll() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.createDeleteAllSql(null);

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_DummyEntity");
 }

 @Test // DATAJDBC-107
 public void cascadingDeleteAllFirstLevel() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_DummyEntity IS NOT NULL");
 }

 @Test // DATAJDBC-107
 public void cascadingDeleteSecondLevel() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref.further", DummyEntity.class));

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_SecondLevelReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity IN "
    + "(SELECT FixedCustomPropertyPrefix_l1id " + "FROM FixedCustomSchema.FixedCustomTablePrefix_ReferencedEntity "
    + "WHERE FixedCustomSchema.FixedCustomTablePrefix_DummyEntity IS NOT NULL)");
 }

 @Test // DATAJDBC-113
 public void deleteByList() {

  SqlGenerator sqlGenerator = configureSqlGenerator(fixedCustomTablePrefixStrategy);

  String sql = sqlGenerator.getDeleteByList();

  assertThat(sql).isEqualTo("DELETE FROM FixedCustomSchema.FixedCustomTablePrefix_DummyEntity WHERE FixedCustomPropertyPrefix_id IN (:ids)");
 }


 /**
  * Plug in a custom {@link NamingStrategy} for this test case.
  *
  * @param namingStrategy
  */
 private SqlGenerator configureSqlGenerator(NamingStrategy namingStrategy) {

  JdbcMappingContext context = new JdbcMappingContext(namingStrategy);
  JdbcPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DummyEntity.class);
  return new SqlGenerator(context, persistentEntity, new SqlGeneratorSource(context));
 }

 static class DummyEntity {

  @Id Long id;
  String name;
  ReferencedEntity ref;
 }

 static class ReferencedEntity {

  @Id Long l1id;
  String content;
  SecondLevelReferencedEntity further;
 }

 static class SecondLevelReferencedEntity {

  @Id Long l2id;
  String something;
 }

}