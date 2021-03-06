/*
 * Copyright 2011 the original author or authors.
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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.querydsl.core.dml.DeleteClause;
import com.querydsl.core.dml.UpdateClause;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathBuilderFactory;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAUpdateClause;

/**
 * Base class for implementing repositories using QueryDsl library.
 * 
 * @author Oliver Gierke
 */
@Repository
public abstract class QueryDslRepositorySupport {

 private final PathBuilder<?> builder;

 private EntityManager entityManager;
 private Querydsl querydsl;

 /**
  * Creates a new {@link QueryDslRepositorySupport} instance for the given domain type.
  * 
  * @param domainClass must not be {@literal null}.
  */
 public QueryDslRepositorySupport(Class<?> domainClass) {
  Assert.notNull(domainClass);
  this.builder = new PathBuilderFactory().create(domainClass);
 }

 /**
  * Setter to inject {@link EntityManager}.
  * 
  * @param entityManager must not be {@literal null}
  */
 @PersistenceContext
 public void setEntityManager(EntityManager entityManager) {

  Assert.notNull(entityManager);
  this.querydsl = new Querydsl(entityManager, builder);
  this.entityManager = entityManager;
 }

 /**
  * Callback to verify configuration. Used by containers.
  */
 @PostConstruct
 public void validate() {
  Assert.notNull(entityManager, "EntityManager must not be null!");
  Assert.notNull(querydsl, "Querydsl must not be null!");
 }

 /**
  * Returns the {@link EntityManager}.
  * 
  * @return the entityManager
  */
 protected EntityManager getEntityManager() {
  return entityManager;
 }

 /**
  * Returns a fresh {@link JPQLQuery}.
  * 
  * @param path must not be {@literal null}.
  * @return the Querydsl {@link JPQLQuery}.
  */
 protected JPQLQuery<Object> from(EntityPath<?>... paths) {
  return querydsl.createQuery(paths);
 }

 /**
  * Returns a {@link JPQLQuery} for the given {@link EntityPath}.
  * 
  * @param path must not be {@literal null}.
  * @return
  */
 protected <T> JPQLQuery<T> from(EntityPath<T> path) {
  return querydsl.createQuery(path).select(path);
 }

 /**
  * Returns a fresh {@link DeleteClause}.
  * 
  * @param path
  * @return the Querydsl {@link DeleteClause}.
  */
 protected DeleteClause<JPADeleteClause> delete(EntityPath<?> path) {
  return new JPADeleteClause(entityManager, path);
 }

 /**
  * Returns a fresh {@link UpdateClause}.
  * 
  * @param path
  * @return the Querydsl {@link UpdateClause}.
  */
 protected UpdateClause<JPAUpdateClause> update(EntityPath<?> path) {
  return new JPAUpdateClause(entityManager, path);
 }

 /**
  * Returns a {@link PathBuilder} for the configured domain type.
  * 
  * @param <T>
  * @return the Querdsl {@link PathBuilder}.
  */
 @SuppressWarnings("unchecked")
 protected <T> PathBuilder<T> getBuilder() {
  return (PathBuilder<T>) builder;
 }

 /**
  * Returns the underlying Querydsl helper instance.
  * 
  * @return
  */
 protected Querydsl getQuerydsl() {
  return this.querydsl;
 }
}