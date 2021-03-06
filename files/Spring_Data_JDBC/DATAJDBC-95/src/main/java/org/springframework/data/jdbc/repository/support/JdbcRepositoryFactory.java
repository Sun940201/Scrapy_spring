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

import java.io.Serializable;
import javax.sql.DataSource;
import org.springframework.data.jdbc.mapping.context.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity;
import org.springframework.data.jdbc.repository.SimpleJdbcRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * @author Jens Schauder
 */
public class JdbcRepositoryFactory extends RepositoryFactorySupport {

 private final DataSource dataSource;
 private final JdbcMappingContext context = new JdbcMappingContext();

 public JdbcRepositoryFactory(DataSource dataSource) {
  this.dataSource = dataSource;
 }

 @Override
 public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {
  JdbcPersistentEntity<T> persistentEntity = (JdbcPersistentEntity<T>) context.getPersistentEntity(aClass);
  return new JdbcPersistentEntityInformation<T, ID>(persistentEntity);
 }

 @Override
 protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
  return new SimpleJdbcRepository(getEntityInformation(repositoryInformation.getDomainType()), dataSource);
 }

 @Override
 protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
  return SimpleJdbcRepository.class;
 }
}