/*
 * Copyright 2011-2017 the original author or authors.
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
package org.springframework.data.convert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Value object to capture custom conversion. That is essentially a {@link List} of converters and some additional logic
 * around them. The converters build up two sets of types which store-specific basic types can be converted into and
 * from. These types will be considered simple ones (which means they neither need deeper inspection nor nested
 * conversion. Thus the {@link CustomConversions} also act as factory for {@link SimpleTypeHolder} .
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.0
 */
@Slf4j
public class CustomConversions {

 private static final String READ_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as reading converter although it doesn't convert from a store-supported type! You might wanna check you annotation setup at the converter implementation.";
 private static final String WRITE_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as writing converter although it doesn't convert to a store-supported type! You might wanna check you annotation setup at the converter implementation.";
 private static final String NOT_A_CONVERTER = "Converter %s is neither a Spring Converter, GenericConverter or ConverterFactory!";
 private static final List<Object> DEFAULT_CONVERTERS;

 static {

  List<Object> defaults = new ArrayList<>();

  defaults.addAll(JodaTimeConverters.getConvertersToRegister());
  defaults.addAll(Jsr310Converters.getConvertersToRegister());
  defaults.addAll(ThreeTenBackPortConverters.getConvertersToRegister());

  DEFAULT_CONVERTERS = Collections.unmodifiableList(defaults);
 }

 private final Set<ConvertiblePair> readingPairs;
 private final Set<ConvertiblePair> writingPairs;
 private final Set<Class<?>> customSimpleTypes;
 private final SimpleTypeHolder simpleTypeHolder;

 private final List<Object> converters;

 private final Map<ConvertiblePair, Optional<Class<?>>> customReadTargetTypes;
 private final Map<ConvertiblePair, Optional<Class<?>>> customWriteTargetTypes;
 private final Map<Class<?>, Optional<Class<?>>> rawWriteTargetTypes;

 /**
  * Creates a new {@link CustomConversions} instance registering the given converters.
  * 
  * @param converters
  */
 public CustomConversions(StoreConversions storeConversions, List<?> converters) {

  Assert.notNull(converters, "List of converters must not be null!");

  this.readingPairs = new LinkedHashSet<>();
  this.writingPairs = new LinkedHashSet<>();
  this.customSimpleTypes = new HashSet<>();
  this.customReadTargetTypes = new ConcurrentHashMap<>();
  this.customWriteTargetTypes = new ConcurrentHashMap<>();
  this.rawWriteTargetTypes = new ConcurrentHashMap<>();

  List<Object> toRegister = new ArrayList<Object>();

  // Add user provided converters to make sure they can override the defaults
  toRegister.addAll(converters);
  toRegister.addAll(storeConversions.getStoreConverters());
  toRegister.addAll(DEFAULT_CONVERTERS);

  toRegister.stream()//
    .flatMap(it -> storeConversions.getRegistrationsFor(it).stream())//
    .forEach(this::register);

  Collections.reverse(toRegister);

  this.converters = Collections.unmodifiableList(toRegister);
  this.simpleTypeHolder = new SimpleTypeHolder(customSimpleTypes, storeConversions.getStoreTypeHolder());
 }

 /**
  * Returns the underlying {@link SimpleTypeHolder}.
  * 
  * @return
  */
 public SimpleTypeHolder getSimpleTypeHolder() {
  return simpleTypeHolder;
 }

 /**
  * Returns whether the given type is considered to be simple. That means it's either a general simple type or we have
  * a writing {@link Converter} registered for a particular type.
  * 
  * @see SimpleTypeHolder#isSimpleType(Class)
  * @param type
  * @return
  */
 public boolean isSimpleType(Class<?> type) {

  Assert.notNull(type, "Type must not be null!");

  return simpleTypeHolder.isSimpleType(type);
 }

 /**
  * Populates the given {@link GenericConversionService} with the converters registered.
  * 
  * @param conversionService
  */
 public void registerConvertersIn(GenericConversionService conversionService) {

  Assert.notNull(conversionService, "ConversionService must not be null!");

  converters.forEach(it -> {

   boolean added = false;

   if (it instanceof Converter) {
    conversionService.addConverter(Converter.class.cast(it));
    added = true;
   }

   if (it instanceof ConverterFactory) {
    conversionService.addConverterFactory(ConverterFactory.class.cast(it));
    added = true;
   }

   if (it instanceof GenericConverter) {
    conversionService.addConverter(GenericConverter.class.cast(it));
    added = true;
   }

   if (!added) {
    throw new IllegalArgumentException(String.format(NOT_A_CONVERTER, it));
   }
  });
 }

