/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import groovy.lang.MetaClass;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit test for {@link AbstractMappingContext}.
 * 
 * @author Oliver Gierke
 */
public class AbstractMappingContextUnitTests {

 final SimpleTypeHolder holder = new SimpleTypeHolder();
 SampleMappingContext context;

 @Before
 public void setUp() {
  context = new SampleMappingContext();
  context.setSimpleTypeHolder(holder);
 }

 @Test
 public void doesNotTryToLookupPersistentEntityForLeafProperty() {
  PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath(PropertyPath.from("name",
    Person.class));
  assertThat(path, is(notNullValue()));
 }

 /**
  * @see DATACMNS-92
  */
 @Test(expected = MappingException.class)
 public void doesNotAddInvalidEntity() {

  context = new SampleMappingContext() {
   @Override
   @SuppressWarnings("unchecked")
   protected <S> BasicPersistentEntity<Object, SamplePersistentProperty> createPersistentEntity(
     TypeInformation<S> typeInformation) {
    return new BasicPersistentEntity<Object, SamplePersistentProperty>((TypeInformation<Object>) typeInformation) {
     @Override
     public void verify() {
      if (Unsupported.class.isAssignableFrom(getType())) {
       throw new MappingException("Unsupported type!");
      }
     }
    };
   }
  };

  try {
   context.getPersistentEntity(Unsupported.class);
  } catch (MappingException e) {
   // expected
  }

  context.getPersistentEntity(Unsupported.class);
 }

 @Test
 public void registersEntitiesOnInitialization() {

  ApplicationContext context = mock(ApplicationContext.class);

  SampleMappingContext mappingContext = new SampleMappingContext();
  mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
  mappingContext.setApplicationEventPublisher(context);

  verify(context, times(0)).publishEvent(Mockito.any(ApplicationEvent.class));

  mappingContext.afterPropertiesSet();
  verify(context, times(1)).publishEvent(Mockito.any(ApplicationEvent.class));
 }

 /**
  * @see DATACMNS-214
  */
 @Test
 public void returnsNullPersistentEntityForSimpleTypes() {

  SampleMappingContext context = new SampleMappingContext();
  assertThat(context.getPersistentEntity(String.class), is(nullValue()));
 }

 /**
  * @see DATACMNS-214
  */
 @Test(expected = IllegalArgumentException.class)
 public void rejectsNullValueForGetPersistentEntityOfClass() {
  context.getPersistentEntity((Class<?>) null);
 }

 /**
  * @see DATACMNS-214
  */
 @Test(expected = IllegalArgumentException.class)
 public void rejectsNullValueForGetPersistentEntityOfTypeInformation() {
  context.getPersistentEntity((TypeInformation<?>) null);
 }

 /**
  * @see DATACMNS-228
  */
 @Test
 public void doesNotCreatePersistentPropertyForGroovyMetaClass() {

  SampleMappingContext mappingContext = new SampleMappingContext();
  mappingContext.initialize();

  PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext.getPersistentEntity(Sample.class);
  assertThat(entity.getPersistentProperty("metaClass"), is(nullValue()));
 }

 /**
  * @see DATACMNS-332
  */
 @Test
 public void usesMostConcreteProperty() {

  SampleMappingContext mappingContext = new SampleMappingContext();
  PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext.getPersistentEntity(Extension.class);
  assertThat(entity.getPersistentProperty("foo").isIdProperty(), is(true));
 }

 /**
  * @see DATACMNS-345
  */
 @Test
 @SuppressWarnings("rawtypes")
 public void returnsEntityForComponentType() {

  SampleMappingContext mappingContext = new SampleMappingContext();
  PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext.getPersistentEntity(Sample.class);
  SamplePersistentProperty property = entity.getPersistentProperty("persons");
  PersistentEntity<Object, SamplePersistentProperty> propertyEntity = mappingContext.getPersistentEntity(property);

  assertThat(propertyEntity, is(notNullValue()));
  assertThat(propertyEntity.getType(), is(equalTo((Class) Person.class)));
 }

 /**
  * @see DATACMNS-380
  */
 @Test
 public void returnsPersistentPropertyPathForDotPath() {

  PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("persons.name",
    Sample.class);

  assertThat(path.getLength(), is(2));
  assertThat(path.getBaseProperty().getName(), is("persons"));
  assertThat(path.getLeafProperty().getName(), is("name"));
 }

 /**
  * @see DATACMNS-380
  */
 @Test(expected = MappingException.class)
 public void rejectsInvalidPropertyReferenceWithMappingException() {
  context.getPersistentPropertyPath("foo", Sample.class);
 }

 /**
  * @see DATACMNS-390
  */
 @Test
 public void exposesCopyOfPersistentEntitiesToAvoidConcurrentModificationException() {

  SampleMappingContext context = new SampleMappingContext();
  context.getPersistentEntity(ClassTypeInformation.MAP);

  Iterator<BasicPersistentEntity<Object, SamplePersistentProperty>> iterator = context.getPersistentEntities()
    .iterator();

  while (iterator.hasNext()) {
   context.getPersistentEntity(ClassTypeInformation.SET);
   iterator.next();
  }
 }

 class Person {
  String name;
 }

 class Unsupported {

 }

 class Sample {

  MetaClass metaClass;
  List<Person> persons;
 }

 static class Base {
  String foo;
 }

 static class Extension extends Base {
  @Id String foo;
 }
}