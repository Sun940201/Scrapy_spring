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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.cassandra.core.Ordering;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.cassandra.core.keyspace.ColumnSpecification;
import org.springframework.cassandra.core.keyspace.CreateTableSpecification;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.convert.CustomConversions;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.util.ClassTypeInformation;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.DataType.Name;

/**
 * Unit tests for {@link BasicCassandraMappingContext}.
 *
 * @author Matthew T. Adams
 * @author Mark Paluch
 */
public class BasicCassandraMappingContextUnitTests {

 BasicCassandraMappingContext mappingContext = new BasicCassandraMappingContext();

 @Test(expected = MappingException.class)
 public void testGetPersistentEntityOfTransientType() {
  mappingContext.getPersistentEntity(Transient.class);
 }

 private static class Transient {}

 @Test
 public void testGetExistingPersistentEntityHappyPath() {

  mappingContext.getPersistentEntity(X.class);

  assertThat(mappingContext.contains(X.class)).isTrue();
  assertThat(mappingContext.getExistingPersistentEntity(X.class)).isNotNull();
  assertThat(mappingContext.contains(Y.class)).isFalse();
 }

 @Table
 private static class X {
  @PrimaryKey String key;
 }

 @Table
 private static class Y {
  @PrimaryKey String key;
 }

 /**
  * @see DATACASS-248
  */
 @Test
 public void primaryKeyOnPropertyShouldWork() {

  CassandraPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(PrimaryKeyOnProperty.class);

  CassandraPersistentProperty idProperty = persistentEntity.getIdProperty();

  assertThat(idProperty.getColumnName().toCql()).isEqualTo("foo");

  List<CqlIdentifier> columnNames = idProperty.getColumnNames();

  assertThat(columnNames).hasSize(1);
  assertThat(columnNames.get(0).toCql()).isEqualTo("foo");
 }

 @Table
 private static class PrimaryKeyOnProperty {

  String key;

  @PrimaryKey(value = "foo")
  public String getKey() {
   return key;
  }

  public void setKey(String key) {
   this.key = key;
  }
 }

 /**
  * @see DATACASS-248
  */
 @Test
 public void primaryKeyColumnsOnPropertyShouldWork() {

  CassandraPersistentEntity<?> persistentEntity = mappingContext
    .getPersistentEntity(PrimaryKeyColumnsOnProperty.class);

  assertThat(persistentEntity.isCompositePrimaryKey()).isFalse();

  CassandraPersistentProperty firstname = persistentEntity.getPersistentProperty("firstname");

  assertThat(firstname.isCompositePrimaryKey()).isFalse();
  assertThat(firstname.isPrimaryKeyColumn()).isTrue();
  assertThat(firstname.isPartitionKeyColumn()).isTrue();
  assertThat(firstname.getColumnName().toCql()).isEqualTo("firstname");

  CassandraPersistentProperty lastname = persistentEntity.getPersistentProperty("lastname");

  assertThat(lastname.isPrimaryKeyColumn()).isTrue();
  assertThat(lastname.isClusterKeyColumn()).isTrue();
  assertThat(lastname.getColumnName().toCql()).isEqualTo("mylastname");
 }

 @Table
 private static class PrimaryKeyColumnsOnProperty {

  String firstname;
  String lastname;

  @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED)
  public String getFirstname() {
   return firstname;
  }

  public void setFirstname(String firstname) {
   this.firstname = firstname;
  }

  @PrimaryKeyColumn(name = "mylastname", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
  public String getLastname() {
   return lastname;
  }

  public void setLastname(String lastname) {
   this.lastname = lastname;
  }
 }

