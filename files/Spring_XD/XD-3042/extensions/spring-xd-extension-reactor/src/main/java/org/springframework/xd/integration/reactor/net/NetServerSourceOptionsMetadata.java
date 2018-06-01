/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor.net;

import reactor.core.Environment;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Provides metadata about the configuration options of a {@link reactor.net.NetServer} in Spring XD.
 * 
 * @author Jon Brisbin
 */
public class NetServerSourceOptionsMetadata {

 private String transport = "tcp";

 private String dispatcher = Environment.RING_BUFFER;

 private String host = "0.0.0.0";

 private int port = 3000;

 private String framing = "linefeed";

 private int lengthFieldLength = 4;

 private String codec = "string";

 public String getTransport() {
  return transport;
 }

 @ModuleOption("whether to use TCP or UDP as a transport")
 public void setTransport(String transport) {
  this.transport = transport;
 }

 public String getDispatcher() {
  return dispatcher;
 }

 @ModuleOption("type of Reactor Dispatcher to use")
 public void setDispatcher(String dispatcher) {
  this.dispatcher = dispatcher;
 }

 public String getHost() {
  return host;
 }

 @ModuleOption("host to bind the server to")
 public void setHost(String host) {
  this.host = host;
 }

 public int getPort() {
  return port;
 }

 @ModuleOption("port to bind the server to")
 public void setPort(int port) {
  this.port = port;
 }

 public String getFraming() {
  return framing;
 }

 @ModuleOption("method of framing the data")
 public void setFraming(String framing) {
  this.framing = framing;
 }

 public int getLengthFieldLength() {
  return lengthFieldLength;
 }

 @ModuleOption("byte precision of the number used in the length field")
 public void setLengthFieldLength(int lengthFieldLength) {
  this.lengthFieldLength = lengthFieldLength;
 }

 public String getCodec() {
  return codec;
 }

 @ModuleOption("codec used to transcode data")
 public void setCodec(String codec) {
  this.codec = codec;
 }

}