/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.data.redis.connection;


/**
 * Interface for the commands supported by Redis.
 * 
 * @author Costin Leau
 */
public interface RedisCommands extends RedisKeyCommands, RedisStringCommands, RedisListCommands, RedisSetCommands,
  RedisZSetCommands, RedisHashCommands, RedisTxCommands, RedisPubSubCommands, RedisConnectionCommands,
  RedisServerCommands, RedisScriptingCommands {


 /**
  * 'Native' or 'raw' execution of the given command along-side the given arguments.
  * The command is executed as is, with as little 'interpretation' as possible - it is up to the caller
  * to take care of any processing of arguments or the result.
  * 
  * @param command Command to execute
  * @param args Possible command arguments (may be null)
  * @return execution result.
  */
 Object execute(String command, byte[]... args);
}