 /**
  * @see DATACASS-248
  */
 @Test
 public void primaryKeyClassWithPrimaryKeyColumnsOnPropertyShouldWork() {

  CassandraPersistentEntity<?> persistentEntity = mappingContext
    .getPersistentEntity(PrimaryKeyOnPropertyWithPrimaryKeyClass.class);

  CassandraPersistentEntity<?> primaryKeyClass = mappingContext
    .getPersistentEntity(CompositePrimaryKeyClassWithProperties.class);

  assertThat(persistentEntity.isCompositePrimaryKey()).isFalse();
  assertThat(persistentEntity.getPersistentProperty("key").isCompositePrimaryKey()).isTrue();

  assertThat(primaryKeyClass.isCompositePrimaryKey()).isTrue();
  assertThat(primaryKeyClass.getCompositePrimaryKeyProperties()).hasSize(2);

  CassandraPersistentProperty firstname = primaryKeyClass.getPersistentProperty("firstname");

  assertThat(firstname.isPrimaryKeyColumn()).isTrue();
  assertThat(firstname.isPartitionKeyColumn()).isTrue();
  assertThat(firstname.isClusterKeyColumn()).isFalse();
  assertThat(firstname.getColumnName().toCql()).isEqualTo("firstname");

  CassandraPersistentProperty lastname = primaryKeyClass.getPersistentProperty("lastname");

  assertThat(lastname.isPrimaryKeyColumn()).isTrue();
  assertThat(lastname.isPartitionKeyColumn()).isFalse();
  assertThat(lastname.isClusterKeyColumn()).isTrue();
  assertThat(lastname.getColumnName().toCql()).isEqualTo("mylastname");
 }

 /**
  * @see DATACASS-340
  */
 @Test
 public void createdTableSpecificationShouldConsiderClusterColumnOrdering() {

  CassandraPersistentEntity<?> persistentEntity = mappingContext
    .getPersistentEntity(EntityWithOrderedClusteredColumns.class);

  CreateTableSpecification tableSpecification = mappingContext.getCreateTableSpecificationFor(persistentEntity);

  assertThat(tableSpecification.getPartitionKeyColumns()).hasSize(1);
  assertThat(tableSpecification.getClusteredKeyColumns()).hasSize(3);

  ColumnSpecification breed = tableSpecification.getClusteredKeyColumns().get(0);
  assertThat(breed.getName().toCql()).isEqualTo("breed");
  assertThat(breed.getOrdering()).isEqualTo(Ordering.ASCENDING);

  ColumnSpecification color = tableSpecification.getClusteredKeyColumns().get(1);
  assertThat(color.getName().toCql()).isEqualTo("color");
  assertThat(color.getOrdering()).isEqualTo(Ordering.DESCENDING);

  ColumnSpecification kind = tableSpecification.getClusteredKeyColumns().get(2);
  assertThat(kind.getName().toCql()).isEqualTo("kind");
  assertThat(kind.getOrdering()).isEqualTo(Ordering.ASCENDING);
 }

 /**
  * @see DATACASS-340
  */
 @Test
 public void createdTableSpecificationShouldConsiderPrimaryKeyClassClusterColumnOrdering() {

  CassandraPersistentEntity<?> persistentEntity = mappingContext
    .getPersistentEntity(EntityWithPrimaryKeyWithOrderedClusteredColumns.class);

  CreateTableSpecification tableSpecification = mappingContext.getCreateTableSpecificationFor(persistentEntity);

  assertThat(tableSpecification.getPartitionKeyColumns()).hasSize(1);
  assertThat(tableSpecification.getClusteredKeyColumns()).hasSize(3);

  ColumnSpecification breed = tableSpecification.getClusteredKeyColumns().get(0);
  assertThat(breed.getName().toCql()).isEqualTo("breed");
  assertThat(breed.getOrdering()).isEqualTo(Ordering.ASCENDING);

  ColumnSpecification color = tableSpecification.getClusteredKeyColumns().get(1);
  assertThat(color.getName().toCql()).isEqualTo("color");
  assertThat(color.getOrdering()).isEqualTo(Ordering.DESCENDING);

  ColumnSpecification kind = tableSpecification.getClusteredKeyColumns().get(2);
  assertThat(kind.getName().toCql()).isEqualTo("kind");
  assertThat(kind.getOrdering()).isEqualTo(Ordering.ASCENDING);
 }

 @Table
 private static class PrimaryKeyOnPropertyWithPrimaryKeyClass {

  CompositePrimaryKeyClassWithProperties key;

  @PrimaryKey
  public CompositePrimaryKeyClassWithProperties getKey() {
   return key;
  }

