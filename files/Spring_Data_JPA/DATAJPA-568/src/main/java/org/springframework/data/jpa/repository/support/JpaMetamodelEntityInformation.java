/*
 * Copyright 2011-2014 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.IdClass;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.Type.PersistenceType;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.data.repository.core.EntityInformation} that uses JPA {@link Metamodel}
 * to find the domain class' id field.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class JpaMetamodelEntityInformation<T, ID extends Serializable> extends JpaEntityInformationSupport<T, ID> {

 private final IdMetadata<T> idMetadata;
 private final SingularAttribute<? super T, ?> versionAttribute;
 private final Metamodel metamodel;
 private final String entityName;

 /**
  * Creates a new {@link JpaMetamodelEntityInformation} for the given domain class and {@link Metamodel}.
  * 
  * @param domainClass must not be {@literal null}.
  * @param metamodel must not be {@literal null}.
  */
 public JpaMetamodelEntityInformation(Class<T> domainClass, Metamodel metamodel) {

  super(domainClass);

  Assert.notNull(metamodel);
  this.metamodel = metamodel;

  ManagedType<T> type = metamodel.managedType(domainClass);
  if (type == null) {
   throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel!");
  }

  this.entityName = type instanceof EntityType ? ((EntityType<?>) type).getName() : null;

  if (!(type instanceof IdentifiableType)) {
   throw new IllegalArgumentException("The given domain class does not contain an id attribute!");
  }

  this.idMetadata = new IdMetadata<T>((IdentifiableType<T>) type);
  this.versionAttribute = findVersionAttribute(type);
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.jpa.repository.support.JpaEntityInformationSupport#getEntityName()
  */
 @Override
 public String getEntityName() {
  return entityName != null ? entityName : super.getEntityName();
 }

 /**
  * Returns the version attribute of the given {@link ManagedType} or {@literal null} if none available.
  * 
  * @param type must not be {@literal null}.
  * @return
  */
 private static <T> SingularAttribute<? super T, ?> findVersionAttribute(ManagedType<T> type) {

  Set<SingularAttribute<? super T, ?>> attributes = type.getSingularAttributes();

  for (SingularAttribute<? super T, ?> attribute : attributes) {
   if (attribute.isVersion()) {
    return attribute;
   }
  }

  return null;
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
  */
 @SuppressWarnings("unchecked")
 public ID getId(T entity) {

  BeanWrapper entityWrapper = new DirectFieldAccessFallbackBeanWrapper(entity);

  if (idMetadata.hasSimpleId()) {
   return (ID) entityWrapper.getPropertyValue(idMetadata.getSimpleIdAttribute().getName());
  }

  BeanWrapper idWrapper = new IdentifierDerivingDirectFieldAccessFallbackBeanWrapper(idMetadata.getType(), metamodel);
  boolean partialIdValueFound = false;

  for (SingularAttribute<? super T, ?> attribute : idMetadata) {
   Object propertyValue = entityWrapper.getPropertyValue(attribute.getName());

   if (propertyValue != null) {
    partialIdValueFound = true;
   }

   idWrapper.setPropertyValue(attribute.getName(), propertyValue);
  }

  return (ID) (partialIdValueFound ? idWrapper.getWrappedInstance() : null);
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.repository.core.EntityInformation#getIdType()
  */
 @SuppressWarnings("unchecked")
 public Class<ID> getIdType() {
  return (Class<ID>) idMetadata.getType();
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.jpa.repository.support.JpaEntityInformation#getIdAttribute()
  */
 public SingularAttribute<? super T, ?> getIdAttribute() {
  return idMetadata.getSimpleIdAttribute();
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.jpa.repository.support.JpaEntityInformation#hasCompositeId()
  */
 public boolean hasCompositeId() {
  return !idMetadata.hasSimpleId();
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.jpa.repository.support.JpaEntityInformation#getIdAttributeNames()
  */
 public Iterable<String> getIdAttributeNames() {

  List<String> attributeNames = new ArrayList<String>(idMetadata.attributes.size());

  for (SingularAttribute<? super T, ?> attribute : idMetadata.attributes) {
   attributeNames.add(attribute.getName());
  }

  return attributeNames;
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.data.jpa.repository.support.JpaEntityInformation#getCompositeIdAttributeValue(java.io.Serializable, java.lang.String)
  */
 public Object getCompositeIdAttributeValue(Serializable id, String idAttribute) {
  Assert.isTrue(hasCompositeId());
  return new DirectFieldAccessFallbackBeanWrapper(id).getPropertyValue(idAttribute);
 }

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.repository.core.support.AbstractEntityInformation#isNew(java.lang.Object)
  */
 @Override
 public boolean isNew(T entity) {

  if (versionAttribute == null) {
   return super.isNew(entity);
  }

  BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(entity);
  Object versionValue = wrapper.getPropertyValue(versionAttribute.getName());

  if (versionValue == null) {
   return true;
  }

  return versionAttribute.getJavaType().isPrimitive() && ((Number) versionValue).longValue() == 0;
 }

 /**
  * Simple value object to encapsulate id specific metadata.
  * 
  * @author Oliver Gierke
  */
 private static class IdMetadata<T> implements Iterable<SingularAttribute<? super T, ?>> {

  private final IdentifiableType<T> type;
  private final Set<SingularAttribute<? super T, ?>> attributes;
  private Class<?> idType;

  @SuppressWarnings("unchecked")
  public IdMetadata(IdentifiableType<T> source) {

   this.type = source;
   this.attributes = (Set<SingularAttribute<? super T, ?>>) (source.hasSingleIdAttribute() ? Collections
     .singleton(source.getId(source.getIdType().getJavaType())) : source.getIdClassAttributes());
  }

  public boolean hasSimpleId() {
   return attributes.size() == 1;
  }

  public Class<?> getType() {

   if (idType != null) {
    return idType;
   }

   Class<?> idType;

   try {
    Type<?> idType2 = type.getIdType();
    idType = idType2 == null ? fallbackIdTypeLookup(type) : idType2.getJavaType();
   } catch (IllegalStateException e) {
    // see https://hibernate.onjira.com/browse/HHH-6951
    idType = fallbackIdTypeLookup(type);
   }

   this.idType = idType;
   return idType;
  }

  private static Class<?> fallbackIdTypeLookup(IdentifiableType<?> type) {

   IdClass annotation = AnnotationUtils.findAnnotation(type.getJavaType(), IdClass.class);
   return annotation == null ? null : annotation.value();
  }

  public SingularAttribute<? super T, ?> getSimpleIdAttribute() {
   return attributes.iterator().next();
  }

  /* 
   * (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  public Iterator<SingularAttribute<? super T, ?>> iterator() {
   return attributes.iterator();
  }
 }

 /**
  * Custom extension of {@link DirectFieldAccessFallbackBeanWrapper} that allows to derived the identifier if composite
  * keys with complex key attribute types (e.g. types that are annotated with {@code @Entity} themselves) are used.
  * 
  * @author Thomas Darimont
  */
 private static class IdentifierDerivingDirectFieldAccessFallbackBeanWrapper extends
   DirectFieldAccessFallbackBeanWrapper {

  private final Metamodel metamodel;

  public IdentifierDerivingDirectFieldAccessFallbackBeanWrapper(Class<?> type, Metamodel metamodel) {
   super(type);
   this.metamodel = metamodel;
  }

  /**
   * In addition to the functionality described in {@link BeanWrapperImpl} it is checked whether we have a nested
   * entity that is part of the id key. If this is the case, we need to derive the identifier of the nested entity.
   * 
   * @see org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation.DirectFieldAccessFallbackBeanWrapper#setPropertyValue(java.lang.String,
   *      java.lang.Object)
   */
  @Override
  public void setPropertyValue(String propertyName, Object value) {

   if (isIdentifierDerivationNecessary(value)) {

    // Derive the identifer from the nested entity that is part of the composite key.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    JpaMetamodelEntityInformation nestedEntityInformation = new JpaMetamodelEntityInformation(value.getClass(),
      this.metamodel);
    Object nestedIdPropertyValue = new DirectFieldAccessFallbackBeanWrapper(value)
      .getPropertyValue(nestedEntityInformation.getIdAttribute().getName());
    super.setPropertyValue(propertyName, nestedIdPropertyValue);

    return;
   }

   super.setPropertyValue(propertyName, value);
  }

  /**
   * @param value
   * @return {@literal true} if the given value is not {@literal null} and a mapped persistable entity otherwise
   *         {@literal false}
   */
  private boolean isIdentifierDerivationNecessary(Object value) {

   if (value == null) {
    return false;
   }

   try {
    ManagedType<? extends Object> managedType = this.metamodel.managedType(value.getClass());
    return managedType != null && managedType.getPersistenceType() == PersistenceType.ENTITY;
   } catch (IllegalArgumentException iae) {
    // no mapped type
    return false;
   }
  }
 }
}