/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.data.redis.connection.lettuce;

import org.springframework.data.redis.connection.Pool;

import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.api.StatefulConnection;

/**
 * Pool of Lettuce {@link StatefulConnection}s
 * 
 * @author Jennifer Hickey
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public interface LettucePool extends Pool<StatefulConnection<byte[], byte[]>> {

 /**
  * @return The {@link AbstractRedisClient} used to create pooled connections
  */
 AbstractRedisClient getClient();

}