  public void setKey(CompositePrimaryKeyClassWithProperties key) {
   this.key = key;
  }
 }

 @PrimaryKeyClass
 private static class CompositePrimaryKeyClassWithProperties implements Serializable {

  String firstname;
  String lastname;

  @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED)
  public String getFirstname() {
   return firstname;
  }

  public void setFirstname(String firstname) {
   this.firstname = firstname;
  }

  @PrimaryKeyColumn(name = "mylastname", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
  public String getLastname() {
   return lastname;
  }

  public void setLastname(String lastname) {
   this.lastname = lastname;
  }
 }

 @Table
 static class EntityWithOrderedClusteredColumns {

  @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED) String species;
  @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING) String breed;
  @PrimaryKeyColumn(ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING) String color;
  @PrimaryKeyColumn(ordinal = 3, type = PrimaryKeyType.CLUSTERED) String kind;
 }

 @PrimaryKeyClass
 static class PrimaryKeyWithOrderedClusteredColumns implements Serializable {

  @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED) String species;
  @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING) String breed;
  @PrimaryKeyColumn(ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING) String color;
  @PrimaryKeyColumn(ordinal = 3, type = PrimaryKeyType.CLUSTERED) String kind;
 }

 @Table
 private static class EntityWithPrimaryKeyWithOrderedClusteredColumns {

  @PrimaryKey PrimaryKeyWithOrderedClusteredColumns key;
 }

 /**
  * @see DATACASS-296
  */
 @Test
 public void shouldCreatePersistentEntityIfNoConversionRegistered() {

  mappingContext.setCustomConversions(new CustomConversions(Collections.EMPTY_LIST));
  assertThat(mappingContext.shouldCreatePersistentEntityFor(ClassTypeInformation.from(Human.class))).isTrue();
 }

 /**
  * @see DATACASS-296
  */
 @Test
 public void shouldNotCreateEntitiesForCustomConvertedTypes() {

  mappingContext
    .setCustomConversions(new CustomConversions(Collections.singletonList(HumanToStringConverter.INSTANCE)));

  assertThat(mappingContext.shouldCreatePersistentEntityFor(ClassTypeInformation.from(Human.class))).isFalse();
 }

 /**
  * @see DATACASS-349
  */
 @Test
 public void propertyTypeShouldConsiderRegisteredConverterForPropertyType() {

  mappingContext
    .setCustomConversions(new CustomConversions(Collections.singletonList(StringMapToStringConverter.INSTANCE)));

  CassandraPersistentEntity<?> persistentEntity = mappingContext
    .getPersistentEntity(TypeWithCustomConvertedMap.class);

  assertThat(mappingContext.getDataType(persistentEntity.getPersistentProperty("stringMap")))
    .isEqualTo(DataType.varchar());

  assertThat(mappingContext.getDataType(persistentEntity.getPersistentProperty("blobMap")))
    .isEqualTo(DataType.ascii());
 }

 /**
  * @see DATACASS-349
  */
 @Test
 public void propertyTypeShouldConsiderRegisteredConverterForCollectionComponentType() {

  mappingContext
    .setCustomConversions(new CustomConversions(Collections.singletonList(HumanToStringConverter.INSTANCE)));

  CassandraPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(TypeWithListOfHumans.class);

  assertThat(mappingContext.getDataType(persistentEntity.getPersistentProperty("humans")))
    .isEqualTo(DataType.list(DataType.varchar()));
 }

 private static class Human {}

 enum HumanToStringConverter implements Converter<Human, String> {

  INSTANCE;

  @Override
  public String convert(Human source) {
   return "hello";
  }
 }

 @Table
 private static class TypeWithCustomConvertedMap {

  @Id String id;
  Map<String, Collection<String>> stringMap;

  @CassandraType(type = Name.ASCII) Map<String, Collection<String>> blobMap;
 }

 @Table
 private static class TypeWithListOfHumans {

  @Id String id;
  List<Human> humans;
 }

 @WritingConverter
 enum StringMapToStringConverter implements Converter<Map<String, Collection<String>>, String> {

  INSTANCE;

  @Override
  public String convert(Map<String, Collection<String>> source) {
   return "serialized";
  }
 }
}