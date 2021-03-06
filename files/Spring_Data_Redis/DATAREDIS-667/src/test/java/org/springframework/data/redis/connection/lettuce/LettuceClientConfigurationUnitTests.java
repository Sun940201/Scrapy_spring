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
package org.springframework.data.redis.connection.lettuce;

import static org.assertj.core.api.Assertions.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;

import org.junit.Test;

/**
 * Unit tests for {@link LettuceClientConfiguration}.
 *
 * @author Mark Paluch
 */
public class LettuceClientConfigurationUnitTests {

 @Test // DATAREDIS-574
 public void shouldCreateEmptyConfiguration() {

  LettuceClientConfiguration configuration = LettuceClientConfiguration.defaultConfiguration();

  assertThat(configuration.isUseSsl()).isFalse();
  assertThat(configuration.isVerifyPeer()).isTrue();
  assertThat(configuration.isStartTls()).isFalse();
  assertThat(configuration.getClientOptions()).isEmpty();
  assertThat(configuration.getClientResources()).isEmpty();
  assertThat(configuration.getCommandTimeout()).isEqualTo(Duration.ofSeconds(60));
  assertThat(configuration.getShutdownTimeout()).isEqualTo(Duration.ofMillis(100));
 }

 @Test // DATAREDIS-574
 public void shouldConfigureAllProperties() {

  ClientOptions clientOptions = ClientOptions.create();
  ClientResources sharedClientResources = LettuceTestClientResources.getSharedClientResources();

  LettuceClientConfiguration configuration = LettuceClientConfiguration.builder() //
    .useSsl() //
    .disablePeerVerification() //
    .startTls().and() //
    .clientOptions(clientOptions) //
    .clientResources(sharedClientResources) //
    .commandTimeout(Duration.ofMinutes(5)) //
    .shutdownTimeout(Duration.ofHours(2)) //
    .build();

  assertThat(configuration.isUseSsl()).isTrue();
  assertThat(configuration.isVerifyPeer()).isFalse();
  assertThat(configuration.isStartTls()).isTrue();
  assertThat(configuration.getClientOptions()).contains(clientOptions);
  assertThat(configuration.getClientResources()).contains(sharedClientResources);
  assertThat(configuration.getCommandTimeout()).isEqualTo(Duration.ofMinutes(5));
  assertThat(configuration.getShutdownTimeout()).isEqualTo(Duration.ofHours(2));
 }
}