/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.data.mongodb.core.aggregation;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.mongodb.core.aggregation.AggregationFunctionExpressions.*;
import static org.springframework.data.mongodb.core.aggregation.Fields.*;
import static org.springframework.data.mongodb.test.util.IsBsonObject.*;
import static org.springframework.data.mongodb.util.DBObjectUtils.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.data.mongodb.core.DBObjectTestUtils;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation.ProjectionOperationBuilder;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Unit tests for {@link ProjectionOperation}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
public class ProjectionOperationUnitTests {

 static final String MOD = "$mod";
 static final String ADD = "$add";
 static final String SUBTRACT = "$subtract";
 static final String MULTIPLY = "$multiply";
 static final String DIVIDE = "$divide";
 static final String PROJECT = "$project";

 @Test(expected = IllegalArgumentException.class)
 public void rejectsNullFields() {
  new ProjectionOperation(null);
 }

 @Test
 public void declaresBackReferenceCorrectly() {

  ProjectionOperation operation = new ProjectionOperation();
  operation = operation.and("prop").previousOperation();

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  assertThat(projectClause.get("prop"), is((Object) Fields.UNDERSCORE_ID_REF));
 }

 @Test
 public void alwaysUsesExplicitReference() {

  ProjectionOperation operation = new ProjectionOperation(Fields.fields("foo").and("bar", "foobar"));

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);

