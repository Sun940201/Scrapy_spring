/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.cassandra.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;

/**
 * Pooling Options Factory Bean.
 *
 * @author Matthew T. Adams
 * @author David Webb
 * @author Mark Paluch
 */
public class PoolingOptionsFactoryBean implements FactoryBean<PoolingOptions>, InitializingBean, DisposableBean {

 private Integer localMinSimultaneousRequests;
 private Integer localMaxSimultaneousRequests;
 private Integer localCoreConnections;
 private Integer localMaxConnections;
 private Integer remoteMinSimultaneousRequests;
 private Integer remoteMaxSimultaneousRequests;
 private Integer remoteCoreConnections;
 private Integer remoteMaxConnections;

 PoolingOptions poolingOptions;

 @Override
 public void destroy() throws Exception {
  localMinSimultaneousRequests = null;
  localMaxSimultaneousRequests = null;
  localCoreConnections = null;
  localMaxConnections = null;
  remoteMinSimultaneousRequests = null;
  remoteMaxSimultaneousRequests = null;
  remoteCoreConnections = null;
  remoteMaxConnections = null;
 }

 @Override
 public void afterPropertiesSet() throws Exception {

  poolingOptions = new PoolingOptions();

  if (localMaxConnections != null) {
   poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, localMaxConnections);
  }

  if (localCoreConnections != null) {
   poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, localCoreConnections);
  }

  if (localMinSimultaneousRequests != null) {
   /*
    * If the new min is greater than the current Max, set the current max to the new min first.
    * This is enforced by the DSE Driver so you cannot set a new min/max together if either one falls outside of the default 25-100 range.
    */
   int currentMax = poolingOptions.getNewConnectionThreshold(HostDistance.LOCAL);
   if (currentMax < localMinSimultaneousRequests) {
    poolingOptions.setNewConnectionThreshold(HostDistance.LOCAL,
      localMinSimultaneousRequests);
   }
  }

  if (localMaxSimultaneousRequests != null) {
   poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, localMaxSimultaneousRequests);
  }

  if (remoteMaxConnections != null) {
   poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, remoteMaxConnections);
  }

  if (remoteCoreConnections != null) {
   poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, remoteCoreConnections);
  }

  if (remoteMinSimultaneousRequests != null) {
   /*
    * If the new min is greater than the current Max, set the current max to the new min first.
    * This is enforced by the DSE Driver so you cannot set a new min/max together if either one falls outside of the default 25-100 range.
    */
   int currentMax = poolingOptions.getNewConnectionThreshold(HostDistance.REMOTE);
   if (currentMax < remoteMinSimultaneousRequests) {
    poolingOptions.setNewConnectionThreshold(HostDistance.REMOTE,
      remoteMinSimultaneousRequests);
   }
  }

  if (remoteMaxSimultaneousRequests != null) {
   poolingOptions.setMaxRequestsPerConnection(HostDistance.REMOTE,
     remoteMaxSimultaneousRequests);
  }

 }

 @Override
 public PoolingOptions getObject() throws Exception {
  return poolingOptions;
 }

 @Override
 public Class<?> getObjectType() {
  return PoolingOptions.class;
 }

 @Override
 public boolean isSingleton() {
  return true;
 }

 /**
  * @return Returns the localMinSimultaneousRequests.
  */
 public Integer getLocalMinSimultaneousRequests() {
  return localMinSimultaneousRequests;
 }

 /**
  * @param localMinSimultaneousRequests The localMinSimultaneousRequests to set.
  */
 public void setLocalMinSimultaneousRequests(Integer localMinSimultaneousRequests) {
  this.localMinSimultaneousRequests = localMinSimultaneousRequests;
 }

 /**
  * @return Returns the localMaxSimultaneousRequests.
  */
 public Integer getLocalMaxSimultaneousRequests() {
  return localMaxSimultaneousRequests;
 }

 /**
  * @param localMaxSimultaneousRequests The localMaxSimultaneousRequests to set.
  */
 public void setLocalMaxSimultaneousRequests(Integer localMaxSimultaneousRequests) {
  this.localMaxSimultaneousRequests = localMaxSimultaneousRequests;
 }

 /**
  * @return Returns the localCoreConnections.
  */
 public Integer getLocalCoreConnections() {
  return localCoreConnections;
 }

 /**
  * @param localCoreConnections The localCoreConnections to set.
  */
 public void setLocalCoreConnections(Integer localCoreConnections) {
  this.localCoreConnections = localCoreConnections;
 }

 /**
  * @return Returns the localMaxConnections.
  */
 public Integer getLocalMaxConnections() {
  return localMaxConnections;
 }

 /**
  * @param localMaxConnections The localMaxConnections to set.
  */
 public void setLocalMaxConnections(Integer localMaxConnections) {
  this.localMaxConnections = localMaxConnections;
 }

 /**
  * @return Returns the remoteMinSimultaneousRequests.
  */
 public Integer getRemoteMinSimultaneousRequests() {
  return remoteMinSimultaneousRequests;
 }

 /**
  * @param remoteMinSimultaneousRequests The remoteMinSimultaneousRequests to set.
  */
 public void setRemoteMinSimultaneousRequests(Integer remoteMinSimultaneousRequests) {
  this.remoteMinSimultaneousRequests = remoteMinSimultaneousRequests;
 }

 /**
  * @return Returns the remoteMaxSimultaneousRequests.
  */
 public Integer getRemoteMaxSimultaneousRequests() {
  return remoteMaxSimultaneousRequests;
 }

 /**
  * @param remoteMaxSimultaneousRequests The remoteMaxSimultaneousRequests to set.
  */
 public void setRemoteMaxSimultaneousRequests(Integer remoteMaxSimultaneousRequests) {
  this.remoteMaxSimultaneousRequests = remoteMaxSimultaneousRequests;
 }

 /**
  * @return Returns the remoteCoreConnections.
  */
 public Integer getRemoteCoreConnections() {
  return remoteCoreConnections;
 }

 /**
  * @param remoteCoreConnections The remoteCoreConnections to set.
  */
 public void setRemoteCoreConnections(Integer remoteCoreConnections) {
  this.remoteCoreConnections = remoteCoreConnections;
 }

 /**
  * @return Returns the remoteMaxConnections.
  */
 public Integer getRemoteMaxConnections() {
  return remoteMaxConnections;
 }

 /**
  * @param remoteMaxConnections The remoteMaxConnections to set.
  */
 public void setRemoteMaxConnections(Integer remoteMaxConnections) {
  this.remoteMaxConnections = remoteMaxConnections;
 }

}