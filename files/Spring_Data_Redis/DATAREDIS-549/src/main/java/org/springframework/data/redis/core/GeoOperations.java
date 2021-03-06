/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.redis.core;

import java.util.List;
import java.util.Map;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;

/**
 * Redis operations for geo commands.
 *
 * @author Ninad Divadkar
 * @author Christoph Strobl
 * @see <a href="http://redis.io/commands#geo">http://redis.io/commands#geo</a>
 * @since 1.8
 */
public interface GeoOperations<K, M> {

 /**
  * Add {@link Point} with given member {@literal name} to {@literal key}.
  * 
  * @param key must not be {@literal null}.
  * @param point must not be {@literal null}.
  * @param member must not be {@literal null}.
  * @return Number of elements added.
  * @see <a href="http://redis.io/commands/geoadd">http://redis.io/commands/geoadd</a>
  */
 Long geoAdd(K key, Point point, M member);

 /**
  * Add {@link GeoLocation} to {@literal key}.
  * 
  * @param key must not be {@literal null}.
  * @param location must not be {@literal null}.
  * @return Number of elements added.
  * @see <a href="http://redis.io/commands/geoadd">http://redis.io/commands/geoadd</a>
  */
 Long geoAdd(K key, GeoLocation<M> location);

 /**
  * Add {@link Map} of member / {@link Point} pairs to {@literal key}.
  * 
  * @param key must not be {@literal null}.
  * @param memberCoordinateMap must not be {@literal null}.
  * @return Number of elements added.
  * @see <a href="http://redis.io/commands/geoadd">http://redis.io/commands/geoadd</a>
  */
 Long geoAdd(K key, Map<M, Point> memberCoordinateMap);

 /**
  * Add {@link GeoLocation}s to {@literal key}
  * 
  * @param key must not be {@literal null}.
  * @param locations must not be {@literal null}.
  * @return Number of elements added.
  * @see <a href="http://redis.io/commands/geoadd">http://redis.io/commands/geoadd</a>
  */
 Long geoAdd(K key, Iterable<GeoLocation<M>> locations);

 /**
  * Get the {@link Distance} between {@literal member1} and {@literal member2}.
  * 
  * @param key must not be {@literal null}.
  * @param member1 must not be {@literal null}.
  * @param member2 must not be {@literal null}.
  * @return can be {@literal null}.
  * @see <a href="http://redis.io/commands/geodist">http://redis.io/commands/geodist</a>
  */
 Distance geoDist(K key, M member1, M member2);

 /**
  * Get the {@link Distance} between {@literal member1} and {@literal member2} in the given {@link Metric}.
  * 
  * @param key must not be {@literal null}.
  * @param member1 must not be {@literal null}.
  * @param member2 must not be {@literal null}.
  * @param metric must not be {@literal null}.
  * @return can be {@literal null}.
  * @see <a href="http://redis.io/commands/geodist">http://redis.io/commands/geodist</a>
  */
 Distance geoDist(K key, M member1, M member2, Metric metric);

 /**
  * Get Geohash representation of the position for one or more {@literal member}s.
  * 
  * @param key must not be {@literal null}.
  * @param members must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/geohash">http://redis.io/commands/geohash</a>
  */
 List<String> geoHash(K key, M... members);

 /**
  * Get the {@link Point} representation of positions for one or more {@literal member}s.
  * 
  * @param key must not be {@literal null}.
  * @param members must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/geopos">http://redis.io/commands/geopos</a>
  */
 List<Point> geoPos(K key, M... members);

 /**
  * Get the {@literal member}s within the boundaries of a given {@link Circle}.
  * 
  * @param key must not be {@literal null}.
  * @param within must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/georadius">http://redis.io/commands/georadius</a>
  */
 GeoResults<GeoLocation<M>> geoRadius(K key, Circle within);

 /**
  * Get the {@literal member}s within the boundaries of a given {@link Circle} applying {@link GeoRadiusCommandArgs}.
  * 
  * @param key must not be {@literal null}.
  * @param within must not be {@literal null}.
  * @param args must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/georadius">http://redis.io/commands/georadius</a>
  */
 GeoResults<GeoLocation<M>> geoRadius(K key, Circle within, GeoRadiusCommandArgs args);

 /**
  * Get the {@literal member}s within the circle defined by the {@literal members} coordinates and given
  * {@literal radius}.
  * 
  * @param key must not be {@literal null}.
  * @param member must not be {@literal null}.
  * @param radius
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/georadiusbymember">http://redis.io/commands/georadiusbymember</a>
  */
 GeoResults<GeoLocation<M>> geoRadiusByMember(K key, M member, double radius);

 /**
  * Get the {@literal member}s within the circle defined by the {@literal members} coordinates and given
  * {@literal radius} applying {@link Metric}.
  * 
  * @param key must not be {@literal null}.
  * @param member must not be {@literal null}.
  * @param distance must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/georadiusbymember">http://redis.io/commands/georadiusbymember</a>
  */
 GeoResults<GeoLocation<M>> geoRadiusByMember(K key, M member, Distance distance);

 /**
  * Get the {@literal member}s within the circle defined by the {@literal members} coordinates and given
  * {@literal radius} applying {@link Metric} and {@link GeoRadiusCommandArgs}.
  * 
  * @param key must not be {@literal null}.
  * @param member must not be {@literal null}.
  * @param distance must not be {@literal null}.
  * @param args must not be {@literal null}.
  * @return never {@literal null}.
  * @see <a href="http://redis.io/commands/georadiusbymember">http://redis.io/commands/georadiusbymember</a>
  */
 GeoResults<GeoLocation<M>> geoRadiusByMember(K key, M member, Distance distance, GeoRadiusCommandArgs args);

 /**
  * Remove the {@literal member}s.
  * 
  * @param key must not be {@literal null}.
  * @param members must not be {@literal null}.
  * @return Number of elements removed.
  */
 Long geoRemove(K key, M... members);
}