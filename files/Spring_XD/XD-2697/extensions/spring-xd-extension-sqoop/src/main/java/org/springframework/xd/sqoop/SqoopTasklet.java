/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.sqoop;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.batch.step.tasklet.x.AbstractProcessBuilderTasklet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tasklet used for running Sqoop tool.
 *
 * Note: This this class is not thread-safe.
 *
 * @since 1.1
 * @author Thomas Risberg
 */
public class SqoopTasklet extends AbstractProcessBuilderTasklet implements InitializingBean {

 private static final String SQOOP_RUNNER_CLASS = "org.springframework.xd.sqoop.SqoopRunner";

 private String[] arguments;


 public String[] getArguments() {
  return arguments;
 }

 public void setArguments(String[] arguments) {
  this.arguments = arguments;
 }

 @Override
 protected boolean isStoppable() {
  return false;
 }

 @Override
 protected List<String> createCommand() {
  List<String> command = new ArrayList<String>();
  command.add("java");
  command.add(SQOOP_RUNNER_CLASS);
  command.addAll(Arrays.asList(arguments));
  return command;
 }

 @Override
 protected String getCommandDisplayString() {
  if (arguments.length > 1) {
   return arguments[0] + " " + arguments[1];
  }
  else {
   return arguments[0];
  }
 }

 @Override
 protected String getCommandName() {
  return "Sqoop";
 }

 @Override
 protected String getCommandDescription() {
  return "Sqoop job for '" + arguments[0] + "'";
 }

 @Override
 public void afterPropertiesSet() throws Exception {
  if (arguments == null || arguments.length < 1) {
   throw new IllegalArgumentException("Missing arguments and/or configuration options for Sqoop");
  }
 }
}