/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.job.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xml.transform.StringResult;

/**
 * The root AST node for any AST parsed from a job specification.
 *
 * Andy Clement
 */
public class JobSpecification extends AstNode {

 /**
  * The DSL text that was parsed to create this JobSpec.
  */
 private String jobDefinitionText;

 /**
  * The top level JobNode within this JobSpec.
  */
 private JobNode jobNode;

 /**
  * A list of jobs that were defined inline in the DSL text that was parsed
  * to create this Ast. Computed on first reference.
  */
 private List<JobDefinition> jobDefinitions;

 /**
  * Any arguments specified at the end of the DSL, e.g. --timeout
  */
 private ArgumentNode[] globalOptions;

 public JobSpecification(String jobDefinitionText, JobNode jobNode, ArgumentNode[] globalOptions) {
  super(jobNode == null ? 0 : jobNode.getStartPos(), jobNode == null ? 0 : jobNode.getEndPos());
  this.jobDefinitionText = jobDefinitionText;
  this.jobNode = jobNode;
  this.globalOptions = globalOptions;
 }

 public Map<String, String> getGlobalOptionsMap() {
  if (globalOptions == null) {
   return Collections.<String, String> emptyMap();
  }
  Map<String, String> optionsMap = new LinkedHashMap<String, String>();
  for (ArgumentNode option : globalOptions) {
   optionsMap.put(option.getName(), option.getValue());
  }
  return optionsMap;
 }

 @Override
 public String stringify(boolean includePositionInfo) {
  StringBuilder s = new StringBuilder();
  s.append(jobNode.stringify(includePositionInfo));
  if (globalOptions != null) {
   for (ArgumentNode option : globalOptions) {
    s.append(" ");
    s.append(option.stringify(includePositionInfo));
   }
  }
  return s.toString();
 }

 public String getJobDefinitionText() {
  return jobDefinitionText;
 }

 public JobNode getJobNode() {
  return this.jobNode;
 }

 /**
  * A shortcut (avoiding traversing the tree) that returns the list
  * of all job definitions inlined somewhere in this AST. Computed
  * on demand.
  *
  * @return a list of inlined job definitions defined in this AST
  */
 public List<JobDefinition> getJobDefinitions() {
  if (jobDefinitions != null) {
   return jobDefinitions;
  }
  JobDefinitionLocator jdl = new JobDefinitionLocator();
  jdl.accept(this);
  jobDefinitions = jdl.getJobDefinitions();
  return jobDefinitions;
 }

 /**
  * A shortcut (avoiding traversing the tree) that returns the list
  * of all job references somewhere in this AST (references in
  * transitions do not count).
  *
  * @return a list of job references in this AST
  */
 public List<JobReference> getJobReferences() {
  JobReferenceLocator jrl = new JobReferenceLocator();
  jrl.accept(this);
  return jrl.getJobReferences();
 }

 /**
  * Performs validation of the AST. Where the initial parse is about
  * checking the syntactic structure, validation is about checking more
  * semantic elements:<ul>
  * <li>Do the inline job definitions refer to valid job modules?
  * <li>Do the inline job definitions supply correct arguments?
  * <li>Do the job references point to valid job definitions?
  * </ul>
  * @param jobDefinitionRepository a repository to check job definitions against
  * @throws JobSpecificationException if validation fails
  */
 public void validate(JobDefinitionRepository jobDefinitionRepository) {
  // TODO !

 }

 /**
  * @return this AST converted to a Graph form for display by Flo
  */
 public Graph toGraph() {
  GraphGeneratorVisitor ggv = new GraphGeneratorVisitor();
  ggv.accept(this);
  return ggv.getGraph();
 }

 /**
  * @param batchJobId the id that will be inserted into the XML document for the batch:job element
  * @return this AST converted to an XML form
  */
 public String toXML(String batchJobId) {
  return toXML(batchJobId, false);
 }

 /**
  * @param batchJobId the id that will be inserted into the XML document for the batch:job element
  * @param prettyPrint determine if the XML should be human readable.
  * @return this AST converted to an XML form
  */
 public String toXML(String batchJobId, boolean prettyPrint) {
  XMLGeneratorVisitor xgv = new XMLGeneratorVisitor(batchJobId, prettyPrint);
  xgv.accept(this);
  return xgv.getXmlString();
 }

 /**
  * Basic visitor that simply collects up any inlined job definitions.
  */
 static class JobDefinitionLocator extends JobSpecificationVisitor<Object> {

