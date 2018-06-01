/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.JsonSchemaFormat;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.json.JsonSchema.AbstractJsonSchemaProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.Definitions;
import org.springframework.data.rest.webmvc.json.JsonSchema.EnumProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.Item;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Converter to create {@link JsonSchema} instances for {@link PersistentEntity}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityToJsonSchemaConverter implements ConditionalGenericConverter {

 private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
 private static final TypeDescriptor SCHEMA_TYPE = TypeDescriptor.valueOf(JsonSchema.class);
 private static final TypeInformation<?> STRING_TYPE_INFORMATION = ClassTypeInformation.from(String.class);

 private final Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
 private final ResourceMappings mappings;
 private final PersistentEntities entities;
 private final MessageSourceAccessor accessor;
 private final ObjectMapper objectMapper;
 private final RepositoryRestConfiguration configuration;

 /**
  * Creates a new {@link PersistentEntityToJsonSchemaConverter} for the given {@link PersistentEntities} and
  * {@link ResourceMappings}.
  * 
  * @param entities must not be {@literal null}.
  * @param mappings must not be {@literal null}.
  * @param accessor must not be {@literal null}.
  * @param objectMapper must not be {@literal null}.
  * @param configuration must not be {@literal null}.
  */
 public PersistentEntityToJsonSchemaConverter(PersistentEntities entities, ResourceMappings mappings,
   MessageSourceAccessor accessor, ObjectMapper objectMapper, RepositoryRestConfiguration configuration) {

  Assert.notNull(entities, "PersistentEntities must not be null!");
  Assert.notNull(mappings, "ResourceMappings must not be null!");
  Assert.notNull(accessor, "MessageSourceAccessor must not be null!");
  Assert.notNull(objectMapper, "ObjectMapper must not be null!");
  Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

  this.entities = entities;
  this.mappings = mappings;
  this.accessor = accessor;
  this.objectMapper = objectMapper;
  this.configuration = configuration;

  for (TypeInformation<?> domainType : entities.getManagedTypes()) {
   convertiblePairs.add(new ConvertiblePair(domainType.getType(), JsonSchema.class));
  }
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
  */
 @Override
 public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
  return Class.class.isAssignableFrom(sourceType.getType())
    && JsonSchema.class.isAssignableFrom(targetType.getType());
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
  */
 @Override
 public Set<ConvertiblePair> getConvertibleTypes() {
  return convertiblePairs;
 }

 /**
  * Converts the given type into a {@link JsonSchema} instance.
  * 
  * @param domainType must not be {@literal null}.
  * @return
  */
 public JsonSchema convert(Class<?> domainType) {
  return (JsonSchema) convert(domainType, STRING_TYPE, SCHEMA_TYPE);
 }

 /*
  * (non-Javadoc)
  * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
  */
 @Override
 public JsonSchema convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

  final PersistentEntity<?, ?> persistentEntity = entities.getPersistentEntity((Class<?>) source);
  final ResourceMetadata metadata = mappings.getMetadataFor(persistentEntity.getType());

  Definitions descriptors = new Definitions();
  List<AbstractJsonSchemaProperty<?>> propertiesFor = getPropertiesFor(persistentEntity.getType(), metadata,
    descriptors);

  String title = resolveMessageWithDefault(new ResolvableType(persistentEntity.getType()));

  return new JsonSchema(title, resolveMessage(metadata.getItemResourceDescription()), propertiesFor, descriptors);
 }

 private List<AbstractJsonSchemaProperty<?>> getPropertiesFor(Class<?> type, final ResourceMetadata metadata,
   final Definitions descriptors) {

  final PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);
  final JacksonMetadata jackson = new JacksonMetadata(objectMapper, type);
  final AssociationLinks associationLinks = new AssociationLinks(mappings);

  if (entity == null) {
   return Collections.<AbstractJsonSchemaProperty<?>> emptyList();
  }

  JsonSchemaPropertyRegistrar registrar = new JsonSchemaPropertyRegistrar(jackson);

  for (BeanPropertyDefinition definition : jackson) {

   PersistentProperty<?> persistentProperty = entity.getPersistentProperty(definition.getInternalName());

   // First pass, early drops to avoid unnecessary calculation
   if (persistentProperty != null) {

    if (persistentProperty.isIdProperty() && !configuration.isIdExposedFor(type)) {
     continue;
    }

    if (persistentProperty.isVersionProperty()) {
     continue;
    }

    if (!definition.couldSerialize()) {
     continue;
    }
   }

   TypeInformation<?> propertyType = persistentProperty == null
     ? ClassTypeInformation.from(definition.getPrimaryMember().getRawType())
     : persistentProperty.getTypeInformation();
   TypeInformation<?> actualPropertyType = propertyType.getActualType();
   Class<?> rawPropertyType = propertyType.getType();

   JsonSchemaFormat format = configuration.getMetadataConfiguration().getSchemaFormatFor(rawPropertyType);
   ResourceDescription description = persistentProperty == null
     ? jackson.getFallbackDescription(metadata, definition) : getDescriptionFor(persistentProperty, metadata);
   JsonSchemaProperty property = getSchemaProperty(definition, propertyType, description);

   boolean isSyntheticProperty = persistentProperty == null;
   boolean isNotWritable = !isSyntheticProperty && !persistentProperty.isWritable();
   boolean isJacksonReadOnly = !isSyntheticProperty && jackson.isReadOnly(persistentProperty);

   if (isSyntheticProperty || isNotWritable || isJacksonReadOnly) {
    property = property.withReadOnly();
   }

   if (format != null) {

    // Types with explicitly registered format -> value object with format
    registrar.register(property.withFormat(format), actualPropertyType);
    continue;
   }

   Pattern pattern = configuration.getMetadataConfiguration().getPatternFor(rawPropertyType);

   if (pattern != null) {
    registrar.register(property.withPattern(pattern), actualPropertyType);
    continue;
   }

   if (jackson.isValueType()) {
    registrar.register(property.with(STRING_TYPE_INFORMATION), actualPropertyType);
    continue;
   }

   if (persistentProperty == null) {
    registrar.register(property, actualPropertyType);
    continue;
   }

   if (associationLinks.isLinkableAssociation(persistentProperty)) {
    registrar.register(property.asAssociation(), null);
   } else {

    if (persistentProperty.isEntity()) {

     if (!descriptors.hasDefinitionFor(propertyType)) {
      descriptors.addDefinition(propertyType,
        new Item(propertyType, getNestedPropertiesFor(persistentProperty, descriptors)));
     }

     registrar.register(property.with(propertyType, Definitions.getReference(propertyType)), actualPropertyType);

    } else {

     registrar.register(property.with(propertyType), actualPropertyType);
    }
   }
  }

  return registrar.getProperties();
 }

 private Collection<AbstractJsonSchemaProperty<?>> getNestedPropertiesFor(PersistentProperty<?> property,
   Definitions descriptors) {

  if (!property.isEntity()) {
   return Collections.emptyList();
  }

  return getPropertiesFor(property.getActualType(), mappings.getMetadataFor(property.getActualType()), descriptors);
 }

 private JsonSchemaProperty getSchemaProperty(BeanPropertyDefinition definition, TypeInformation<?> type,
   ResourceDescription description) {

  String name = definition.getName();
  String title = resolveMessageWithDefault(new ResolvableProperty(definition));
  String resolvedDescription = resolveMessage(description);
  boolean required = definition.isRequired();
  Class<?> rawType = type.getType();

  if (!rawType.isEnum()) {
   return new JsonSchemaProperty(name, title, resolvedDescription, required).with(type);
  }

  String message = resolveMessage(new DefaultMessageSourceResolvable(description.getMessage()));

  return new EnumProperty(name, title, rawType,
    description.getDefaultMessage().equals(resolvedDescription) ? message : resolvedDescription, required);
 }

 private ResourceDescription getDescriptionFor(PersistentProperty<?> property, ResourceMetadata metadata) {

  ResourceMapping propertyMapping = metadata.getMappingFor(property);
  return propertyMapping.getDescription();
 }

 private String resolveMessageWithDefault(MessageSourceResolvable resolvable) {
  return resolveMessage(new DefaultingMessageSourceResolvable(resolvable));
 }

 private String resolveMessage(MessageSourceResolvable resolvable) {

  if (resolvable == null) {
   return null;
  }

  try {
   return accessor.getMessage(resolvable);
  } catch (NoSuchMessageException o_O) {

   if (configuration.getMetadataConfiguration().omitUnresolvableDescriptionKeys()) {
    return null;
   } else {
    throw o_O;
   }
  }
 }

 /**
  * Helper to register {@link JsonSchemaProperty} instances after post-processing them.
  *
  * @author Oliver Gierke
  * @since 2.4
  */
 private static class JsonSchemaPropertyRegistrar {

  private final JacksonMetadata metadata;
  private final List<AbstractJsonSchemaProperty<?>> properties;

  /**
   * Creates a new {@link JsonSchemaPropertyRegistrar} using the given {@link JacksonMetadata}.
   * 
   * @param metadata must not be {@literal null}.
   */
  public JsonSchemaPropertyRegistrar(JacksonMetadata metadata) {

   Assert.notNull(metadata, "Metadata must not be null!");

   this.metadata = metadata;
   this.properties = new ArrayList<AbstractJsonSchemaProperty<?>>();
  }

  public void register(JsonSchemaProperty property, TypeInformation<?> type) {

   if (type == null) {
    properties.add(property);
    return;
   }

   JsonSerializer<?> serializer = metadata.getTypeSerializer(type.getType());

   if (!(serializer instanceof JsonSchemaPropertyCustomizer)) {
    properties.add(property);
    return;
   }

   properties.add(((JsonSchemaPropertyCustomizer) serializer).customize(property, type));
  }

  public List<AbstractJsonSchemaProperty<?>> getProperties() {
   return properties;
  }
 }

 /**
  * Message source resolvable that defaults the messages to the last segment of the dot-separated code in case the
  * configured delegate doesn't return a default message itself.
  *
  * @author Oliver Gierke
  * @since 2.4
  */
 private static class DefaultingMessageSourceResolvable implements MessageSourceResolvable {

  private static Pattern SPLIT_CAMEL_CASE = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

  private final MessageSourceResolvable delegate;

  /**
   * Creates a new {@link DefaultingMessageSourceResolvable} for the given delegate {@link MessageSourceResolvable}.
   * 
   * @param delegate must not be {@literal null}.
   */
  public DefaultingMessageSourceResolvable(MessageSourceResolvable delegate) {
   this.delegate = delegate;
  }

  /* 
   * (non-Javadoc)
   * @see org.springframework.context.MessageSourceResolvable#getArguments()
   */
  @Override
  public Object[] getArguments() {
   return delegate.getArguments();
  }

  /* 
   * (non-Javadoc)
   * @see org.springframework.context.MessageSourceResolvable#getCodes()
   */
  @Override
  public String[] getCodes() {
   return delegate.getCodes();
  }

  /* 
   * (non-Javadoc)
   * @see org.springframework.context.MessageSourceResolvable#getDefaultMessage()
   */
  @Override
  public String getDefaultMessage() {

   String defaultMessage = delegate.getDefaultMessage();

   if (defaultMessage != null) {
    return defaultMessage;
   }

   String[] split = getCodes()[0].split("\\.");
   String tail = split[split.length - 1];
   tail = "_title".equals(tail) ? split[split.length - 2] : tail;

   return StringUtils.capitalize(StringUtils
     .collectionToDelimitedString(Arrays.asList(SPLIT_CAMEL_CASE.split(tail)), " ").toLowerCase(Locale.US));
  }
 }

 /**
  * A {@link BeanPropertyDefinition} that can be resolved via a {@link MessageSource}.
  *
  * @author Oliver Gierke
  * @since 2.4.1
  */
 private static class ResolvableProperty extends DefaultMessageSourceResolvable {

  private static final long serialVersionUID = -5603381674553244480L;

  /**
   * Creates a new {@link ResolvableProperty} for the given {@link BeanPropertyDefinition}.
   * 
   * @param property must not be {@literal null}.
   */
  public ResolvableProperty(BeanPropertyDefinition property) {
   super(getCodes(property));
  }

  private static String[] getCodes(BeanPropertyDefinition property) {

   Assert.notNull(property, "BeanPropertyDefinition must not be null!");

   Class<?> owner = property.getPrimaryMember().getDeclaringClass();

   String propertyTitle = property.getInternalName().concat("._title");
   String localName = owner.getSimpleName().concat(".").concat(propertyTitle);
   String fullName = owner.getName().concat(".").concat(propertyTitle);

   return new String[] { fullName, localName, propertyTitle };
  }
 }

 /**
  * A type whose title can be resolved through a {@link MessageSource}.
  *
  * @author Oliver Gierke
  * @since 2.4.1
  */
 private static class ResolvableType extends DefaultMessageSourceResolvable {

  private static final long serialVersionUID = -7199875272753949857L;

  /**
   * Creates a new {@link ResolvableType} for the given type.
   * 
   * @param type must not be {@literal null}.
   */
  public ResolvableType(Class<?> type) {
   super(getTitleCodes(type));
  }

  private static String[] getTitleCodes(Class<?> type) {

   Assert.notNull(type, "Type must not be null!");

   return new String[] { type.getName().concat("._title"), type.getSimpleName().concat("._title") };
  }
 }
}