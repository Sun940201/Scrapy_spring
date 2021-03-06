/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.geo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom {@link Page} to carry the average distance retrieved from the {@link GeoResults} the {@link GeoPage} is set up
 * from.
 * 
 * @deprecated As of release 1.5, replaced by {@link org.springframework.data.geo.GeoPage}. This class is scheduled to
 *             be removed in the next major release.
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Deprecated
public class GeoPage<T> extends org.springframework.data.geo.GeoPage<T> {

 private static final long serialVersionUID = 23421312312412L;

 /**
  * Creates a new {@link GeoPage} from the given {@link GeoResults}.
  * 
  * @param content must not be {@literal null}.
  */
 public GeoPage(GeoResults<T> results) {
  super(results);
 }

 /**
  * Creates a new {@link GeoPage} from the given {@link GeoResults}, {@link Pageable} and total.
  * 
  * @param results must not be {@literal null}.
  * @param pageable must not be {@literal null}.
  * @param total
  */
 public GeoPage(GeoResults<T> results, Pageable pageable, long total) {
  super(results, pageable, total);
 }
}