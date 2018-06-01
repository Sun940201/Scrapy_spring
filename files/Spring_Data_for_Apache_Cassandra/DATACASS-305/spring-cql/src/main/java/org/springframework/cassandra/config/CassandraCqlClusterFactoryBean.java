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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.cassandra.core.cql.generator.CreateKeyspaceCqlGenerator;
import org.springframework.cassandra.core.cql.generator.DropKeyspaceCqlGenerator;
import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification;
import org.springframework.cassandra.core.keyspace.DropKeyspaceSpecification;
import org.springframework.cassandra.core.keyspace.KeyspaceActionSpecification;
import org.springframework.cassandra.core.util.CollectionUtils;
import org.springframework.cassandra.support.CassandraExceptionTranslator;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.LatencyTracker;
import com.datastax.driver.core.NettyOptions;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

/**
 * Convenient {@link org.springframework.beans.factory.FactoryBean} for configuring a Cassandra {@link Cluster}.
 *
 * @author Alex Shvid
 * @author Matthew T. Adams
 * @author David Webb
 * @author Kirk Clemens
 * @author Jorge Davison
 * @author John Blum
 * @author Mark Paluch
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.FactoryBean
 * @see com.datastax.driver.core.Cluster
 */
public class CassandraCqlClusterFactoryBean
  implements FactoryBean<Cluster>, InitializingBean, DisposableBean, PersistenceExceptionTranslator {

 public static final String DEFAULT_CONTACT_POINTS = "localhost";
 public static final boolean DEFAULT_METRICS_ENABLED = true;
 public static final boolean DEFAULT_JMX_REPORTING_ENABLED = true;
 public static final boolean DEFAULT_SSL_ENABLED = false;
 public static final int DEFAULT_PORT = 9042;

 protected static final Logger log = LoggerFactory.getLogger(CassandraCqlClusterFactoryBean.class);

 private Cluster cluster;

 /*
  * Attributes needed for cluster builder
  */
 private String contactPoints = DEFAULT_CONTACT_POINTS;
 private int port = CassandraCqlClusterFactoryBean.DEFAULT_PORT;

 // Protocol options
 private CompressionType compressionType;
 private SSLOptions sslOptions;
 private boolean sslEnabled = DEFAULT_SSL_ENABLED;
 private AuthProvider authProvider;
 private String username;
 private String password;
 private NettyOptions nettyOptions;
 private ProtocolVersion protocolVersion;

 // Policies
 private LoadBalancingPolicy loadBalancingPolicy;
 private ReconnectionPolicy reconnectionPolicy;
 private RetryPolicy retryPolicy;

 private PoolingOptions poolingOptions;
 private QueryOptions queryOptions;
 private SocketOptions socketOptions;

 private boolean metricsEnabled = DEFAULT_METRICS_ENABLED;
 private boolean jmxReportingEnabled = DEFAULT_JMX_REPORTING_ENABLED;

 private Host.StateListener hostStateListener;
 private LatencyTracker latencyTracker;

 // Startup and shutdown actions
 private Set<KeyspaceActionSpecification<?>> keyspaceSpecifications = new HashSet<KeyspaceActionSpecification<?>>();
 private List<CreateKeyspaceSpecification> keyspaceCreations = new ArrayList<CreateKeyspaceSpecification>();
 private List<DropKeyspaceSpecification> keyspaceDrops = new ArrayList<DropKeyspaceSpecification>();
 private List<String> startupScripts = new ArrayList<String>();
 private List<String> shutdownScripts = new ArrayList<String>();

 private final PersistenceExceptionTranslator exceptionTranslator = new CassandraExceptionTranslator();

 /* (non-Javadoc)
  * @see org.springframework.beans.factory.FactoryBean#getObject()
  */
 @Override
 public Cluster getObject() {
  return cluster;
 }

 /* (non-Javadoc)
  * @see org.springframework.beans.factory.FactoryBean#getObjectType()
  */
 @Override
 public Class<? extends Cluster> getObjectType() {
  return Cluster.class;
 }

 /* (non-Javadoc)
  * @see org.springframework.beans.factory.FactoryBean#isSingleton()
  */
 @Override
 public boolean isSingleton() {
  return true;
 }

 /* (non-Javadoc)
  * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(java.lang.RuntimeException)
  */
 @Override
 public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
  return exceptionTranslator.translateExceptionIfPossible(ex);
 }

 /* (non-Javadoc)
  * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
  */
 @Override
 public void afterPropertiesSet() throws Exception {

  if (!StringUtils.hasText(contactPoints)) {
   throw new IllegalArgumentException("at least one server is required");
  }

  Cluster.Builder builder = Cluster.builder();

  builder.addContactPoints(StringUtils.commaDelimitedListToStringArray(contactPoints)).withPort(port);

  if (compressionType != null) {
   builder.withCompression(convertCompressionType(compressionType));
  }

  if (poolingOptions != null) {
   builder.withPoolingOptions(poolingOptions);
  }

  if (socketOptions != null) {
   builder.withSocketOptions(socketOptions);
  }

  if (queryOptions != null) {
   builder.withQueryOptions(queryOptions);
  }

  if (authProvider != null) {
   builder.withAuthProvider(authProvider);
  } else if (username != null) {
   builder.withCredentials(username, password);
  }

  if (nettyOptions != null) {
   builder.withNettyOptions(nettyOptions);
  }

  if (loadBalancingPolicy != null) {
   builder.withLoadBalancingPolicy(loadBalancingPolicy);
  }

  if (reconnectionPolicy != null) {
   builder.withReconnectionPolicy(reconnectionPolicy);
  }

  if (retryPolicy != null) {
   builder.withRetryPolicy(retryPolicy);
  }

  if (!metricsEnabled) {
   builder.withoutMetrics();
  }

  if (!jmxReportingEnabled) {
   builder.withoutJMXReporting();
  }

  if (sslEnabled) {
   if (sslOptions == null) {
    builder.withSSL();
   } else {
    builder.withSSL(sslOptions);
   }
  }

  if (protocolVersion != null) {
   builder.withProtocolVersion(protocolVersion);
  }

  cluster = builder.build();

  if (hostStateListener != null) {
   cluster.register(hostStateListener);
  }

  if (latencyTracker != null) {
   cluster.register(latencyTracker);
  }

  generateSpecificationsFromFactoryBeans();

  executeSpecsAndScripts(keyspaceCreations, startupScripts);
 }

 /* (non-Javadoc)
  * @see org.springframework.beans.factory.DisposableBean#destroy()
  */
 @Override
 public void destroy() throws Exception {

  executeSpecsAndScripts(keyspaceDrops, shutdownScripts);
  cluster.close();
 }

 /**
  * Examines the contents of all the KeyspaceSpecificationFactoryBeans and generates the proper KeyspaceSpecification
  * from them.
  */
 private void generateSpecificationsFromFactoryBeans() {

  for (KeyspaceActionSpecification<?> spec : keyspaceSpecifications) {

   if (spec instanceof CreateKeyspaceSpecification) {
    keyspaceCreations.add((CreateKeyspaceSpecification) spec);
   }
   if (spec instanceof DropKeyspaceSpecification) {
    keyspaceDrops.add((DropKeyspaceSpecification) spec);
   }
  }
 }

 protected void executeSpecsAndScripts(@SuppressWarnings("rawtypes") List specs, List<String> scripts) {

  Session system = null;

  try {
   if (!CollectionUtils.isEmpty(specs)) {
    system = cluster.connect();

    CqlTemplate template = new CqlTemplate(system);

    for (Object spec : specs) {
     String cql = (spec instanceof CreateKeyspaceSpecification)
       ? new CreateKeyspaceCqlGenerator((CreateKeyspaceSpecification) spec).toCql()
       : new DropKeyspaceCqlGenerator((DropKeyspaceSpecification) spec).toCql();

     if (log.isDebugEnabled()) {
      log.debug("executing raw CQL [{}]", cql);
     }

     template.execute(cql);
    }
   }

   if (!CollectionUtils.isEmpty(scripts)) {
    system = (system != null ? system : cluster.connect());

    CqlTemplate template = new CqlTemplate(system);

    for (String script : scripts) {
     if (log.isDebugEnabled()) {
      log.debug("executing raw CQL [{}]", script);
     }

     template.execute(script);
    }
   }
  } finally {
   if (system != null) {
    system.close();
   }
  }
 }

 /**
  * Set a comma-delimited string of the contact points (hosts) to connect to. Default is {@code localhost}, see
  * {@link #DEFAULT_CONTACT_POINTS}.
  */
 public void setContactPoints(String contactPoints) {
  this.contactPoints = contactPoints;
 }

 /**
  * Set the port for the contact points. Default is {@code 9042}, see {@link #DEFAULT_PORT}.
  */
 public void setPort(int port) {
  this.port = port;
 }

 /**
  * Set the {@link CompressionType}. Default is uncompressed.
  */
 public void setCompressionType(CompressionType compressionType) {
  this.compressionType = compressionType;
 }

 /**
  * Set the {@link PoolingOptions} to configure the connection pooling behavior.
  */
 public void setPoolingOptions(PoolingOptions poolingOptions) {
  this.poolingOptions = poolingOptions;
 }

 /**
  * Set the {@link ProtocolVersion}.
  *
  * @since 1.4
  */
 public void setProtocolVersion(ProtocolVersion protocolVersion) {
  this.protocolVersion = protocolVersion;
 }

 /**
  * Set the {@link SocketOptions} containing low-level socket options.
  */
 public void setSocketOptions(SocketOptions socketOptions) {
  this.socketOptions = socketOptions;
 }

 /**
  * Set the {@link QueryOptions} to tune to defaults for individual queries.
  */
 public void setQueryOptions(QueryOptions queryOptions) {
  this.queryOptions = queryOptions;
 }

 /**
  * Set the {@link AuthProvider}. Default is unauthenticated.
  */
 public void setAuthProvider(AuthProvider authProvider) {
  this.authProvider = authProvider;
 }

 /**
  * Set the {@link NettyOptions} used by a client to customize the driver's underlying Netty layer.
  *
  * @param nettyOptions
  * @since 1.5
  */
 public void setNettyOptions(NettyOptions nettyOptions) {
  this.nettyOptions = nettyOptions;
 }

 /**
  * Set the {@link LoadBalancingPolicy} that decides which Cassandra hosts to contact for each new query.
  */
 public void setLoadBalancingPolicy(LoadBalancingPolicy loadBalancingPolicy) {
  this.loadBalancingPolicy = loadBalancingPolicy;
 }

 /**
  * Set the {@link ReconnectionPolicy} that decides how often the reconnection to a dead node is attempted.
  */
 public void setReconnectionPolicy(ReconnectionPolicy reconnectionPolicy) {
  this.reconnectionPolicy = reconnectionPolicy;
 }

 /**
  * Set the {@link RetryPolicy} that defines a default behavior to adopt when a request fails.
  */
 public void setRetryPolicy(RetryPolicy retryPolicy) {
  this.retryPolicy = retryPolicy;
 }

 /**
  * Set whether metrics are enabled. Default is {@literal true}, see {@link #DEFAULT_METRICS_ENABLED}.
  */
 public void setMetricsEnabled(boolean metricsEnabled) {
  this.metricsEnabled = metricsEnabled;
 }

 /**
  * Set a {@link List} of {@link CreateKeyspaceSpecification create keyspace specifications} that are executed when
  * this factory is {@link #afterPropertiesSet() initialized}. {@link CreateKeyspaceSpecification Create keyspace
  * specifications} are executed on a system session with no keyspace set, before executing
  * {@link #setStartupScripts(List)}.
  */
 public void setKeyspaceCreations(List<CreateKeyspaceSpecification> specifications) {
  this.keyspaceCreations = specifications;
 }

 /**
  * Return a {@link List} of {@link CreateKeyspaceSpecification create keyspace specifications}.
  */
 public List<CreateKeyspaceSpecification> getKeyspaceCreations() {
  return keyspaceCreations;
 }

 /**
  * Set a {@link List} of {@link DropKeyspaceSpecification drop keyspace specifications} that are executed when this
  * factory is {@link #destroy() destroyed}. {@link DropKeyspaceSpecification Drop keyspace specifications} are
  * executed on a system session with no keyspace set, before executing {@link #setShutdownScripts(List)}.
  */
 public void setKeyspaceDrops(List<DropKeyspaceSpecification> specifications) {
  this.keyspaceDrops = specifications;
 }

 /**
  * Reurn the {@link List} of {@link DropKeyspaceSpecification drop keyspace specifications}.
  */
 public List<DropKeyspaceSpecification> getKeyspaceDrops() {
  return keyspaceDrops;
 }

 /**
  * Set a {@link List} of raw {@link String CQL statements} that are executed when this factory is
  * {@link #afterPropertiesSet() initialized}. Scripts are executed on a system session with no keyspace set, after
  * executing {@link #setKeyspaceCreations(List)}.
  */
 public void setStartupScripts(List<String> scripts) {
  this.startupScripts = scripts;
 }

 public List<String> getStartupScripts() {
  return startupScripts;
 }

 /**
  * Set a {@link List} of raw {@link String CQL statements} that are executed when this factory is {@link #destroy()
  * destroyed}. {@link DropKeyspaceSpecification Drop keyspace specifications} are executed on a system session with no
  * keyspace set, after executing {@link #setKeyspaceDrops(List)}.
  */
 public void setShutdownScripts(List<String> scripts) {
  this.shutdownScripts = scripts;
 }

 public List<String> getShutdownScripts() {
  return shutdownScripts;
 }

 /**
  * @return Returns the keyspaceSpecifications.
  */
 public Set<KeyspaceActionSpecification<?>> getKeyspaceSpecifications() {
  return keyspaceSpecifications;
 }

 /**
  * @param keyspaceSpecifications The keyspaceSpecifications to set.
  */
 public void setKeyspaceSpecifications(Set<KeyspaceActionSpecification<?>> keyspaceSpecifications) {
  this.keyspaceSpecifications = keyspaceSpecifications;
 }

 /**
  * Set the username to use with {@link com.datastax.driver.core.PlainTextAuthProvider}.
  *
  * @param username The username to set.
  */
 public void setUsername(String username) {
  this.username = username;
 }

 /**
  * Set the username to use with {@link com.datastax.driver.core.PlainTextAuthProvider}.
  *
  * @param password The password to set.
  */
 public void setPassword(String password) {
  this.password = password;
 }

 /**
  * Set whether to use JMX reporting. Default is {@literal false}, see {@link #DEFAULT_JMX_REPORTING_ENABLED}.
  *
  * @param jmxReportingEnabled The jmxReportingEnabled to set.
  */
 public void setJmxReportingEnabled(boolean jmxReportingEnabled) {
  this.jmxReportingEnabled = jmxReportingEnabled;
 }

 /**
  * Set whether to use SSL. Default is plain, see {@link #DEFAULT_SSL_ENABLED}.
  *
  * @param sslEnabled The sslEnabled to set.
  */
 public void setSslEnabled(boolean sslEnabled) {
  this.sslEnabled = sslEnabled;
 }

 /**
  * @param sslOptions The sslOptions to set.
  */
 public void setSslOptions(SSLOptions sslOptions) {
  this.sslOptions = sslOptions;
 }

 /**
  * @param hostStateListener The hostStateListener to set.
  */
 public void setHostStateListener(Host.StateListener hostStateListener) {
  this.hostStateListener = hostStateListener;
 }

 /**
  * @param latencyTracker The latencyTracker to set.
  */
 public void setLatencyTracker(LatencyTracker latencyTracker) {
  this.latencyTracker = latencyTracker;
 }

 private static Compression convertCompressionType(CompressionType type) {
  switch (type) {
   case NONE:
    return Compression.NONE;
   case SNAPPY:
    return Compression.SNAPPY;
  }

  throw new IllegalArgumentException("unknown compression type " + type);
 }
}