 /**
  * Registers the given {@link ConvertiblePair} as reading or writing pair depending on the type sides being basic
  * Mongo types.
  * 
  * @param pair
  */
 private void register(ConverterRegistration converterRegistration) {

  Assert.notNull(converterRegistration, "Converter registration must not be null!");

  ConvertiblePair pair = converterRegistration.getConvertiblePair();

  if (converterRegistration.isReading()) {

   readingPairs.add(pair);

   if (LOG.isWarnEnabled() && !converterRegistration.isSimpleSourceType()) {
    LOG.warn(String.format(READ_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
   }
  }

  if (converterRegistration.isWriting()) {

   writingPairs.add(pair);
   customSimpleTypes.add(pair.getSourceType());

   if (LOG.isWarnEnabled() && !converterRegistration.isSimpleTargetType()) {
    LOG.warn(String.format(WRITE_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
   }
  }
 }

 /**
  * Returns the target type to convert to in case we have a custom conversion registered to convert the given source
  * type into a Mongo native one.
  * 
  * @param sourceType must not be {@literal null}
  * @return
  */
 public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {

  Assert.notNull(sourceType, "Source type must not be null!");

  return rawWriteTargetTypes.computeIfAbsent(sourceType,
    it -> getCustomTarget(sourceType, Optional.empty(), writingPairs));
 }

 /**
  * Returns the target type we can readTargetWriteLocl an inject of the given source type to. The returned type might
  * be a subclass of the given expected type though. If {@code expectedTargetType} is {@literal null} we will simply
  * return the first target type matching or {@literal null} if no conversion can be found.
  * 
  * @param sourceType must not be {@literal null}
  * @param requestedTargetType must not be {@literal null}.
  * @return
  */
 public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType, Class<?> requestedTargetType) {

  Assert.notNull(sourceType, "Source type must not be null!");
  Assert.notNull(requestedTargetType, "Target type must not be null!");

  return customWriteTargetTypes.computeIfAbsent(new ConvertiblePair(sourceType, requestedTargetType),
    it -> getCustomTarget(sourceType, Optional.of(requestedTargetType), writingPairs));
 }

 /**
  * Returns whether we have a custom conversion registered to readTargetWriteLocl into a Mongo native type. The
  * returned type might be a subclass of the given expected type though.
  * 
  * @param sourceType must not be {@literal null}
  * @return
  */
 public boolean hasCustomWriteTarget(Class<?> sourceType) {

  Assert.notNull(sourceType, "Source type must not be null!");

  return getCustomWriteTarget(sourceType).isPresent();
 }

 /**
  * Returns whether we have a custom conversion registered to readTargetWriteLocl an object of the given source type
  * into an object of the given Mongo native target type.
  * 
  * @param sourceType must not be {@literal null}.
  * @param targetType must not be {@literal null}.
  * @return
  */
 public boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {

  Assert.notNull(sourceType, "Source type must not be null!");
  Assert.notNull(targetType, "Target type must not be null!");

  return getCustomWriteTarget(sourceType, targetType).isPresent();
 }

 /**
  * Returns whether we have a custom conversion registered to readTargetReadLock the given source into the given target
  * type.
  * 
  * @param sourceType must not be {@literal null}
  * @param targetType must not be {@literal null}
  * @return
  */
 public boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {

  Assert.notNull(sourceType, "Source type must not be null!");
  Assert.notNull(targetType, "Target type must not be null!");

  return getCustomReadTarget(sourceType, targetType).isPresent();
 }

 /**
  * Returns the actual target type for the given {@code sourceType} and {@code requestedTargetType}. Note that the
  * returned {@link Class} could be an assignable type to the given {@code requestedTargetType}.
  * 
  * @param sourceType must not be {@literal null}.
  * @param targetType must not be {@literal null}.
  * @return
  */
 private Optional<Class<?>> getCustomReadTarget(Class<?> sourceType, Class<?> targetType) {

  return customReadTargetTypes.computeIfAbsent(new ConvertiblePair(sourceType, targetType),
    it -> getCustomTarget(sourceType, Optional.of(targetType), readingPairs));
 }

 /**
  * Inspects the given {@link ConvertiblePair}s for ones that have a source compatible type as source. Additionally
  * checks assignability of the target type if one is given.
  * 
  * @param sourceType must not be {@literal null}.
  * @param targetType can be {@literal null}.
  * @param pairs must not be {@literal null}.
  * @return
  */
 private static Optional<Class<?>> getCustomTarget(Class<?> sourceType, Optional<Class<?>> targetType,
   Collection<ConvertiblePair> pairs) {

  Assert.notNull(sourceType, "Source Class must not be null!");
  Assert.notNull(pairs, "Collection of ConvertiblePair must not be null!");

  return Optionals.firstNonEmpty(//
    () -> targetType.filter(it -> pairs.contains(new ConvertiblePair(sourceType, it))), //
    () -> pairs.stream()//
      .filter(it -> hasAssignableSourceType(it, sourceType)) //
      .<Class<?>> map(ConvertiblePair::getTargetType)//
      .filter(it -> requestTargetTypeIsAssignable(targetType, it))//
      .findFirst());
 }

 private static boolean hasAssignableSourceType(ConvertiblePair pair, Class<?> sourceType) {
  return pair.getSourceType().isAssignableFrom(sourceType);
 }

 private static boolean requestTargetTypeIsAssignable(Optional<Class<?>> requestedTargetType, Class<?> targetType) {

  return !requestedTargetType.isPresent() //
    ? true //
    : requestedTargetType.map(it -> targetType.isAssignableFrom(it)).orElse(false);
 }

 /**
  * Conversion registration information.
  * 
  * @author Oliver Gierke
  * @author Mark Paluch
  */
 @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
 private static class ConverterRegistration {

  private final @NonNull ConvertiblePair convertiblePair;
  private final @NonNull StoreConversions storeConversions;
  private final boolean reading;
  private final boolean writing;

  /**
   * Returns whether the converter shall be used for writing.
   * 
   * @return
   */
  public boolean isWriting() {
   return writing == true || (!reading && isSimpleTargetType());
  }

  /**
   * Returns whether the converter shall be used for reading.
   * 
   * @return
   */
  public boolean isReading() {
   return reading == true || (!writing && isSimpleSourceType());
  }

  /**
   * Returns the actual conversion pair.
   * 
   * @return
   */
  public ConvertiblePair getConvertiblePair() {
   return convertiblePair;
  }

  /**
   * Returns whether the source type is a Mongo simple one.
   * 
   * @return
   */
  public boolean isSimpleSourceType() {
   return storeConversions.isStoreSimpleType(convertiblePair.getSourceType());
  }

  /**
   * Returns whether the target type is a Mongo simple one.
   * 
   * @return
   */
  public boolean isSimpleTargetType() {
   return storeConversions.isStoreSimpleType(convertiblePair.getTargetType());
  }
 }

 /**
  * Value type to capture store-specific extensions to the {@link CustomConversions}. Allows to forward store specific
  * default conversions and a set of types that are supposed to be considered simple.
  *
  * @author Oliver Gierke
  */
 @Value
 @Getter(AccessLevel.PACKAGE)
 @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
 public static class StoreConversions {

  public static final StoreConversions NONE = StoreConversions.of(SimpleTypeHolder.DEFAULT, Collections.emptyList());

  SimpleTypeHolder storeTypeHolder;
  Collection<?> storeConverters;

  /**
   * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
   * converters.
   * 
   * @param storeTypeHolder must not be {@literal null}.
   * @param converters must not be {@literal null}.
   * @return
   */
  public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Object... converters) {

   Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null!");
   Assert.notNull(converters, "Converters must not be null!");

   return new StoreConversions(storeTypeHolder, Arrays.asList(converters));
  }

  /**
   * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
   * converters.
   * 
   * @param storeTypeHolder must not be {@literal null}.
   * @param converters must not be {@literal null}.
   * @return
   */
  public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Collection<?> converters) {

   Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null!");
   Assert.notNull(converters, "Converters must not be null!");

   return new StoreConversions(storeTypeHolder, converters);
  }

  /**
   * Returns {@link ConverterRegistration}s for the given converter.
   * 
   * @param converter must not be {@literal null}.
   * @return
   */
  public Streamable<ConverterRegistration> getRegistrationsFor(Object converter) {

   Assert.notNull(converter, "Converter must not be null!");

   Class<?> type = converter.getClass();
   boolean isWriting = type.isAnnotationPresent(WritingConverter.class);
   boolean isReading = type.isAnnotationPresent(ReadingConverter.class);

   if (converter instanceof GenericConverter) {

    GenericConverter genericConverter = (GenericConverter) converter;
    return Streamable.of(genericConverter.getConvertibleTypes()).map(it -> register(it, isReading, isWriting));

   } else if (converter instanceof ConverterFactory) {

    return getRegistrationFor(converter, ConverterFactory.class, isReading, isWriting);

   } else if (converter instanceof Converter) {

    return getRegistrationFor(converter, Converter.class, isReading, isWriting);

   } else {
    throw new IllegalArgumentException("Unsupported converter type!");
   }
  }

  private Streamable<ConverterRegistration> getRegistrationFor(Object converter, Class<?> type, boolean isReading,
    boolean isWriting) {

   Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(converter.getClass(), type);
   return Streamable.of(register(arguments[0], arguments[1], isReading, isWriting));
  }

  private ConverterRegistration register(Class<?> source, Class<?> target, boolean isReading, boolean isWriting) {
   return register(new ConvertiblePair(source, target), isReading, isWriting);
  }

  private ConverterRegistration register(ConvertiblePair pair, boolean isReading, boolean isWriting) {
   return new ConverterRegistration(pair, this, isReading, isWriting);
  }

  private boolean isStoreSimpleType(Class<?> type) {
   return storeTypeHolder.isSimpleType(type);
  }
 }
}