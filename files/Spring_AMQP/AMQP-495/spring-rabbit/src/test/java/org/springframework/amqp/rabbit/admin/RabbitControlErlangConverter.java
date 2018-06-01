/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.amqp.rabbit.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.erlang.core.Application;
import org.springframework.erlang.core.Node;
import org.springframework.erlang.support.converter.ErlangConversionException;
import org.springframework.erlang.support.converter.ErlangConverter;
import org.springframework.erlang.support.converter.SimpleErlangConverter;
import org.springframework.util.Assert;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

/***
 * Converter that understands the responses from the rabbit control module and related functionality.
 *
 * @author Mark Pollack
 * @author Mark Fisher
 * @author Helena Edelson
 */
public class RabbitControlErlangConverter extends SimpleErlangConverter implements ErlangConverter {

 protected final Log logger = LogFactory.getLog(getClass());

 private final Map<String, ErlangConverter> converterMap = new HashMap<String, ErlangConverter>();

 private final Map<String, String> moduleAdapter;

 public RabbitControlErlangConverter(Map<String, String> moduleAdapter) {
  this.moduleAdapter = moduleAdapter;
  initializeConverterMap();
 }

 public Object fromErlangRpc(String module, String function, OtpErlangObject erlangObject)
   throws ErlangConversionException {
  ErlangConverter converter = getConverter(module, function);
  if (converter != null) {
   return converter.fromErlang(erlangObject);
  } else {
   return super.fromErlangRpc(module, function, erlangObject);
  }
 }

 protected ErlangConverter getConverter(String module, String function) {
  return converterMap.get(generateKey(module, function));
 }

 protected void initializeConverterMap() {
  registerConverter("rabbit_auth_backend_internal", "list_users", new ListUsersConverter());
  registerConverter("rabbit", "status", new StatusConverter());
  registerConverter("rabbit_amqqueue", "info_all", new QueueInfoAllConverter());
 }

 protected void registerConverter(String module, String function, ErlangConverter listUsersConverter) {
  String key = generateKey(module, function);
  if (moduleAdapter.containsKey(key)) {
   String adapter = moduleAdapter.get(key);
   String[] values = adapter.split("%");
   Assert.state(values.length == 2,
     "The module adapter should be a map from 'module%function' to 'module%function'. "
       + "This one contained [" + adapter + "] which cannot be parsed to a module, function pair.");
   module = values[0];
   function = values[1];
  }
  converterMap.put(generateKey(module, function), listUsersConverter);
 }

 protected String generateKey(String module, String function) {
  return module + "%" + function;
 }

 public class ListUsersConverter extends SimpleErlangConverter {

  public Object fromErlang(OtpErlangObject erlangObject) throws ErlangConversionException {

   List<String> users = new ArrayList<String>();
   if (erlangObject instanceof OtpErlangList) {
    OtpErlangList erlangList = (OtpErlangList) erlangObject;
    for (OtpErlangObject obj : erlangList) {
     String value = extractString(obj);
     if (value != null) {
      users.add(value);
     }
    }
   }
   return users;
  }

  private String extractString(OtpErlangObject obj) {

   if (obj instanceof OtpErlangBinary) {
    OtpErlangBinary binary = (OtpErlangBinary) obj;
    return new String(binary.binaryValue());
   } else if (obj instanceof OtpErlangTuple) {
    OtpErlangTuple tuple = (OtpErlangTuple) obj;
    return extractString(tuple.elementAt(0));
   }
   return null;
  }
 }

 public class StatusConverter extends SimpleErlangConverter {

  public Object fromErlang(OtpErlangObject erlangObject) throws ErlangConversionException {

   List<Application> applications = new ArrayList<Application>();
   List<Node> nodes = new ArrayList<Node>();
   List<Node> runningNodes = new ArrayList<Node>();
   if (erlangObject instanceof OtpErlangList) {
    OtpErlangList erlangList = (OtpErlangList) erlangObject;

    OtpErlangTuple runningAppTuple = (OtpErlangTuple) erlangList.elementAt(0);
    OtpErlangList appList = (OtpErlangList) runningAppTuple.elementAt(1);
    extractApplications(applications, appList);

    OtpErlangTuple nodesTuple = (OtpErlangTuple) erlangList.elementAt(1);
    OtpErlangList nodesList = (OtpErlangList) nodesTuple.elementAt(1);
    extractNodes(nodes, nodesList);

    OtpErlangTuple runningNodesTuple = (OtpErlangTuple) erlangList.elementAt(2);
    nodesList = (OtpErlangList) runningNodesTuple.elementAt(1);
    extractNodes(runningNodes, nodesList);

    /*
     * for (OtpErlangObject obj : erlangList) { if (obj instanceof OtpErlangBinary) { OtpErlangBinary binary
     * = (OtpErlangBinary) obj; users.add(new String(binary.binaryValue())); } }
     */
   }

   return new RabbitStatus(applications, nodes, runningNodes);
  }

