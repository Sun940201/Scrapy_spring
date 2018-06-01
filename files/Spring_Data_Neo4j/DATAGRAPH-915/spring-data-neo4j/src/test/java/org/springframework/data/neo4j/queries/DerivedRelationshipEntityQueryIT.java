/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class DerivedRelationshipEntityQueryIT extends MultiDriverTestClass {

 private static GraphDatabaseService graphDatabaseService;

 @Autowired
 private UserRepository userRepository;

 @Autowired
 private CinemaRepository cinemaRepository;

 @Autowired
 private RatingRepository ratingRepository;

 @Autowired
 private Neo4jOperations neo4jOperations;

 @BeforeClass
 public static void beforeClass(){
  graphDatabaseService = getGraphDatabaseService();
 }

 @Before
 public void clearDatabase() {
  graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
  neo4jOperations.clear();
 }

 private void executeUpdate(String cypher) {
  graphDatabaseService.execute(cypher);
 }

 /**
  * @see DATAGRAPH-629
  */
 @Test
 public void shouldFindREWithSingleProperty() {
  User critic = new User("Gary");
  TempMovie film = new TempMovie("Fast and Furious XVII");
  Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

  userRepository.save(critic);

  List<Rating> ratings = ratingRepository.findByStars(2);
  assertNotNull(ratings);
  Rating loadedRating = ratings.get(0);
  assertNotNull("The loaded rating shouldn't be null", loadedRating);
  assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
  assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
  assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
 }

 /**
  * @see DATAGRAPH-629
  */
 @Test
 public void shouldFindREWithMultiplePropertiesAnded() {
  User critic = new User("Gary");
  TempMovie film = new TempMovie("Fast and Furious XVII");
  Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
  filmRating.setRatingTimestamp(1000);

  userRepository.save(critic);

  List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 1000);
  assertNotNull(ratings);
  Rating loadedRating = ratings.get(0);
  assertNotNull("The loaded rating shouldn't be null", loadedRating);
  assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
  assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
  assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

  ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 2000);
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-629
  */
 @Test
 public void shouldFindREWithMultiplePropertiesOred() {
  User critic = new User("Gary");
  TempMovie film = new TempMovie("Fast and Furious XVII");
  Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
  filmRating.setRatingTimestamp(1000);

  userRepository.save(critic);

  List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestamp(5, 1000);
  assertNotNull(ratings);
  Rating loadedRating = ratings.get(0);
  assertNotNull("The loaded rating shouldn't be null", loadedRating);
  assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
  assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
  assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

  ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-629
  */
 @Test
 public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsAnded() {
  User critic = new User("Gary");
  TempMovie film = new TempMovie("Fast and Furious XVII");
  Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
  filmRating.setRatingTimestamp(1000);

  userRepository.save(critic);

  List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestampLessThan(2, 2000);
  assertNotNull(ratings);
  Rating loadedRating = ratings.get(0);
  assertNotNull("The loaded rating shouldn't be null", loadedRating);
  assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
  assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
  assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

  ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 3000);
  assertEquals(0, ratings.size());
 }


 /**
  * @see DATAGRAPH-629
  */
 @Test
 public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsOred() {
  User critic = new User("Gary");
  TempMovie film = new TempMovie("Fast and Furious XVII");
  Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
  filmRating.setRatingTimestamp(1000);

  userRepository.save(critic);

  List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestampGreaterThan(5, 500);
  assertNotNull(ratings);
  Rating loadedRating = ratings.get(0);
  assertNotNull("The loaded rating shouldn't be null", loadedRating);
  assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
  assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
  assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

  ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithNestedStartNodeProperty() {
  executeUpdate("CREATE (m1:Movie {name:'Speed'}) CREATE (m2:Movie {name:'The Matrix'}) CREATE (m:Movie {name:'Chocolat'})" +
    " CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

  List<Rating> ratings = ratingRepository.findByUserName("Michal");
  assertEquals(2, ratings.size());
  Collections.sort(ratings);
  assertEquals("Speed", ratings.get(0).getMovie().getName());
  assertEquals("The Matrix", ratings.get(1).getMovie().getName());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithNestedEndNodeProperty() {
  executeUpdate("CREATE (m1:Movie {name:'Finding Dory'}) CREATE (m2:Movie {name:'Captain America'}) CREATE (m:Movie {name:'X-Men'})" +
    " CREATE (u:User {name:'Vince'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

  List<Rating> ratings = ratingRepository.findByMovieName("Captain America");
  assertEquals(1, ratings.size());
  assertEquals("Vince", ratings.get(0).getUser().getName());
  assertEquals("Captain America", ratings.get(0).getMovie().getName());
  assertEquals(4, ratings.get(0).getStars());

  ratings = ratingRepository.findByMovieName("X-Men");
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithBothStartEndNestedProperty() {
  executeUpdate("CREATE (m1:Movie {name:'Independence Day: Resurgence'}) CREATE (m2:Movie {name:'The Conjuring 2'}) CREATE (m:Movie {name:'The BFG'})" +
    " CREATE (u:User {name:'Daniela'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
  List<Rating> ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "Independence Day: Resurgence");
  assertEquals(1, ratings.size());
  assertEquals("Daniela", ratings.get(0).getUser().getName());
  assertEquals("Independence Day: Resurgence", ratings.get(0).getMovie().getName());
  assertEquals(3, ratings.get(0).getStars());

  ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "The BFG");
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyAnded() {
  executeUpdate("CREATE (m1:Movie {name:'The Shallows'}) CREATE (m2:Movie {name:'Central Intelligence'}) CREATE (m:Movie {name:'Now you see me'})" +
    " CREATE (u:User {name:'Luanne'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
  List<Rating> ratings = ratingRepository.findByUserNameAndStars("Luanne", 3);
  assertEquals(1, ratings.size());
  assertEquals("Luanne", ratings.get(0).getUser().getName());
  assertEquals("The Shallows", ratings.get(0).getMovie().getName());
  assertEquals(3, ratings.get(0).getStars());

  ratings = ratingRepository.findByUserNameAndStars("Luanne", 1);
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-662
  * //TODO FIXME
  */
 @Test(expected = UnsupportedOperationException.class)
 public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyOred() {
  executeUpdate("CREATE (m1:Movie {name:'Swiss Army Man'}) CREATE (m2:Movie {name:'Me Before You'}) CREATE (m:Movie {name:'X-Men Apocalypse'})" +
    " CREATE (u:User {name:'Mark'}) CREATE (u2:User {name:'Adam'})  " +
    " CREATE (u)-[:RATED {stars:2}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
    " CREATE (u2)-[:RATED {stars:3}]->(m)");
  List<Rating> ratings = ratingRepository.findByStarsOrUserName(3, "Mark");
  assertEquals(3, ratings.size());
  Collections.sort(ratings);
  assertEquals("Swiss Army Man", ratings.get(0).getMovie().getName());
  assertEquals("X-Men Apocalypse", ratings.get(1).getMovie().getName());
  assertEquals("Me Before You", ratings.get(2).getMovie().getName());

  ratings = ratingRepository.findByStarsOrUserName(0, "Vince");
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithBaseAndNestedEndNodeProperty() {
  executeUpdate("CREATE (m1:Movie {name:'Our Kind of Traitor'}) CREATE (m2:Movie {name:'Teenage Mutant Ninja Turtles'}) CREATE (m:Movie {name:'Zootopia'})" +
    " CREATE (u:User {name:'Chris'}) CREATE (u2:User {name:'Katerina'}) " +
    " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
    " CREATE (u2)-[:RATED {stars:4}]->(m2)");
  List<Rating> ratings = ratingRepository.findByStarsAndMovieName(4, "Teenage Mutant Ninja Turtles");
  assertEquals(2, ratings.size());
  Collections.sort(ratings);
  assertEquals("Chris", ratings.get(0).getUser().getName());
  assertEquals("Katerina", ratings.get(1).getUser().getName());

  ratings = ratingRepository.findByStarsAndMovieName(5, "Teenage Mutant Ninja Turtles");
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithBaseAndBothStartEndNestedProperty() {
  executeUpdate("CREATE (m1:Movie {name:'The Jungle Book'}) CREATE (m2:Movie {name:'The Angry Birds Movie'}) CREATE (m:Movie {name:'Alice Through The Looking Glass'})" +
    " CREATE (u:User {name:'Alessandro'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
  List<Rating> ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Alessandro", "The Jungle Book", 3);
  assertEquals(1, ratings.size());
  assertEquals("Alessandro", ratings.get(0).getUser().getName());
  assertEquals("The Jungle Book", ratings.get(0).getMovie().getName());
  assertEquals(3, ratings.get(0).getStars());

  ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Colin", "Speed", 0);
  assertEquals(0, ratings.size());
 }

 /**
  * @see DATAGRAPH-632
  */
 @Test
 public void shouldFindRelEntitiesWithTwoStartNodeNestedProperties() {
  executeUpdate("CREATE (m1:Movie {name:'Batman v Superman'}) CREATE (m2:Movie {name:'Genius'}) CREATE (m:Movie {name:'Home'})" +
    " CREATE (u:User {name:'David', middleName:'M'}) CREATE (u2:User {name:'Martin', middleName:'M'}) " +
    " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
    " CREATE (u2)-[:RATED {stars:4}]->(m2)");
  List<Rating> ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "M");
  assertEquals(2, ratings.size());
  Collections.sort(ratings);
  assertEquals("David", ratings.get(0).getUser().getName());
  assertEquals("Batman v Superman", ratings.get(0).getMovie().getName());
  assertEquals("David", ratings.get(1).getUser().getName());
  assertEquals("Genius", ratings.get(1).getMovie().getName());

  ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "V");
  assertEquals(0, ratings.size());
 }
}