  List<JobDefinition> jobDefinitions = new ArrayList<JobDefinition>();

  public List<JobDefinition> getJobDefinitions() {
   return jobDefinitions;
  }

  @Override
  public Object walk(Object context, Flow sjs) {
   for (JobNode jobNode : sjs.getSeries()) {
    walk(context, jobNode);
   }
   return context;
  }

  @Override
  public Object walk(Object context, JobDefinition jd) {
   jobDefinitions.add(jd);
   return context;
  }

  @Override
  public Object walk(Object context, JobReference jr) {
   return context;
  }

  @Override
  public Object walk(Object context, Split pjs) {
   for (JobNode jobNode : pjs.getSeries()) {
    walk(context, jobNode);
   }
   return context;
  }

 }

 /**
  * Basic visitor that simply collects up any job references (*not* those named in transitions)
  */
 static class JobReferenceLocator extends JobSpecificationVisitor<Object> {

  List<JobReference> jobReferences = new ArrayList<JobReference>();

  public List<JobReference> getJobReferences() {
   return jobReferences;
  }

  @Override
  public Object walk(Object context, Flow sjs) {
   for (JobNode jobNode : sjs.getSeries()) {
    walk(context, jobNode);
   }
   return context;
  }

  @Override
  public Object walk(Object context, JobDefinition jd) {
   return context;
  }

  @Override
  public Object walk(Object context, JobReference jr) {
   jobReferences.add(jr);
   return context;
  }

  @Override
  public Object walk(Object context, Split pjs) {
   for (JobNode jobNode : pjs.getSeries()) {
    walk(context, jobNode);
   }
   return context;
  }

 }

 /**
  * Visitor that produces an XML representation of the Job specification.
  */
 static class XMLGeneratorVisitor extends JobSpecificationVisitor<Element[]> {

  /**
   * containing document that can be used for element creation
   */
  private Document doc;

  /**
   * Where to append elements created during the visit
   */
  private Element batchJobElement;

  /**
   * Should the XML output be readable or compressed onto one line.
   */
  private boolean prettyPrint;

  /**
   * A stack that tracks the element that should have new children attached to it.
   * Initially populated with batchJobElement from above.
   */
  private Stack<Element> currentElement = new Stack<>();

  /**
   * Counter for the numeric suffix to attach to generated split id attributes.
   */
  private int splitIdCounter = 1;

  private List<String> jobRunnerBeanNames = new ArrayList<>();

  // As a new flow is entered, a new map is pushed here (popped on flow exit).
  // The map holds onto a map of transition names to allocated XML IDs within that flow.
  // This ensures all references to the same transition in a flow point to the same XML ID
  // But in a different flow the same transition names will point to a different XML ID.
  private Stack<Map<String, String>> transitionNamesToElementIdsInFlow = new Stack<>();

  // Knowing all the explicit job references in the tree means when seeing a transition
  // it can be determined if it is to a node not yet visited or something that will
  // never be visited (and so the step must be created right now).
  private Map<JobReference, String> jobReferencesToElementIds = new LinkedHashMap<>();

  private String xmlString;

  private String batchJobId;

  XMLGeneratorVisitor(String batchJobId, boolean prettyPrint) {
   this.batchJobId = batchJobId;
   this.prettyPrint = prettyPrint;
  }

  @Override
  protected void accept(JobSpecification jobSpec) {
   List<JobReference> jobReferences = jobSpec.getJobReferences();
   for (JobReference jr : jobReferences) {
    // Allocate unique XML Element IDs now, makes life easier later
    jobReferencesToElementIds.put(jr, getNextStepId(jr.getName()));
   }
   super.accept(jobSpec);
  }

  public String getXmlString() {
   return xmlString;
  }

  @Override
  public Element[] preJobSpecWalk() {
   try {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    DOMImplementation domImplementation = db.getDOMImplementation();

    // Generate:
    // <beans xmlns="http://www.springframework.org/schema/beans"
    //    xmlns:batch="http://www.springframework.org/schema/batch"
    //    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
    //      http://www.springframework.org/schema/batch http://www.springframework.org/schema/batch/spring-batch.xsd">
    this.doc = domImplementation.createDocument("http://www.springframework.org/schema/beans", "beans",
      null);
    doc.createElementNS("http://www.springframework.org/schema/batch", "batch");
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:batch",
      "http://www.springframework.org/schema/batch");
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi",
      "http://www.w3.org/2001/XMLSchema-instance");
    doc.getDocumentElement().setAttributeNS(
      "http://www.w3.org/2001/XMLSchema-instance",
      "xsi:schemaLocation",
      "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd "
        +
        "http://www.springframework.org/schema/batch http://www.springframework.org/schema/batch/spring-batch.xsd");
    // Setting this 'again' to get it on the front and look more like the above.
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns",
      "http://www.springframework.org/schema/beans");

