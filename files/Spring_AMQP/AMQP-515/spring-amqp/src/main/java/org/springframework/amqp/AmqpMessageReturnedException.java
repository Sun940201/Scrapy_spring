/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.amqp;

import org.springframework.amqp.core.Message;

/**
 * Exception thrown in an RPC scenario if the request message cannot be delivered when
 * the mandatory flag is set.
 *
 * @author Gary Russell
 * @since 1.5
 *
 */
public class AmqpMessageReturnedException extends AmqpException {

 private static final long serialVersionUID = 1866579721126554167L;

 private final Message returnedMessage;

 private final int replyCode;

 private final String replyText;

 private final String exchange;

 private final String routingKey;

 public AmqpMessageReturnedException(String message, Message returnedMessage, int replyCode, String replyText,
   String exchange, String routingKey) {
  super(message);
  this.returnedMessage = returnedMessage;
  this.replyCode = replyCode;
  this.replyText = replyText;
  this.exchange = exchange;
  this.routingKey = routingKey;
 }

 public Message getReturnedMessage() {
  return returnedMessage;
 }

 public int getReplyCode() {
  return replyCode;
 }

 public String getReplyText() {
  return replyText;
 }

 public String getExchange() {
  return exchange;
 }

 public String getRoutingKey() {
  return routingKey;
 }

 @Override
 public String toString() {
  return "AmqpMessageReturnedException: "
    + getMessage()
    + "[returnedMessage=" + returnedMessage + ", replyCode=" + replyCode
    + ", replyText=" + replyText + ", exchange=" + exchange + ", routingKey=" + routingKey + "]";
 }

}