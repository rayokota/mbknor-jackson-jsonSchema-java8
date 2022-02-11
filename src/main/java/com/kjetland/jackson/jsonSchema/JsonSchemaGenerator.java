package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.*;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaBool;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInt;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaString;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JsonSchemaGenerator {
    
    final ObjectMapper objectMapper;
    final JsonSchemaConfig config;

    /**
     * JSON Schema Generator.
     * @param rootObjectMapper pre-configured ObjectMapper
     */
    public JsonSchemaGenerator(ObjectMapper rootObjectMapper) {
        this(rootObjectMapper, JsonSchemaConfig.DEFAULT);
    }

    /**
     * JSON Schema Generator.
     * @param rootObjectMapper pre-configured ObjectMapper
     * @param config by default, {@link JsonSchemaConfig#DEFAULT}. 
     *     Use {@link JsonSchemaConfig#JSON_EDITOR} for {@link https://github.com/jdorn/json-editor JSON GUI}.
     */
    public JsonSchemaGenerator(ObjectMapper rootObjectMapper, JsonSchemaConfig config) {
        this.objectMapper = rootObjectMapper;
        this.config = config;
    }

    public JsonNode generateJsonSchema(Class<?> clazz) throws JsonMappingException { 
        return generateJsonSchema(clazz, null, null);
    }
    
    public JsonNode generateJsonSchema(JavaType javaType) throws JsonMappingException { 
        return generateJsonSchema(javaType, null, null); 
    }
    
    public JsonNode generateJsonSchema(Class<?> clazz, String title, String description) throws JsonMappingException {

        Class<?> clazzToUse = tryToReMapType(clazz);

        JavaType javaType = objectMapper.constructType(clazzToUse);

        return generateJsonSchema(javaType, title, description);
    }

    public JsonNode generateJsonSchema(JavaType javaType, String title, String description) throws JsonMappingException {

        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        rootNode.put("$schema", config.jsonSchemaDraft.url);

        if (title == null)
            title = Utils.camelCaseToSentenceCase(javaType.getRawClass().getSimpleName());
        if (!title.isEmpty())
            // If root class is annotated with @JsonSchemaTitle, it will later override this title
            rootNode.put("title", title);

        if (description != null)
            // If root class is annotated with @JsonSchemaDescription, it will later override this description
            rootNode.put("description", description);


        DefinitionsHandler definitionsHandler = new DefinitionsHandler(config);
        JsonSchemaGeneratorVisitor rootVisitor = new JsonSchemaGeneratorVisitor(this, 0, rootNode, definitionsHandler, null);


        objectMapper.acceptJsonFormatVisitor(javaType, rootVisitor);

        ObjectNode definitionsNode = definitionsHandler.getFinalDefinitionsNode();
        if (definitionsNode != null)
            rootNode.set("definitions", definitionsNode);

        return rootNode;
    }
    

    JavaType tryToReMapType(JavaType originalType) {
        Class<?> mappedToClass = config.classTypeReMapping.get(originalType.getRawClass());
        if (mappedToClass != null) {
            log.trace("Class {} is remapped to {}", originalType, mappedToClass);
            return objectMapper.getTypeFactory().constructType(mappedToClass);
        }
        else
            return originalType;
    }
    
    Class<?> tryToReMapType(Class<?> originalClass) {
        Class<?> mappedToClass = config.classTypeReMapping.get(originalClass);
        if (mappedToClass != null) {
            log.trace("Class {} is remapped to {}", originalClass, mappedToClass);
            return mappedToClass;
        }
        else
            return originalClass;
    }

    String  resolvePropertyFormat(JavaType type) {
        DeserializationConfig omConfig = objectMapper.getDeserializationConfig();
        AnnotatedClass annotatedClass = AnnotatedClassResolver.resolve(omConfig, type, omConfig);
        JsonSchemaFormat annotation = annotatedClass.getAnnotation(JsonSchemaFormat.class);
        if (annotation != null)
            return annotation.value();
        
        String rawClassName = type.getRawClass().getName();
        return config.customType2FormatMapping.get(rawClassName);
    }

    String resolvePropertyFormat(BeanProperty prop) {
        JsonSchemaFormat annotation = prop.getAnnotation(JsonSchemaFormat.class);
        if (annotation != null)
            return annotation.value();
        
        String rawClassName = prop.getType().getRawClass().getName();
        return config.customType2FormatMapping.get(rawClassName);
    }
    
    /** Tries to retrieve an annotation and validates that it is applicable. */
    <T extends Annotation> T selectAnnotation(BeanProperty prop, Class<T> annotationClass) {
        if (prop == null)
            return null;
       T ann = prop.getAnnotation(annotationClass);
        if (ann == null || !annotationIsApplicable(ann))
            return null;
        return ann;
    }

    <T extends Annotation> T selectAnnotation(AnnotatedClass annotatedClass, Class<T> annotationClass) {
        T ann = annotatedClass.getAnnotation(annotationClass);
        if (ann == null || !annotationIsApplicable(ann))
            return null;
        return ann;
    }

    // Checks to see if a javax.validation field that makes our field required is present.
    boolean validationAnnotationRequired(BeanProperty prop) {
        return selectAnnotation(prop, NotNull.class) != null
                || selectAnnotation(prop, NotBlank.class) != null
                || selectAnnotation(prop, NotEmpty.class) != null;
    }
    
    // Checks to see if a javax.validation field that makes our field nullable.
    public boolean hasNullableAnnotation(BeanProperty prop)  {
        AnnotationMap annotations = prop.getMember().getAllAnnotations();
        for (Annotation annotation : annotations.annotations()) {
            String annotationType = annotation.annotationType().getSimpleName();
            if (annotationType .equals ("Nullable"))
                return true;
        }
        return false;
    }

    // Checks to see if a javax.validation field that makes our field not null.
    public boolean hasNotNullAnnotation(BeanProperty prop)  {
    	AnnotationMap annotations = prop.getMember().getAllAnnotations();
        for (Annotation annotation : annotations.annotations()) {
            String annotationType = annotation.annotationType().getSimpleName();
            if (annotationType .equals ("NonNull")
                    || annotationType .equals ("Nonnull")
                    || annotationType .equals ("NotNull")
                    || selectAnnotation(prop, NotBlank.class) != null
                    || selectAnnotation(prop, NotEmpty.class) != null)
                return true;
        }
        return false;
    }

    /** Verifies that the annotation is applicable based on the config.javaxValidationGroups. */
    boolean annotationIsApplicable(Annotation annotation) {
        List<Class<?>> desiredGroups = config.javaxValidationGroups;
        if (desiredGroups == null || desiredGroups.isEmpty())
            desiredGroups = Collections.unmodifiableList( Arrays.asList(Default.class) );

        List<Class<?>> annotationGroups = Utils.extractGroupsFromAnnotation(annotation);
        if (annotationGroups.isEmpty())
            annotationGroups = Collections.unmodifiableList( Arrays.asList(Default.class) );

        for (Class<?> group : annotationGroups)
            if (desiredGroups.contains (group))
                return true;
        return false;
    }
    
    TypeSerializer getTypeSerializer(JavaType baseType) throws JsonMappingException {

        return objectMapper
                .getSerializerFactory()
                .createTypeSerializer(objectMapper.getSerializationConfig(), baseType);
    }
    
    
    /**
     * @returns the value of merge
     */
    boolean injectFromAnnotation(ObjectNode node, JsonSchemaInject injectAnnotation) throws JsonMappingException {
        // Must parse json
        JsonNode injectedNode;
        try {
            injectedNode = objectMapper.readTree(injectAnnotation.json());
        }
        catch(JsonProcessingException e) {
            throw new JsonMappingException("Could not parse JsonSchemaInject.json", e);
        }
        
        // Apply the JSON supplier (may be a no-op)
        try {
            Supplier<JsonNode> jsonSupplier = injectAnnotation.jsonSupplier().newInstance();
            JsonNode jsonNode = jsonSupplier.get();
            if (jsonNode != null)
                Utils.merge (injectedNode, jsonNode);
        }
        catch (InstantiationException|IllegalAccessException e) {
            throw new JsonMappingException("Could not call JsonSchemaInject.jsonSupplier constructor", e);
        }
        
        // Apply the JSON-supplier-via-lookup
        if (!injectAnnotation.jsonSupplierViaLookup().isEmpty()) {
        	Supplier<JsonNode> jsonSupplier = config.jsonSuppliers.get(injectAnnotation.jsonSupplierViaLookup());
            if (jsonSupplier == null)
                throw new JsonMappingException("@JsonSchemaInject(jsonSupplierLookup='"+injectAnnotation.jsonSupplierViaLookup()+"') does not exist in ctx.config.jsonSupplierLookup-map");
            JsonNode jsonNode = jsonSupplier.get();
            if (jsonNode != null)
                Utils.merge(injectedNode, jsonNode);
        }
        
        // 
        for (JsonSchemaString v : injectAnnotation.strings())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));
        for (JsonSchemaInt v : injectAnnotation.ints())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));
        for (JsonSchemaBool v : injectAnnotation.bools())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));

        boolean injectOverridesAll = injectAnnotation.overrideAll();
        if (injectOverridesAll) {
          // Since we're not merging, we must remove all content of thisObjectNode before injecting.
          // We cannot just "replace" it with injectJsonNode, since thisObjectNode already have been added to its parent
          node.removeAll();
        }

        Utils.merge(node, injectedNode);

        return injectOverridesAll;
    }
}
