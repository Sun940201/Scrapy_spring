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
package org.springframework.data.jdbc.repository.support;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.mapping.context.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntityImpl;
import org.springframework.data.jdbc.repository.SimpleJdbcRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * @author Jens Schauder
 * @since 2.0
 */
@RequiredArgsConstructor
public class JdbcRepositoryFactory extends RepositoryFactorySupport {

 private final JdbcMappingContext context = new JdbcMappingContext();
 private final NamedParameterJdbcOperations jdbcOperations;
 private final ApplicationEventPublisher publisher;

 @SuppressWarnings("unchecked")
 @Override
 public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {

  JdbcPersistentEntityImpl<?> persistentEntity = context.getPersistentEntity(aClass);
  if (persistentEntity == null)
   return null;
  return new BasicJdbcPersistentEntityInformation<T, ID>((JdbcPersistentEntity<T>) persistentEntity);
 }

 @Override
 protected Object getTargetRepository(RepositoryInformation repositoryInformation) {

  JdbcPersistentEntity<?> persistentEntity = context
    .getRequiredPersistentEntity(repositoryInformation.getDomainType());

  return new SimpleJdbcRepository<>(persistentEntity, jdbcOperations, publisher);
 }

 @Override
 protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
  return SimpleJdbcRepository.class;
 }
}