  assertThat(projectClause.get("foo"), is((Object) 1));
  assertThat(projectClause.get("bar"), is((Object) "$foobar"));
 }

 @Test
 public void aliasesSimpleFieldProjection() {

  ProjectionOperation operation = new ProjectionOperation();

  DBObject dbObject = operation.and("foo").as("bar").toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);

  assertThat(projectClause.get("bar"), is((Object) "$foo"));
 }

 @Test
 public void aliasesArithmeticProjection() {

  ProjectionOperation operation = new ProjectionOperation();

  DBObject dbObject = operation.and("foo").plus(41).as("bar").toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject barClause = DBObjectTestUtils.getAsDBObject(projectClause, "bar");
  List<Object> addClause = (List<Object>) barClause.get("$add");

  assertThat(addClause, hasSize(2));
  assertThat(addClause.get(0), is((Object) "$foo"));
  assertThat(addClause.get(1), is((Object) 41));
 }

 public void arithmenticProjectionOperationWithoutAlias() {

  String fieldName = "a";
  ProjectionOperationBuilder operation = new ProjectionOperation().and(fieldName).plus(1);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject oper = exctractOperation(fieldName, projectClause);

  assertThat(oper.containsField(ADD), is(true));
  assertThat(oper.get(ADD), is((Object) Arrays.<Object> asList("$a", 1)));
 }

 @Test
 public void arithmenticProjectionOperationPlus() {

  String fieldName = "a";
  String fieldAlias = "b";
  ProjectionOperation operation = new ProjectionOperation().and(fieldName).plus(1).as(fieldAlias);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);

  DBObject oper = exctractOperation(fieldAlias, projectClause);
  assertThat(oper.containsField(ADD), is(true));
  assertThat(oper.get(ADD), is((Object) Arrays.<Object> asList("$a", 1)));
 }

 @Test
 public void arithmenticProjectionOperationMinus() {

  String fieldName = "a";
  String fieldAlias = "b";
  ProjectionOperation operation = new ProjectionOperation().and(fieldName).minus(1).as(fieldAlias);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject oper = exctractOperation(fieldAlias, projectClause);

  assertThat(oper.containsField(SUBTRACT), is(true));
  assertThat(oper.get(SUBTRACT), is((Object) Arrays.<Object> asList("$a", 1)));
 }

 @Test
 public void arithmenticProjectionOperationMultiply() {

  String fieldName = "a";
  String fieldAlias = "b";
  ProjectionOperation operation = new ProjectionOperation().and(fieldName).multiply(1).as(fieldAlias);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject oper = exctractOperation(fieldAlias, projectClause);

  assertThat(oper.containsField(MULTIPLY), is(true));
  assertThat(oper.get(MULTIPLY), is((Object) Arrays.<Object> asList("$a", 1)));
 }

 @Test
 public void arithmenticProjectionOperationDivide() {

  String fieldName = "a";
  String fieldAlias = "b";
  ProjectionOperation operation = new ProjectionOperation().and(fieldName).divide(1).as(fieldAlias);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject oper = exctractOperation(fieldAlias, projectClause);

  assertThat(oper.containsField(DIVIDE), is(true));
  assertThat(oper.get(DIVIDE), is((Object) Arrays.<Object> asList("$a", 1)));
 }

 @Test(expected = IllegalArgumentException.class)
 public void arithmenticProjectionOperationDivideByZeroException() {

  new ProjectionOperation().and("a").divide(0);
 }

 @Test
 public void arithmenticProjectionOperationMod() {

  String fieldName = "a";
  String fieldAlias = "b";
  ProjectionOperation operation = new ProjectionOperation().and(fieldName).mod(3).as(fieldAlias);
  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  DBObject oper = exctractOperation(fieldAlias, projectClause);

  assertThat(oper.containsField(MOD), is(true));
  assertThat(oper.get(MOD), is((Object) Arrays.<Object> asList("$a", 3)));
 }

 /**
  * @see DATAMONGO-758
  */
 @Test(expected = IllegalArgumentException.class)
 public void excludeShouldThrowExceptionForFieldsOtherThanUnderscoreId() {

  new ProjectionOperation().andExclude("foo");
 }

 /**
  * @see DATAMONGO-758
  */
 @Test
 public void excludeShouldAllowExclusionOfUnderscoreId() {

  ProjectionOperation projectionOp = new ProjectionOperation().andExclude(Fields.UNDERSCORE_ID);
  DBObject dbObject = projectionOp.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);
  assertThat((Integer) projectClause.get(Fields.UNDERSCORE_ID), is(0));
 }

 /**
  * @see DATAMONGO-757
  */
 @Test
 public void usesImplictAndExplicitFieldAliasAndIncludeExclude() {

  ProjectionOperation operation = Aggregation.project("foo").and("foobar").as("bar").andInclude("inc1", "inc2")
    .andExclude("_id");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);

  assertThat(projectClause.get("foo"), is((Object) 1)); // implicit
  assertThat(projectClause.get("bar"), is((Object) "$foobar")); // explicit
  assertThat(projectClause.get("inc1"), is((Object) 1)); // include shortcut
  assertThat(projectClause.get("inc2"), is((Object) 1));
  assertThat(projectClause.get("_id"), is((Object) 0));
 }

 @Test(expected = IllegalArgumentException.class)
 public void arithmenticProjectionOperationModByZeroException() {

  new ProjectionOperation().and("a").mod(0);
 }

 /**
  * @see DATAMONGO-769
  */
 @Test
 public void allowArithmeticOperationsWithFieldReferences() {

  ProjectionOperation operation = Aggregation.project() //
    .and("foo").plus("bar").as("fooPlusBar") //
    .and("foo").minus("bar").as("fooMinusBar") //
    .and("foo").multiply("bar").as("fooMultiplyBar") //
    .and("foo").divide("bar").as("fooDivideBar") //
    .and("foo").mod("bar").as("fooModBar");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projectClause = DBObjectTestUtils.getAsDBObject(dbObject, PROJECT);

  assertThat((BasicDBObject) projectClause.get("fooPlusBar"), //
    is(new BasicDBObject("$add", dbList("$foo", "$bar"))));
  assertThat((BasicDBObject) projectClause.get("fooMinusBar"), //
    is(new BasicDBObject("$subtract", dbList("$foo", "$bar"))));
  assertThat((BasicDBObject) projectClause.get("fooMultiplyBar"), //
    is(new BasicDBObject("$multiply", dbList("$foo", "$bar"))));
  assertThat((BasicDBObject) projectClause.get("fooDivideBar"), //
    is(new BasicDBObject("$divide", dbList("$foo", "$bar"))));
  assertThat((BasicDBObject) projectClause.get("fooModBar"), //
    is(new BasicDBObject("$mod", dbList("$foo", "$bar"))));
 }

 /**
  * @see DATAMONGO-774
  */
 @Test
 public void projectionExpressions() {

  ProjectionOperation operation = Aggregation.project() //
    .andExpression("(netPrice + surCharge) * taxrate * [0]", 2).as("grossSalesPrice") //
    .and("foo").as("bar"); //

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  assertThat(
    dbObject.toString(),
    is("{ \"$project\" : { \"grossSalesPrice\" : { \"$multiply\" : [ { \"$add\" : [ \"$netPrice\" , \"$surCharge\"]} , \"$taxrate\" , 2]} , \"bar\" : \"$foo\"}}"));
 }

 /**
  * @see DATAMONGO-975
  */
 @Test
 public void shouldRenderDateTimeFragmentExtractionsForSimpleFieldProjectionsCorrectly() {

  ProjectionOperation operation = Aggregation.project() //
    .and("date").extractHour().as("hour") //
    .and("date").extractMinute().as("min") //
    .and("date").extractSecond().as("second") //
    .and("date").extractMillisecond().as("millis") //
    .and("date").extractYear().as("year") //
    .and("date").extractMonth().as("month") //
    .and("date").extractWeek().as("week") //
    .and("date").extractDayOfYear().as("dayOfYear") //
    .and("date").extractDayOfMonth().as("dayOfMonth") //
    .and("date").extractDayOfWeek().as("dayOfWeek") //
  ;

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  assertThat(dbObject, is(notNullValue()));

  DBObject projected = exctractOperation("$project", dbObject);

  assertThat(projected.get("hour"), is((Object) new BasicDBObject("$hour", Arrays.asList("$date"))));
  assertThat(projected.get("min"), is((Object) new BasicDBObject("$minute", Arrays.asList("$date"))));
  assertThat(projected.get("second"), is((Object) new BasicDBObject("$second", Arrays.asList("$date"))));
  assertThat(projected.get("millis"), is((Object) new BasicDBObject("$millisecond", Arrays.asList("$date"))));
  assertThat(projected.get("year"), is((Object) new BasicDBObject("$year", Arrays.asList("$date"))));
  assertThat(projected.get("month"), is((Object) new BasicDBObject("$month", Arrays.asList("$date"))));
  assertThat(projected.get("week"), is((Object) new BasicDBObject("$week", Arrays.asList("$date"))));
  assertThat(projected.get("dayOfYear"), is((Object) new BasicDBObject("$dayOfYear", Arrays.asList("$date"))));
  assertThat(projected.get("dayOfMonth"), is((Object) new BasicDBObject("$dayOfMonth", Arrays.asList("$date"))));
  assertThat(projected.get("dayOfWeek"), is((Object) new BasicDBObject("$dayOfWeek", Arrays.asList("$date"))));
 }

 /**
  * @see DATAMONGO-975
  */
 @Test
 public void shouldRenderDateTimeFragmentExtractionsForExpressionProjectionsCorrectly() throws Exception {

  ProjectionOperation operation = Aggregation.project() //
    .andExpression("date + 86400000") //
    .extractDayOfYear() //
    .as("dayOfYearPlus1Day") //
  ;

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  assertThat(dbObject, is(notNullValue()));

  DBObject projected = exctractOperation("$project", dbObject);
  assertThat(
    projected.get("dayOfYearPlus1Day"),
    is((Object) new BasicDBObject("$dayOfYear", Arrays.asList(new BasicDBObject("$add", Arrays.<Object> asList(
      "$date", 86400000))))));
 }

 /**
  * @see DATAMONGO-979
  */
 @Test
 public void shouldRenderSizeExpressionInProjection() {

  ProjectionOperation operation = Aggregation //
    .project() //
    .and("tags") //
    .size()//
    .as("tags_count");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);

  DBObject projected = exctractOperation("$project", dbObject);
  assertThat(projected.get("tags_count"), is((Object) new BasicDBObject("$size", Arrays.asList("$tags"))));
 }

 /**
  * @see DATAMONGO-979
  */
 @Test
 public void shouldRenderGenericSizeExpressionInProjection() {

  ProjectionOperation operation = Aggregation //
    .project() //
    .and(SIZE.of(field("tags"))) //
    .as("tags_count");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);

  DBObject projected = exctractOperation("$project", dbObject);
  assertThat(projected.get("tags_count"), is((Object) new BasicDBObject("$size", Arrays.asList("$tags"))));
 }

 /**
  * @see DATAMONGO-1457
  */
 @Test
 public void shouldRenderSliceCorrectly() throws Exception {

  ProjectionOperation operation = Aggregation.project().and("field").slice(10).as("renamed");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projected = exctractOperation("$project", dbObject);

  assertThat(projected.get("renamed"),
    is((Object) new BasicDBObject("$slice", Arrays.<Object> asList("$field", 10))));
 }

 /**
  * @see DATAMONGO-1457
  */
 @Test
 public void shouldRenderSliceWithPositionCorrectly() throws Exception {

  ProjectionOperation operation = Aggregation.project().and("field").slice(10, 5).as("renamed");

  DBObject dbObject = operation.toDBObject(Aggregation.DEFAULT_CONTEXT);
  DBObject projected = exctractOperation("$project", dbObject);

  assertThat(projected.get("renamed"),
    is((Object) new BasicDBObject("$slice", Arrays.<Object> asList("$field", 5, 10))));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderCmpCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").cmp(10).as("cmp10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.cmp10.$cmp.[0]", "$field").containing("$project.cmp10.$cmp.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderEqCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").eq(10).as("eq10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.eq10.$eq.[0]", "$field").containing("$project.eq10.$eq.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderGtCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").gt(10).as("gt10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.gt10.$gt.[0]", "$field").containing("$project.gt10.$gt.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderGteCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").gte(10).as("gte10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.gte10.$gte.[0]", "$field").containing("$project.gte10.$gte.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderLtCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").lt(10).as("lt10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.lt10.$lt.[0]", "$field").containing("$project.lt10.$lt.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderLteCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").lte(10).as("lte10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.lte10.$lte.[0]", "$field").containing("$project.lte10.$lte.[1]", 10));
 }

 /**
  * @see DATAMONGO-784
  */
 @Test
 public void shouldRenderNeCorrectly() {

  ProjectionOperation operation = Aggregation.project().and("field").ne(10).as("ne10");

  assertThat(operation.toDBObject(Aggregation.DEFAULT_CONTEXT),
    isBsonObject().containing("$project.ne10.$ne.[0]", "$field").containing("$project.ne10.$ne.[1]", 10));
 }

 private static DBObject exctractOperation(String field, DBObject fromProjectClause) {
  return (DBObject) fromProjectClause.get(field);
 }
}