    // Generate: <bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor"/>
    Element taskExecutor = doc.createElement("bean");
    doc.getDocumentElement().appendChild(taskExecutor);
    taskExecutor.setAttribute("id", "taskExecutor");
    taskExecutor.setAttribute("class", "org.springframework.core.task.SimpleAsyncTaskExecutor");

    // Generate: <batch:job id="streamName" xmlns="http://www.springframework.org/schema/batch">
    this.batchJobElement = doc.createElement("batch:job");
    doc.getDocumentElement().appendChild(batchJobElement);
    batchJobElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns",
      "http://www.springframework.org/schema/batch");
    if (batchJobId != null) {
     batchJobElement.setAttribute("id", batchJobId);
    }
    this.currentElement.push(batchJobElement);
   }
   catch (Exception e) {
    throw new IllegalStateException("Unexpected problem building XML representation", e);
   }
   return null;
  };

  @Override
  public void postJobSpecWalk(Element[] elements, JobSpecification jobSpec) {
   Set<String> generatedBeans = new HashSet<>();
   for (String jobRunnerBeanName : jobRunnerBeanNames) {
    if (generatedBeans.contains(jobRunnerBeanName)) {
     continue;
    }
    // Producing:
    // <bean class="org.springframework.xd.dirt.batch.tasklet.JobLaunchingTasklet" id="jobRunner-bbb" scope="step">
    //        <constructor-arg ref="messageBus"/>
    //        <constructor-arg ref="jobDefinitionRepository"/>
    //        <constructor-arg ref="xdJobRepository"/>
    //        <constructor-arg value="bbb"/>
    //        <constructor-arg value="${timeout}"/>
    // </bean>
    Element bean = doc.createElement("bean");
    bean.setAttribute("scope", "step");
    bean.setAttribute("class", "org.springframework.xd.dirt.batch.tasklet.JobLaunchingTasklet");
    bean.setAttribute("id", "jobRunner-" + jobRunnerBeanName);
    addConstructorArg(bean, "ref", "messageBus");
    addConstructorArg(bean, "ref", "jobDefinitionRepository");
    addConstructorArg(bean, "ref", "xdJobRepository");
    addConstructorArg(bean, "value", jobRunnerBeanName);
    addConstructorArg(bean, "value", "${timeout}");
    this.doc.getElementsByTagName("beans").item(0).appendChild(bean);
    generatedBeans.add(jobRunnerBeanName);
   }
   try {
    // Write the content
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    javax.xml.transform.Transformer transformer;
    transformer = transformerFactory.newTransformer();
    if (prettyPrint) {
     transformer.setOutputProperty(OutputKeys.INDENT, "yes");
     transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    DOMSource source = new DOMSource(doc);
    StringResult sr = new StringResult();
    transformer.transform(source, sr);
    xmlString = sr.toString().trim();
   }
   catch (TransformerException e) {
    throw new IllegalStateException("Unexpected problem building XML representation", e);
   }
  }

  private void addConstructorArg(Element bean, String attributeName, String argName) {
   Element ctorArgElement = doc.createElement("constructor-arg");
   ctorArgElement.setAttribute(attributeName, argName);
   bean.appendChild(ctorArgElement);
  }

  @Override
  public Element[] walk(Element[] context, Flow jn) {
   boolean inSplit = currentElement.peek().getTagName().equals("split");
   // Only need the flow element if nested in a split
   if (inSplit) {
    Element flow = doc.createElement("flow");
    currentElement.peek().appendChild(flow);
    currentElement.push(flow);
   }
   Element[] result = context;
   transitionNamesToElementIdsInFlow.push(new LinkedHashMap<String, String>());
   for (JobNode j : jn.getSeries()) {
    result = walk(result, j);
   }
   Map<String, String> transitionStepsToCreate = transitionNamesToElementIdsInFlow.pop();
   for (Map.Entry<String, String> transitionStepToCreate : transitionStepsToCreate.entrySet()) {
    Element step = createStep(transitionStepToCreate.getValue(), transitionStepToCreate.getKey());
    currentElement.peek().appendChild(step);
   }
   if (inSplit) {
    currentElement.pop();
   }
   return result;
  }

  @Override
  public Element[] walk(Element[] context, JobDefinition jd) {
   // TODO this code needs some rework to match XML gen for JobReference but we don't
   // support JobDefinitions in the first version of the DSL.
   Element step = doc.createElement("step");
   step.setAttribute("id", jd.getJobName());
   Element tasklet = doc.createElement("tasklet");
   String jobRunnerId = "jobRunner-" + jd.getJobName();
   tasklet.setAttribute("ref", jobRunnerId);
   jobRunnerBeanNames.add(jd.getName());
   step.appendChild(tasklet);
   Element next = null;
   if (jd.hasTransitions()) {
    for (Transition t : jd.transitions) {
     next = doc.createElement("next");
     next.setAttribute("on", t.getStateName());
     next.setAttribute("to", t.getTargetJobName());
     step.appendChild(next);
    }
   }
   if (context != null) {
    // context is an array of earlier elements that should point to this one
    Element[] elements = context;
    for (Element element : elements) {
     next = doc.createElement("next");
     next.setAttribute("on", "*");
     next.setAttribute("to", jd.getJobName());
     element.appendChild(next);
    }
   }
   this.currentElement.peek().appendChild(step);
   return new Element[] { step };
  }

  private List<String> allocatedStepIds = new ArrayList<>();

  /**
   * Determine the next unique ID we can use for an XML element.
   */
  private String getNextStepId(String prefix) {
   if (!allocatedStepIds.contains(prefix)) {
    // Avoid number suffix for the first one
    allocatedStepIds.add(prefix);
    return prefix;
   }
   int suffix = 1;
   String proposal = null;
   do {
    proposal = new StringBuilder(prefix).append(Integer.toString(suffix++)).toString();
   }
   while (allocatedStepIds.contains(proposal));
   allocatedStepIds.add(proposal);
   return proposal;
  }


  /**
   * Visit a job reference. Rules:
   * <ul>
   * <li>The flow surrounding element for the step is created if inside a split
   * </ul>
   */
  @Override
  public Element[] walk(Element[] context, JobReference jr) {
   // Producing this kind of construct:
   // <flow">
   //   <step id="sqoop-6e44">
   //    <tasklet ref="jobRunner-6e44"/>
   //    <next on="*" to="sqoop-e07a"/>
   //    <next on="FAILED" to="kill1"/>
   //   </step>
   // </flow>

   // When a split branch only contains a single job reference, no surrounding Flow object is created,
   // so the flow block needs creating here in this case.
   boolean inSplit = currentElement.peek().getTagName().equals("split");
   if (inSplit) {
    Element flow = doc.createElement("flow");
    currentElement.peek().appendChild(flow);
    currentElement.push(flow);
   }
   String stepId = jobReferencesToElementIds.get(jr);
   Element step = createStep(stepId, jr.getName());
   currentElement.peek().appendChild(step);
   jobRunnerBeanNames.add(jr.getName());
   if (jr.hasTransitions()) {
    for (Transition t : jr.transitions) {
     String targetJob = t.getTargetJobName();
     Map<String, String> transitionNamesToElementIdsInCurrentFlow = transitionNamesToElementIdsInFlow.peek();
     if (transitionNamesToElementIdsInCurrentFlow.containsKey(targetJob)) {
      // already exists, share the ID
      targetJob = transitionNamesToElementIdsInCurrentFlow.get(targetJob);
     }
     else {
      // Is this a reference to a job that already exists elsewhere in this composed job definition?
      String id = getReferenceToExistingJob(targetJob);
      if (id == null) {
       // create an entry, this is the first reference to this target job in this flow
       id = getNextStepId(targetJob);
       transitionNamesToElementIdsInCurrentFlow.put(targetJob, id);
       if (inSplit) {
        // If a job reference is directly inside a split with no surrounding flow to create
        // the steps collected in 'existingTransitionSteps' then it needs to be done here.
        Element transitionStep = createStep(id, t.getTargetJobName());
        currentElement.peek().appendChild(transitionStep);
       }
      }
      targetJob = id;
     }
     step.appendChild(createNextElement(t.getStateName(), targetJob));
     jobRunnerBeanNames.add(t.getTargetJobName());
    }
   }
   if (context != null) {
    // context is an array of earlier elements that should be updated now to point to this one
    Element[] elements = context;
    for (Element element : elements) {
     element.appendChild(createNextElement("*", stepId));
    }
   }
   if (inSplit) {
    currentElement.pop();
   }
   return new Element[] { step };
  }

  @Override
  public Element[] walk(Element[] context, Split pjs) {
   // Producing this kind of output:
   // <split id="split1" task-executor="taskExecutor">
   //   ...
   // </split>
   Element split = doc.createElement("split");
   String splitId = "split" + (splitIdCounter++);
   split.setAttribute("task-executor", "taskExecutor");
   split.setAttribute("id", splitId);

   if (context != null) {
    // context is an array of earlier elements that should point to this one
    Element next = null;
    for (Element element : context) {
     next = doc.createElement("next");
     next.setAttribute("on", "*");
     next.setAttribute("to", splitId);
     element.appendChild(next);
    }
   }

   currentElement.peek().appendChild(split);
   currentElement.push(split);
   Element[] inputContext = new Element[] {};//context;
   Element[] result = new Element[0];
   for (JobNode jn : pjs.getSeries()) {
    transitionNamesToElementIdsInFlow.push(new LinkedHashMap<String, String>());
    Object outputContext = walk(inputContext, jn);
    transitionNamesToElementIdsInFlow.pop();
    result = merge(result, outputContext);
   }
   currentElement.pop();
   // The only element from here to connect to the 'next thing' is the split node.
   // This means only the split gets a 'next on="*"' element.
   return new Element[] { split };
  }

  private Element[] merge(Element[] input, Object additional) {
   Element[] additionalArrayData = (Element[]) additional;
   Element[] result = new Element[input.length + additionalArrayData.length];
   System.arraycopy(input, 0, result, 0, input.length);
   System.arraycopy(additionalArrayData, 0, result, input.length, additionalArrayData.length);
   return result;
  }

  private Element createStep(String stepId, String jobRunnerBeanIdSuffix) {
   Element step = doc.createElement("step");
   step.setAttribute("id", stepId);
   Element tasklet = doc.createElement("tasklet");
   tasklet.setAttribute("ref", "jobRunner-" + jobRunnerBeanIdSuffix);
   step.appendChild(tasklet);
   return step;
  }

  private Element createNextElement(String on, String to) {
   Element next = doc.createElement("next");
   next.setAttribute("on", on);
   next.setAttribute("to", to);
   return next;
  }

  private String getReferenceToExistingJob(String jobName) {
   for (Map.Entry<JobReference, String> jrEntry : jobReferencesToElementIds.entrySet()) {
    if (jrEntry.getKey().getName().equals(jobName)) {
     return jrEntry.getValue();
    }
   }
   return null;
  }

 }

 /**
  * Visitor that produces a Graph representation of the Job specification suitable
  * for display by Flo.
  */
 static class GraphGeneratorVisitor extends JobSpecificationVisitor<int[]> {

  private int id = 0;

  private Map<String, Node> createdNodes = new HashMap<>();

  static class TransitionToMap {

   int from;

   String transitionName;

   String targetJob;

   public TransitionToMap(int from, String transitionName, String targetJob) {
    super();
    this.from = from;
    this.transitionName = transitionName;
    this.targetJob = targetJob;
   }
  }

  private List<TransitionToMap> transitions = new ArrayList<>();

  private List<Node> nodes = new ArrayList<>();

  private List<Link> links = new ArrayList<>();

  private Map<String, String> properties = new LinkedHashMap<String, String>();

  public Graph getGraph() {
   Graph g = new Graph(nodes, links, properties);
   return g;
  }

  @Override
  public int[] preJobSpecWalk() {
   // Insert a START node at the beginning of the graph
   Node node = new Node(Integer.toString(id++), "START");
   nodes.add(node);
   return new int[] { 0 };
  }

  @Override
  public void postJobSpecWalk(int[] finalNodes, JobSpecification jobSpec) {
   // Deal with transitions
   for (TransitionToMap ttm : transitions) {
    int nodeInGraph = findNode(ttm.targetJob);
    if (nodeInGraph == -1) {
     // target isn't in graph yet
     int nextId = id++;
     Node n = new Node(Integer.toString(nextId), ttm.targetJob);
     nodes.add(n);
     nodeInGraph = nextId;
     finalNodes = merge(finalNodes, new int[] { nextId });
    }
    links.add(new Link(ttm.from, nodeInGraph, ttm.transitionName));
   }
   // Insert an END node at the end of the graph
   int endId = id++;
   Node n = new Node(Integer.toString(endId), "END");
   nodes.add(n);
   for (int i : finalNodes) {
    links.add(new Link(i, endId));
   }
   Map<String, String> options = jobSpec.getGlobalOptionsMap();
   if (options.size() != 0) {
    for (Map.Entry<String, String> option : options.entrySet()) {
     properties.put(option.getKey(), option.getValue());
    }
   }
  }

  private int findNode(String targetJob) {
   for (Node n : nodes) {
    if (n.name.equals(targetJob)) {
     return Integer.parseInt(n.id);
    }
   }
   return -1;
  }

  @Override
  public int[] walk(int[] context, Flow jn) {
   int[] result = context;
   for (JobNode j : jn.getSeries()) {
    result = walk(result, j);
   }
   // Only the last result is left dangling when visiting a sequence,
   // return it here.
   return result;
  }

  @Override
  public int[] walk(int[] context, Split pjs) {
   int[] inputContext = context;

   if (inputContext.length > 1) {
    // Cannot directly connect a split to a split, we need a SYNC node
    // to simulate fan-in/fan-out (inputContext.length > 1 indicates a
    // previous split).
    int nextId = id++;
    Node node = new Node(Integer.toString(nextId), "SYNC");
    nodes.add(node);
    for (int i : inputContext) {
     Link l = new Link(i, nextId);
     links.add(l);
    }
    // Now create new context that contains only the sync node output
    inputContext = new int[] { nextId };
   }

   int[] result = new int[0];
   for (JobNode jn : pjs.getSeries()) {
    Object outputContext = walk(inputContext, jn);
    result = merge(result, outputContext);
   }
   return result;
  }

  int[] merge(int[] input, Object additional) {
   int[] additionalArrayData = (int[]) additional;
   int[] result = new int[input.length + additionalArrayData.length];
   System.arraycopy(input, 0, result, 0, input.length);
   System.arraycopy(additionalArrayData, 0, result, input.length, additionalArrayData.length);
   return result;
  }

  @Override
  public int[] walk(int[] context, JobReference jr) {
   int nextId = id++;
   Map<String, String> properties = null;
   ArgumentNode[] args = jr.getArguments();
   if (args != null && args.length != 0) {
    properties = new LinkedHashMap<>();
    for (ArgumentNode arg : args) {
     properties.put(arg.getName(), arg.getValue());
    }
   }
   Node node = new Node(Integer.toString(nextId), jr.getName(), null, properties);
   nodes.add(node);
   createdNodes.put(jr.getName(), node);
   // Create links from the previous nodes to this one
   if (context != null) {
    int[] s = context;
    for (int i : s) {
     Link l = new Link(i, nextId);
     links.add(l);
    }
   }
   if (jr.hasTransitions()) {
    for (Transition t : jr.getTransitions()) {
     transitions.add(new TransitionToMap(nextId, t.getStateNameInDSLForm(), t.getTargetJobName()));
    }
   }
   return new int[] { nextId };
  }

  @Override
  public int[] walk(int[] context, JobDefinition jd) {
   int nextId = id++;
   Map<String, String> properties = null;
   ArgumentNode[] args = jd.getArguments();
   if (args != null && args.length != 0) {
    properties = new LinkedHashMap<>();
    for (ArgumentNode arg : args) {
     properties.put(arg.getName(), arg.getValue());
    }
   }
   Map<String, String> metadata = new HashMap<>();
   metadata.put(Node.METADATAKEY_JOBMODULENAME, jd.getJobModuleName());
   Node node = new Node(Integer.toString(nextId), jd.getJobName(), metadata, properties);
   nodes.add(node);
   createdNodes.put(node.name, node);
   if (context != null) {
    int[] s = context;
    for (int i : s) {
     Link l = new Link(i, nextId);
     links.add(l);
    }
   }
   if (jd.hasTransitions()) {
    for (Transition t : jd.getTransitions()) {
     transitions.add(new TransitionToMap(nextId, t.getStateNameInDSLForm(), t.getTargetJobName()));
    }
   }
   return new int[] { nextId };
  }

 }

 public JobDefinition getJobDefinition(String jobName) {
  for (JobDefinition jd : getJobDefinitions()) {
   if (jd.getJobName().equals(jobName)) {
    return jd;
   }
  }
  return null;
 }

 /**
  * Pretty print the text for this job specification, including appropriate
  * newlines and indentation.
  * @return formatted job specification.
  */
 public String format() {
  return jobNode.format(0);
 }
}