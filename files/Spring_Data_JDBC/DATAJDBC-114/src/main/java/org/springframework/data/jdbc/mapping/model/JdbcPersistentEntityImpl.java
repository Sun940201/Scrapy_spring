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
package org.springframework.data.jdbc.mapping.model;

import lombok.Getter;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

/**
 * Meta data a repository might need for implementing persistence operations for instances of type {@code T}
 *
 * @author Jens Schauder
 * @since 2.0
 */
public class JdbcPersistentEntityImpl<T> extends BasicPersistentEntity<T, JdbcPersistentProperty>
  implements JdbcPersistentEntity<T> {

 private final @Getter String tableName;

 /**
  * Creates a new {@link JdbcPersistentEntityImpl} for the given {@link TypeInformation}.
  * 
  * @param information must not be {@literal null}.
  */
 public JdbcPersistentEntityImpl(TypeInformation<T> information) {

  super(information);

  tableName = getType().getSimpleName();
 }

 /* 
  * (non-Javadoc)
  * @see org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity#getIdColumn()
  */
 @Override
 public String getIdColumn() {
  return getRequiredIdProperty().getName();
 }
}