/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.splunk.support;

import java.io.IOException;
import java.net.Socket;

import org.springframework.integration.splunk.core.ServiceFactory;
import org.springframework.util.Assert;

import com.splunk.Args;
import com.splunk.Index;
import com.splunk.Receiver;
import com.splunk.Service;

/**
 * 
 * DataWriter to stream data into Splunk using an optional index. If no index specified, 
 * the main default index is used.
 *
 * @author Jarred Li
 * @author David Turanski
 * @since 1.0
 *  
 */

public class SplunkIndexWriter extends AbstractSplunkDataWriter {
 private String indexName;
 /**
  *  
  * @param serviceFactory
  * @param args
  */
 public SplunkIndexWriter(ServiceFactory serviceFactory, Args args) {
  super(serviceFactory, args);
 }
 
 /* (non-Javadoc)
  * @see org.springframework.integration.splunk.support.SplunkDataWriter#createSocket(com.splunk.Service)
  */
 @Override
 protected Socket createSocket(Service service) throws IOException {
  Index indexObject = null;
  Receiver receiver = null;
  Socket socket = null;

  if (indexName != null) {
   indexObject = service.getIndexes().get(indexName);
   Assert.notNull(indexObject,String.format("cannot find index [%s]",indexName));
   socket = indexObject.attach(args);
   
   
  } else {
   receiver = service.getReceiver();
   socket = receiver.attach(args);
  }
  if (logger.isDebugEnabled()) {
   logger.debug(String.format("created a socket on %s", socket.getRemoteSocketAddress()));
  }
  return socket;
 }
 

}