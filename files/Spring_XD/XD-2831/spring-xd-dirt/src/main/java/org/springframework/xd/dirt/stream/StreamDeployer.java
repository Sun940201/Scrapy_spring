/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.springframework.xd.dirt.stream.ParsingContext.stream;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.XDDSLMessages;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Default implementation of {@link StreamDeployer} that uses provided
 * {@link StreamDefinitionRepository} and {@link StreamRepository} to
 * persist stream deployment and undeployment requests.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Patrick Peralta
 */
public class StreamDeployer extends AbstractInstancePersistingDeployer<StreamDefinition, Stream> {

 /**
  * Stream definition parser.
  */
 private final XDParser parser;

 /**
  * Construct a StreamDeployer.
  *
  * @param zkConnection       ZooKeeper connection
  * @param repository         repository for stream definitions
  * @param streamRepository   repository for stream instances
  * @param parser             stream definition parser
  */
 public StreamDeployer(ZooKeeperConnection zkConnection, StreamDefinitionRepository repository,
   StreamRepository streamRepository, XDParser parser) {
  super(zkConnection, repository, streamRepository, parser, stream);
  this.parser = parser;
 }

 /**
  * {@inheritDoc}
  */
 @Override
 protected Stream makeInstance(StreamDefinition definition) {
  return new Stream(definition);
 }

 /**
  * {@inheritDoc}
  */
 @Override
 protected StreamDefinition createDefinition(String name, String definition) {
  return new StreamDefinition(name, definition);
 }

 /**
  * {@inheritDoc}
  */
 @Override
 protected String getDeploymentPath(StreamDefinition definition) {
  return Paths.build(Paths.STREAM_DEPLOYMENTS, definition.getName());
 }

}