  private void extractNodes(List<Node> nodes, OtpErlangList nodesList) {
   for (OtpErlangObject erlangNodeName : nodesList) {
    String nodeName = erlangNodeName.toString();
    nodes.add(new Node(nodeName));
   }
  }

  private void extractApplications(List<Application> applications, OtpErlangList appList) {
   for (OtpErlangObject appDescription : appList) {
    OtpErlangTuple appDescriptionTuple = (OtpErlangTuple) appDescription;
    String name = appDescriptionTuple.elementAt(0).toString();
    String description = appDescriptionTuple.elementAt(1).toString();
    String version = appDescriptionTuple.elementAt(2).toString();
    applications.add(new Application(name, description, version));
   }
  }
 }

 public enum QueueInfoField {
  transactions, acks_uncommitted, consumers, pid, durable, messages, memory, auto_delete, messages_ready, arguments, name, messages_unacknowledged, messages_uncommitted, NOVALUE;

  public static QueueInfoField toQueueInfoField(String str) {
   try {
    return valueOf(str);
   } catch (Exception ex) {
    return NOVALUE;
   }
  }
 }

 public class QueueInfoAllConverter extends SimpleErlangConverter {

  @Override
  public Object fromErlang(OtpErlangObject erlangObject) throws ErlangConversionException {
   List<QueueInfo> queueInfoList = new ArrayList<QueueInfo>();
   if (erlangObject instanceof OtpErlangList) {
    OtpErlangList erlangList = (OtpErlangList) erlangObject;
    for (OtpErlangObject element : erlangList.elements()) {
     QueueInfo queueInfo = new QueueInfo();
     OtpErlangList itemList = (OtpErlangList) element;
     for (OtpErlangObject item : itemList.elements()) {
      OtpErlangTuple tuple = (OtpErlangTuple) item;
      if (tuple.arity() == 2) {
       String key = tuple.elementAt(0).toString();
       OtpErlangObject value = tuple.elementAt(1);
       switch (QueueInfoField.toQueueInfoField(key)) {
       case name:
        queueInfo.setName(extractNameValueFromTuple((OtpErlangTuple) value));
        break;
       case transactions:
        queueInfo.setTransactions(extractLong(value));
        break;
       case acks_uncommitted:
        queueInfo.setAcksUncommitted(extractLong(value));
        break;
       case consumers:
        queueInfo.setConsumers(extractLong(value));
        break;
       case pid:
        queueInfo.setPid(extractPid(value));
        break;
       case durable:
        queueInfo.setDurable(extractAtomBoolean(value));
        break;
       case messages:
        queueInfo.setMessages(extractLong(value));
        break;
       case memory:
        queueInfo.setMemory(extractLong(value));
        break;
       case auto_delete:
        queueInfo.setAutoDelete(extractAtomBoolean(value));
        break;
       case messages_ready:
        queueInfo.setMessagesReady(extractLong(value));
        break;
       case arguments:
        OtpErlangList list = (OtpErlangList) value;
        if (list != null) {
         String[] args = new String[list.arity()];
         for (int i = 0; i < list.arity(); i++) {
          OtpErlangObject obj = list.elementAt(i);
          args[i] = obj.toString();
         }
         queueInfo.setArguments(args);
        }
        break;
       case messages_unacknowledged:
        queueInfo.setMessagesUnacknowledged(extractLong(value));
        break;
       case messages_uncommitted:
        queueInfo.setMessageUncommitted(extractLong(value));
        break;
       default:
        break;
       }
      }
     }
     queueInfoList.add(queueInfo);
    }
   }
   return queueInfoList;
  }

  private boolean extractAtomBoolean(OtpErlangObject value) {
   return ((OtpErlangAtom) value).booleanValue();
  }

  private String extractNameValueFromTuple(OtpErlangTuple value) {
   Object nameElement = value.elementAt(3);
   return new String(((OtpErlangBinary) nameElement).binaryValue());
  }
 }

}