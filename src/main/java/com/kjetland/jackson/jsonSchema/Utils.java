package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alex
 */
@Slf4j
public final class Utils {
    
    private Utils() {}
    
    public static String extractMinimalClassnameId(JavaType baseType, JavaType child) {
        // code taken from Jackson's MinimalClassNameIdResolver constructor and method idFromValue
        
        String base = baseType.getRawClass().getName();
        int ix = base.lastIndexOf('.');
        
        String basePackagePrefix;
        if (ix < 0) // can this ever occur?
            basePackagePrefix = ".";
        else
            basePackagePrefix = base.substring(0, ix + 1);
        
        String n = child.getRawClass().getName();
        if (n.startsWith(basePackagePrefix)) { // note: we will leave the leading dot in there
            return n.substring(basePackagePrefix.length() - 1);
        } else {
            return n;
        }
    }
    
    
    public static void merge(JsonNode mainNode, JsonNode updateNode) {
        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, updateNode.get(fieldName));
            }
            else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode)mainNode).set(fieldName, value);
                }
            }
        }
    }
    

    public static void visit(JsonNode o, String path, BiConsumer<ObjectNode, String> f) {
        String[] parts = path.split(Pattern.quote("/"));
        String lastPart = parts[parts.length - 1];
        String[] otherParts = Arrays.copyOfRange(parts, 0, parts.length - 1);
        JsonNode p = o;
        for (String name : otherParts) {
            JsonNode child = p.get(name);
            if (child == null)
                child = ((ObjectNode)p).putObject(name);
            p = child;
        }
        f.accept((ObjectNode)p, lastPart);
    }

    public static String camelCaseToSentenceCase(String propertyName) {
        // Code found here: http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
        String s = propertyName.replaceAll(
            "(?<=[A-Z])(?=[A-Z][a-z])"
            + "|(?<=[^A-Z])(?=[A-Z])"
            + "|(?<=[A-Za-z])(?=[^A-Za-z])",
            " ");

        // Make the first letter uppercase
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static JavaType resolveElementType(JavaType propertyType, BeanProperty prop, ObjectMapper objectMapper) {
        JavaType containedType = propertyType.containedType(0);
        if (containedType.getRawClass() == Object.class) {
            // Scala BS
            // https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges
        	JsonDeserialize jsonDeserialize = prop.getAnnotation(JsonDeserialize.class);
            if (jsonDeserialize != null)
                return objectMapper.getTypeFactory().constructType(jsonDeserialize.contentAs());
            else {
                log.debug("Use @JsonDeserialize(contentAs=, keyAs=) to specify type of collection elements of {}", prop);
                return containedType;
            }
        } 
        else {
          // use containedType as is
          return containedType;
        }
    }
    
    public static ArrayNode getRequiredArrayNode(ObjectNode objectNode) {
        JsonNode requiredNode = objectNode.get("required");
        
        if (requiredNode == null) {
            requiredNode = JsonNodeFactory.instance.arrayNode();
            objectNode.set("required", requiredNode);
        }
        
        return (ArrayNode) requiredNode;
    }

    public static ObjectNode getOptionsNode(ObjectNode objectNode) {
        return getOrCreateObjectChild(objectNode, "options");
    }

    public static ObjectNode getOrCreateObjectChild(ObjectNode parentObjectNode, String name) {
        JsonNode childNode = parentObjectNode.get(name);
        
        if (childNode == null) {
            childNode = JsonNodeFactory.instance.objectNode();
            parentObjectNode.set(name, childNode);
        }
        
        return (ObjectNode) childNode;
    }
    

    public static String extractTypeName(JavaType type) {
        // use JsonTypeName annotation if present
    	JsonTypeName annotation = type.getRawClass().getDeclaredAnnotation(JsonTypeName.class);
        return Optional.ofNullable(annotation)
                .flatMap(a -> Optional.of(a.value()))
                .filter(a -> !a.isEmpty())
                .orElse(type.getRawClass().getSimpleName());
    }
    
    public static List<Class<?>> extractGroupsFromAnnotation(Annotation annotation) {
        // Annotations cannot implement interface, so we have to check each and every
        // javax-annotation... To prevent bugs with missing groups-extract-impl when new
        // validation-annotations are added, I've decided to do it using reflection
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        List<Class<?>> arr= new ArrayList<>();
        if (annotationClass.getPackage().getName().startsWith("javax.validation.constraints")) {
            try {
                Class<?>[] groups = (Class<?>[]) annotationClass.getMethod("groups").invoke(annotation);
                arr = Arrays.asList(groups);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)  {
            }

        } 
        else if (annotation instanceof JsonSchemaInject)
        	arr=Arrays.asList(((JsonSchemaInject)annotation).javaxValidationGroups());

        return Collections.unmodifiableList(arr);
    }
    
    public static JavaType getSuperClass(JavaType type) {
        for (JavaType superType : ClassUtil.findSuperTypes(type, null, false))
            if (superType.getRawClass().isAnnotationPresent(JsonTypeInfo.class))
                return superType;
        // else
        return type.getSuperClass();
    }
}
