package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static com.kjetland.jackson.jsonSchema.TestUtils.*;
import com.kjetland.jackson.jsonSchema.testData.BoringContainer;
import com.kjetland.jackson.jsonSchema.testData.ClassNotExtendingAnything;
import com.kjetland.jackson.jsonSchema.testData.ClassUsingValidationWithGroups;
import com.kjetland.jackson.jsonSchema.testData.ClassUsingValidationWithGroups.ValidationGroup1;
import com.kjetland.jackson.jsonSchema.testData.ClassUsingValidationWithGroups.ValidationGroup2;
import com.kjetland.jackson.jsonSchema.testData.ClassUsingValidationWithGroups.ValidationGroup3_notInUse;
import com.kjetland.jackson.jsonSchema.testData.MyEnum;
import com.kjetland.jackson.jsonSchema.testData.PojoUsingJsonTypeName;
import com.kjetland.jackson.jsonSchema.testData.PojoWithArrays;
import com.kjetland.jackson.jsonSchema.testData.PojoWithCustomSerializer;
import com.kjetland.jackson.jsonSchema.testData.PojoWithCustomSerializerDeserializer;
import com.kjetland.jackson.jsonSchema.testData.PojoWithCustomSerializerSerializer;
import com.kjetland.jackson.jsonSchema.testData.PojoWithParent;
import com.kjetland.jackson.jsonSchema.testData.PolymorphismOrdering;
import com.kjetland.jackson.jsonSchema.testData.TestData;
import com.kjetland.jackson.jsonSchema.testData.UsingJsonSchemaInjectTop.*;
import com.kjetland.jackson.jsonSchema.testData.generic.GenericClassContainer;
import com.kjetland.jackson.jsonSchema.testData.mixin.MixinModule;
import com.kjetland.jackson.jsonSchema.testData.mixin.MixinParent;
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1;
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.Parent;
import com.kjetland.jackson.jsonSchema.testData.polymorphism2.Parent2;
import com.kjetland.jackson.jsonSchema.testData.polymorphism3.Parent3;
import com.kjetland.jackson.jsonSchema.testData.polymorphism4.Child41;
import com.kjetland.jackson.jsonSchema.testData.polymorphism4.Child42;
import com.kjetland.jackson.jsonSchema.testData.polymorphism5.Parent5;
import com.kjetland.jackson.jsonSchema.testData.polymorphism6.Parent6;
import com.kjetland.jackson.jsonSchema.testData_issue_24.EntityWrapper;
import static java.lang.System.out;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.groups.Default;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author alex
 */
public class JsonSchemaGeneratorTest {

    ObjectMapper objectMapper = new ObjectMapper();
    MixinModule mixinModule = new MixinModule();

    {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(PojoWithCustomSerializer.class, new PojoWithCustomSerializerSerializer());
        simpleModule.addDeserializer(PojoWithCustomSerializer.class, new PojoWithCustomSerializerDeserializer());
        objectMapper.registerModule(simpleModule);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JodaModule());

        // For the mixin-test
        objectMapper.registerModule(mixinModule);

        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setTimeZone(TimeZone.getDefault());
    }

    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    JsonSchemaGenerator jsonSchemaGeneratorNullable = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.NULLABLE);
    JsonSchemaGenerator jsonSchemaGeneratorWithIds = new JsonSchemaGenerator(objectMapper, 
        JsonSchemaConfig.builder().useTypeIdForDefinitionName(true).build());
    JsonSchemaGenerator jsonSchemaGeneratorWithIdsNullable = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.NULLABLE.toBuilder().useTypeIdForDefinitionName(true).build());
    JsonSchemaGenerator jsonSchemaGeneratorHTML5 = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.JSON_EDITOR);
    JsonSchemaGenerator jsonSchemaGeneratorHTML5Nullable = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.JSON_EDITOR.toBuilder().useOneOfForNullables(true).build());
    JsonSchemaGenerator jsonSchemaGenerator_draft_06 = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.builder().jsonSchemaDraft(JsonSchemaDraft.DRAFT_06).build());
    JsonSchemaGenerator jsonSchemaGenerator_draft_07 = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.builder().jsonSchemaDraft(JsonSchemaDraft.DRAFT_07).build());
    JsonSchemaGenerator jsonSchemaGenerator_draft_2019_09 = new JsonSchemaGenerator(objectMapper,
        JsonSchemaConfig.builder().jsonSchemaDraft(JsonSchemaDraft.DRAFT_2019_09).build());

    TestData testData = new TestData();

    
    @Test void generateSchemaForPojo() {

    	List<String> enumList = Collections.unmodifiableList( Arrays.asList(MyEnum.values()).stream().map(Object::toString).collect(Collectors.toList()) );

        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass(), jsonNode);

            assertTrue (!schema.at("/additionalProperties").asBoolean());
            assertEquals (schema.at("/properties/someString/type").asText(), "string");

            assertEquals (schema.at("/properties/myEnum/type").asText(), "string");
            assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")), enumList);
        }

        {
          JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything);
          JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything.getClass(), jsonNode);

          assertTrue (!schema.at("/additionalProperties").asBoolean());
          assertNullableType(schema, "/properties/someString", "string");

          assertNullableType(schema, "/properties/myEnum", "string");
          assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/oneOf/1/enum")), enumList);
        }

        {
          JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.genericClassVoid);
          JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.genericClassVoid.getClass(), jsonNode);
          assertEquals (schema.at("/type").asText(), "object");
          assertTrue (!schema.at("/additionalProperties").asBoolean());
          assertEquals (schema.at("/properties/content/type").asText(), "null");
          assertEquals (schema.at("/properties/list/type").asText(), "array");
          assertEquals (schema.at("/properties/list/items/type").asText(), "null");
        }
        {
          JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.genericMapLike);
          JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.genericMapLike.getClass(), jsonNode);
          assertEquals (schema.at("/type").asText(), "object");
          assertEquals (schema.at("/additionalProperties/type").asText(), "string");
        }
    }
    
    @Test void generatingSchemaForPojoWithJsonTypeInfo() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child1);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.child1.getClass(), jsonNode);

        assertTrue (!schema.at("/additionalProperties").asBoolean());
        assertEquals (schema.at("/properties/parentString/type").asText(), "string");
        assertJsonSubTypesInfo(schema, "type", "child1");
    }
    
    @Test void generateSchemaForPropertyWithJsonTypeInfo() {

        // Java
        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithParent);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithParent.getClass(), jsonNode);

            assertTrue(!schema.at("/additionalProperties").asBoolean());
            assertEquals(schema.at("/properties/pojoValue/type").asText(), "boolean");
            assertDefaultValues(schema);

            assertChild1(schema, "/properties/child/oneOf");
            assertChild2(schema, "/properties/child/oneOf");
        }

        // Java - html5
        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoWithParent);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoWithParent.getClass(), jsonNode);

            assertTrue(!schema.at("/additionalProperties").asBoolean());
            assertEquals(schema.at("/properties/pojoValue/type").asText(), "boolean");
            assertDefaultValues(schema);

            assertChild1(schema, "/properties/child/oneOf", true);
            assertChild2(schema, "/properties/child/oneOf", true);
        }

        // Java - html5/nullable
        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.pojoWithParent);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoWithParent.getClass(), jsonNode);

            assertTrue(!schema.at("/additionalProperties").asBoolean());
            assertNullableType(schema, "/properties/pojoValue", "boolean");
            assertNullableDefaultValues(schema);

            assertNullableChild1(schema, "/properties/child/oneOf/1/oneOf", true);
            assertNullableChild2(schema, "/properties/child/oneOf/1/oneOf", true);
        }

        //Using fully-qualified class names;
        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorWithIds, testData.pojoWithParent);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorWithIds, testData.pojoWithParent.getClass(), jsonNode);

            assertTrue(!schema.at("/additionalProperties").asBoolean());
            assertEquals(schema.at("/properties/pojoValue/type").asText(), "boolean");
            assertDefaultValues(schema);

            assertChild1(schema, "/properties/child/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1");
            assertChild2(schema, "/properties/child/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child2");
        }

        // Using fully-qualified class names and nullable types
        {
            JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorWithIdsNullable, testData.pojoWithParent);
            JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorWithIdsNullable, testData.pojoWithParent.getClass(), jsonNode);

            assertTrue(!schema.at("/additionalProperties").asBoolean());
            assertNullableType(schema, "/properties/pojoValue", "boolean");
            assertNullableDefaultValues(schema);

            assertNullableChild1(schema, "/properties/child/oneOf/1/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1");
            assertNullableChild2(schema, "/properties/child/oneOf/1/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child2");
        }
    }
    
    
    void assertDefaultValues(JsonNode schema) {
        assertEquals (schema.at("/properties/stringWithDefault/type").asText(), "string");
        assertEquals (schema.at("/properties/stringWithDefault/default").asText(), "x");
        assertEquals (schema.at("/properties/intWithDefault/type").asText(), "integer");
        assertEquals (schema.at("/properties/intWithDefault/default").asInt(), 12);
        assertEquals (schema.at("/properties/booleanWithDefault/type").asText(), "boolean");
        assertTrue (schema.at("/properties/booleanWithDefault/default").asBoolean());
    };

    void assertNullableDefaultValues(JsonNode schema) {
        assertEquals (schema.at("/properties/stringWithDefault/oneOf/0/type").asText(), "null");
        assertEquals (schema.at("/properties/stringWithDefault/oneOf/0/title").asText(), "Not included");
        assertEquals (schema.at("/properties/stringWithDefault/oneOf/1/type").asText(), "string");
        assertEquals (schema.at("/properties/stringWithDefault/oneOf/1/default").asText(), "x");

        assertEquals (schema.at("/properties/intWithDefault/type").asText(), "integer");
        assertEquals (schema.at("/properties/intWithDefault/default").asInt(), 12);
        assertEquals (schema.at("/properties/booleanWithDefault/type").asText(), "boolean");
        assertTrue (schema.at("/properties/booleanWithDefault/default").asBoolean());
    };
    
    void assertChild1(JsonNode node, String path) {
        assertChild1(node, path, "Child1", "type", "child1", false);
    }
    void assertChild1(JsonNode node, String path, boolean html5Checks) {
        assertChild1(node, path, "Child1", "type", "child1", html5Checks);
    }
    void assertChild1(JsonNode node, String path, String defName) {
        assertChild1(node, path, defName, "type", "child1", false);
    }
    void assertChild1(JsonNode node, String path, String defName, String typeParamName, String typeName) {
        assertChild1(node, path, defName, typeParamName, typeName, false);
    }
    void assertChild1(JsonNode node, String path, String defName, String typeParamName, String typeName, boolean html5Checks) {
        JsonNode child1 = getNodeViaRefs(node, path, defName);
        assertJsonSubTypesInfo(child1, typeParamName, typeName, html5Checks);
        assertEquals (child1.at("/properties/parentString/type").asText(), "string");
        assertEquals (child1.at("/properties/child1String/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String2/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");
        assertPropertyRequired(child1, "_child1String3", true);
    }

    void assertNullableChild1(JsonNode node, String path) {
        assertNullableChild1(node, path, "Child1", false);
    }
    void assertNullableChild1(JsonNode node, String path, boolean html5Checks) {
        assertNullableChild1(node, path, "Child1", html5Checks);
    }
    void assertNullableChild1(JsonNode node, String path, String defName) {
        assertNullableChild1(node, path, defName, false);
    }
    void assertNullableChild1(JsonNode node, String path, String defName, boolean html5Checks) {
        JsonNode child1 = getNodeViaRefs(node, path, defName);
        assertJsonSubTypesInfo(child1, "type", "child1", html5Checks);
        assertNullableType(child1, "/properties/parentString", "string");
        assertNullableType(child1, "/properties/child1String", "string");
        assertNullableType(child1, "/properties/_child1String2", "string");
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");
        assertPropertyRequired(child1, "_child1String3", true);
    }

    void assertChild2(JsonNode node, String path) {
        assertChild2(node, path, "Child2", "type", "child2", false);
    }
    void assertChild2(JsonNode node, String path, boolean html5Checks) {
        assertChild2(node, path, "Child2", "type", "child2", html5Checks);
    }
    void assertChild2(JsonNode node, String path, String defName) {
        assertChild2(node, path, defName, "type", "child2", false);
    }
    void assertChild2(JsonNode node, String path, String defName, String typeParamName, String typeName) {
        assertChild2(node, path, defName, typeParamName, typeName, false);
    }
    void assertChild2(JsonNode node, String path, String defName, String typeParamName, String typeName, boolean html5Checks) {
        JsonNode child2 = getNodeViaRefs(node, path, defName);
        assertJsonSubTypesInfo(child2, typeParamName, typeName, html5Checks);
        assertEquals (child2.at("/properties/parentString/type").asText(), "string");
        assertEquals (child2.at("/properties/child2int/type").asText(), "integer");
    }

    void assertNullableChild2(JsonNode node, String path) {
        assertNullableChild2(node, path, "Child2", false);
    }
    void assertNullableChild2(JsonNode node, String path, boolean html5Checks) {
        assertNullableChild2(node, path, "Child2", html5Checks);
    }
    void assertNullableChild2(JsonNode node, String path, String defName) {
        assertNullableChild2(node, path, defName, false);
    }
    void assertNullableChild2(JsonNode node, String path, String defName, boolean html5Checks) {
        JsonNode child2 = getNodeViaRefs(node, path, defName);
        assertJsonSubTypesInfo(child2, "type", "child2", html5Checks);
        assertNullableType(child2, "/properties/parentString", "string");
        assertNullableType(child2, "/properties/child2int", "integer");
    }
    
    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_use_IdNAME() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child1);
        assertToFromJson(jsonSchemaGenerator, testData.child1, Parent.class);

        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, Parent.class, jsonNode);

        assertChild1(schema, "/oneOf");
        assertChild2(schema, "/oneOf");
    }
    
    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_use_IdNAME_Nullables() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.child1);
        assertToFromJson(jsonSchemaGeneratorNullable, testData.child1, Parent.class);

        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, Parent.class, jsonNode);

        assertChild1(schema, "/oneOf");
        assertChild2(schema, "/oneOf");
    }

    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_use_IdCLASS() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator g = new JsonSchemaGenerator(objectMapper, config);

        JsonNode jsonNode = assertToFromJson(g, testData.child21);
        assertToFromJson(g, testData.child21, Parent2.class);

        JsonNode schema = generateAndValidateSchema(g, Parent2.class, jsonNode);

        assertChild1(schema, "/oneOf", "Child21", "clazz", "com.kjetland.jackson.jsonSchema.testData.polymorphism2.Child21");
        assertChild2(schema, "/oneOf", "Child22", "clazz", "com.kjetland.jackson.jsonSchema.testData.polymorphism2.Child22");
    }

    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_use_IdMINIMALCLASS() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator g = new JsonSchemaGenerator(objectMapper, config);

        JsonNode jsonNode = assertToFromJson(g, testData.child51);
        assertToFromJson(g, testData.child51, Parent5.class);

        JsonNode schema = generateAndValidateSchema(g, Parent5.class, jsonNode);

        assertChild1(schema, "/oneOf", "Child51", "clazz", ".Child51");
        assertChild2(schema, "/oneOf", "Child52", "clazz", ".Child52");

        String embeddedTypeName = objectMapper.valueToTree(new Parent5.Child51InnerClass()).get("clazz").asText();
        assertChild1(schema, "/oneOf", "Child51InnerClass", "clazz", embeddedTypeName);
    }

    @Test void generateSchemaForInterfaceAnnotatedWithJsonTypeInfo_use_IdMINIMALCLASS() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator g = new JsonSchemaGenerator(objectMapper, config);

        JsonNode jsonNode = assertToFromJson(g, testData.child61);
        assertToFromJson(g, testData.child61, Parent6.class);

        JsonNode schema = generateAndValidateSchema(g, Parent6.class, jsonNode);

        assertChild1(schema, "/oneOf", "Child61", "clazz", ".Child61");
        assertChild2(schema, "/oneOf", "Child62", "clazz", ".Child62");
    }

    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_include_AsEXISTINGPROPERTY() {

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child31);
        assertToFromJson(jsonSchemaGenerator, testData.child31, Parent3.class);

        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, Parent3.class, jsonNode);

        assertChild1(schema, "/oneOf", "Child31", "type", "child31");
        assertChild2(schema, "/oneOf", "Child32", "type", "child32");
    }

    @Test void generateSchemaForSuperClassAnnotatedWithJsonTypeInfo_include_AsCUSTOM() {

        JsonNode jsonNode1 = assertToFromJson(jsonSchemaGenerator, testData.child41);
        JsonNode jsonNode2 = assertToFromJson(jsonSchemaGenerator, testData.child42);

        JsonNode schema1 = generateAndValidateSchema(jsonSchemaGenerator, Child41.class, jsonNode1);
        JsonNode schema2 = generateAndValidateSchema(jsonSchemaGenerator, Child42.class, jsonNode2);

        assertJsonSubTypesInfo(schema1, "type", "Child41");
        assertJsonSubTypesInfo(schema2, "type", "Child42");
    }

    @Test void generateSchemaForClassContainingGenericsWithSameBaseTypeButDifferentTypeArguments() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator g = new JsonSchemaGenerator(objectMapper, config);

        GenericClassContainer instance = new GenericClassContainer();
        JsonNode jsonNode = assertToFromJson(g, instance);
        assertToFromJson(g, instance, GenericClassContainer.class);

        JsonNode schema = generateAndValidateSchema(g, GenericClassContainer.class, jsonNode);

        assertEquals (schema.at("/definitions/BoringClass/properties/data/type").asText(), "integer");
        assertEquals (schema.at("/definitions/GenericClass(String)/properties/data/type").asText(), "string");
        assertEquals (schema.at("/definitions/GenericWithJsonTypeName(String)/properties/data/type").asText(), "string");
        assertEquals (schema.at("/definitions/GenericClass(BoringClass)/properties/data/$ref").asText(), "#/definitions/BoringClass");
        assertEquals (schema.at("/definitions/GenericClassTwo(String,GenericClass(BoringClass))/properties/data1/type").asText(), "string");
        assertEquals (schema.at("/definitions/GenericClassTwo(String,GenericClass(BoringClass))/properties/data2/$ref").asText(), "#/definitions/GenericClass(BoringClass)");
    }

    @Test void failOnUnknownProperties() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.manyPrimitives);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.manyPrimitives.getClass(), jsonNode);

        assertFalse (schema.at("/additionalProperties").asBoolean());
    }

    @Test void failOnUnknownPropertiesOff() {
    	JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, 
                JsonSchemaConfig.DEFAULT.toBuilder().failOnUnknownProperties(false).build());
        JsonNode jsonNode = assertToFromJson(generator, testData.manyPrimitives);
        JsonNode schema = generateAndValidateSchema(generator, testData.manyPrimitives.getClass(), jsonNode);

        assertTrue (schema.at("/additionalProperties").asBoolean());
    }

    @Test void primitives() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.manyPrimitives);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.manyPrimitives.getClass(), jsonNode);

        assertEquals (schema.at("/properties/_string/type").asText(), "string");

        assertEquals (schema.at("/properties/_integer/type").asText(), "integer");
        assertPropertyRequired(schema, "_integer", false); // Should allow null by default

        assertEquals (schema.at("/properties/_int/type").asText(), "integer");
        assertTrue(isPropertyRequired(schema, "_int"));

        assertEquals (schema.at("/properties/_booleanObject/type").asText(), "boolean");
        assertPropertyRequired(schema, "_booleanObject", false); // Should allow null by default

        assertEquals (schema.at("/properties/_booleanPrimitive/type").asText(), "boolean");
        assertPropertyRequired(schema, "_booleanPrimitive", true); // Must be required since it must have true or false - not null

        assertEquals (schema.at("/properties/_booleanObjectWithNotNull/type").asText(), "boolean");
        assertPropertyRequired(schema, "_booleanObjectWithNotNull", true);

        assertEquals (schema.at("/properties/_doubleObject/type").asText(), "number");
        assertPropertyRequired(schema, "_doubleObject", false); // Should allow null by default

        assertEquals (schema.at("/properties/_doublePrimitive/type").asText(), "number");
        assertPropertyRequired(schema, "_doublePrimitive", true); // Must be required since it must have a value - not null

        assertEquals (schema.at("/properties/myEnum/type").asText(), "string");
        assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")), 
                Stream.of(MyEnum.values()).map(Enum::name).collect(Collectors.toList()));
        assertEquals (schema.at("/properties/myEnum/JsonSchemaInjectOnEnum").asText(), "true");
    }

    @Test void nullables() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.manyPrimitivesNulls);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.manyPrimitivesNulls.getClass(), jsonNode);

        assertNullableType(schema, "/properties/_string", "string");
        assertNullableType(schema, "/properties/_integer", "integer");
        assertNullableType(schema, "/properties/_booleanObject", "boolean");
        assertNullableType(schema, "/properties/_doubleObject", "number");

        // We're actually going to test this elsewhere, because if we set this to null here it'll break the "generateAndValidateSchema"
        // test. What's fun is that the type system will allow you to set the value as null, but the schema won't (because there's a @NotNull annotation on it).
        assertEquals (schema.at("/properties/_booleanObjectWithNotNull/type").asText(), "boolean");
        assertPropertyRequired(schema, "_booleanObjectWithNotNull", true);

        assertEquals (schema.at("/properties/_int/type").asText(), "integer");
        assertPropertyRequired(schema, "_int", true);

        assertEquals (schema.at("/properties/_booleanPrimitive/type").asText(), "boolean");
        assertPropertyRequired(schema, "_booleanPrimitive", true);

        assertEquals (schema.at("/properties/_doublePrimitive/type").asText(), "number");
        assertPropertyRequired(schema, "_doublePrimitive", true);

        assertNullableType(schema, "/properties/myEnum", "string");
        assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/oneOf/1/enum")), 
                Stream.of(MyEnum.values()).map(Enum::name).collect(Collectors.toList()));
    }

    @Test void optional() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingOptionalJava);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingOptionalJava.getClass(), jsonNode);

        assertEquals (schema.at("/properties/_string/type").asText(), "string");
        assertPropertyRequired(schema, "_string", false); // Should allow null by default

        assertEquals (schema.at("/properties/_integer/type").asText(), "integer");
        assertPropertyRequired(schema, "_integer", false); // Should allow null by default

        JsonNode child1 = getNodeViaRefs(schema, schema.at("/properties/child1"), "Child1");

        assertJsonSubTypesInfo(child1, "type", "child1");
        assertEquals (child1.at("/properties/parentString/type").asText(), "string");
        assertEquals (child1.at("/properties/child1String/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String2/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");

        assertEquals (schema.at("/properties/optionalList/type").asText(), "array");
        assertEquals (schema.at("/properties/optionalList/items/$ref").asText(), "#/definitions/ClassNotExtendingAnything");
    }

    @Test void nullableOptional() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoUsingOptionalJava);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoUsingOptionalJava.getClass(), jsonNode);

        assertNullableType(schema, "/properties/_string", "string");
        assertNullableType(schema, "/properties/_integer", "integer");

        JsonNode child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1");

        assertJsonSubTypesInfo(child1, "type", "child1");
        assertNullableType(child1, "/properties/parentString", "string");
        assertNullableType(child1, "/properties/child1String", "string");
        assertNullableType(child1, "/properties/_child1String2", "string");
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");

        assertNullableType(schema, "/properties/optionalList", "array");
        assertEquals (schema.at("/properties/optionalList/oneOf/1/items/$ref").asText(), "#/definitions/ClassNotExtendingAnything");
    }

    @Test void customSerializerNotOverridingJsonSerializer_acceptJsonFormatVisitor() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithCustomSerializer);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithCustomSerializer.getClass(), jsonNode);
        assertEquals (toList(schema.fieldNames()), Collections.unmodifiableList( Arrays.asList("$schema", "title") )); // Empty schema due to custom serializer
    }

    @Test void objectWithPropertyUsingCustomSerializerNotOverridingJsonSerializer_acceptJsonFormatVisitor() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer.getClass(), jsonNode);
        assertEquals (schema.at("/properties/s/type").asText(), "string");
        assertEquals (toList(schema.at("/properties/child").fieldNames()), Collections.unmodifiableList(new ArrayList<>()));
    }

    void pojoWithArrays_impl(Object pojo, Class<?> clazz, JsonSchemaGenerator g, boolean html5Checks) {
        JsonNode jsonNode = assertToFromJson(g, pojo);
        JsonNode schema = generateAndValidateSchema(g, clazz, jsonNode);

        assertEquals (schema.at("/properties/intArray1/type").asText(), "array");
        assertEquals (schema.at("/properties/intArray1/items/type").asText(), "integer");

        assertEquals (schema.at("/properties/stringArray/type").asText(), "array");
        assertEquals (schema.at("/properties/stringArray/items/type").asText(), "string");

        assertEquals (schema.at("/properties/stringList/type").asText(), "array");
        assertEquals (schema.at("/properties/stringList/items/type").asText(), "string");
        assertTrue (schema.at("/properties/stringList/minItems").asInt() == 1);
        assertTrue (schema.at("/properties/stringList/maxItems").asInt() == 10);

        assertEquals (schema.at("/properties/polymorphismList/type").asText(), "array");
        assertChild1(schema, "/properties/polymorphismList/items/oneOf", html5Checks);
        assertChild2(schema, "/properties/polymorphismList/items/oneOf", html5Checks);

        assertEquals (schema.at("/properties/polymorphismArray/type").asText(), "array");
        assertChild1(schema, "/properties/polymorphismArray/items/oneOf", html5Checks);
        assertChild2(schema, "/properties/polymorphismArray/items/oneOf", html5Checks);

        assertEquals (schema.at("/properties/listOfListOfStrings/type").asText(), "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/items/type").asText(), "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/items/items/type").asText(), "string");

        assertEquals (schema.at("/properties/setOfUniqueValues/type").asText(), "array");
        assertEquals (schema.at("/properties/setOfUniqueValues/items/type").asText(), "string");

        if (html5Checks) {
            assertEquals (schema.at("/properties/setOfUniqueValues/uniqueItems").asText(), "true");
            assertEquals (schema.at("/properties/setOfUniqueValues/format").asText(), "checkbox");
        }
    }

    @Test void pojoWithArrays() {
        pojoWithArrays_impl(testData.pojoWithArrays, testData.pojoWithArrays.getClass(), jsonSchemaGenerator, false);
        pojoWithArrays_impl(testData.pojoWithArrays, testData.pojoWithArrays.getClass(), jsonSchemaGeneratorHTML5, true);
    }

    @Test void pojoWithArraysNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoWithArraysNullable);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoWithArraysNullable.getClass(), jsonNode);

        assertNullableType(schema, "/properties/intArray1", "array");
        assertEquals (schema.at("/properties/intArray1/oneOf/1/items/type").asText(), "integer");

        assertNullableType(schema, "/properties/stringArray", "array");
        assertEquals (schema.at("/properties/stringArray/oneOf/1/items/type").asText(), "string");

        assertNullableType(schema, "/properties/stringList", "array");
        assertEquals (schema.at("/properties/stringList/oneOf/1/items/type").asText(), "string");

        assertNullableType(schema, "/properties/polymorphismList", "array");
        assertNullableChild1(schema, "/properties/polymorphismList/oneOf/1/items/oneOf");
        assertNullableChild2(schema, "/properties/polymorphismList/oneOf/1/items/oneOf");

        assertNullableType(schema, "/properties/polymorphismArray", "array");
        assertNullableChild1(schema, "/properties/polymorphismArray/oneOf/1/items/oneOf");
        assertNullableChild2(schema, "/properties/polymorphismArray/oneOf/1/items/oneOf");

        assertNullableType(schema, "/properties/listOfListOfStrings", "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/oneOf/1/items/type").asText(), "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/oneOf/1/items/items/type").asText(), "string");
    }

    @Test void recursivePojo() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.recursivePojo);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.recursivePojo.getClass(), jsonNode);

        assertEquals (schema.at("/properties/myText/type").asText(), "string");

        assertEquals (schema.at("/properties/children/type").asText(), "array");
        ObjectNode defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/items"), "RecursivePojo");

        assertEquals (defViaRef.at("/properties/myText/type").asText(), "string");
        assertEquals (defViaRef.at("/properties/children/type").asText(), "array");
        ObjectNode defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/items"), "RecursivePojo");

        assertEquals (defViaRef, defViaRef2);
    }

    @Test void recursivePojoNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.recursivePojo);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.recursivePojo.getClass(), jsonNode);

        assertNullableType(schema, "/properties/myText", "string");

        assertNullableType(schema, "/properties/children", "array");
        ObjectNode defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/oneOf/1/items"), "RecursivePojo");

        assertNullableType(defViaRef, "/properties/myText", "string");
        assertNullableType(defViaRef, "/properties/children", "array");
        ObjectNode defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/oneOf/1/items"), "RecursivePojo");

        assertEquals (defViaRef, defViaRef2);
    }

    @Test void pojoUsingMaps() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingMaps);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingMaps.getClass(), jsonNode);

        assertEquals (schema.at("/properties/string2Integer/type").asText(), "object");
        assertEquals (schema.at("/properties/string2Integer/additionalProperties/type").asText(), "integer");

        assertEquals (schema.at("/properties/string2String/type").asText(), "object");
        assertEquals (schema.at("/properties/string2String/additionalProperties/type").asText(), "string");

        assertEquals (schema.at("/properties/string2PojoUsingJsonTypeInfo/type").asText(), "object");
        assertEquals (schema.at("/properties/string2PojoUsingJsonTypeInfo/additionalProperties/oneOf/0/$ref").asText(), "#/definitions/Child1");
        assertEquals (schema.at("/properties/string2PojoUsingJsonTypeInfo/additionalProperties/oneOf/1/$ref").asText(), "#/definitions/Child2");
    }

    @Test void pojoUsingMapsNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoUsingMaps);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoUsingMaps.getClass(), jsonNode);

        assertNullableType(schema, "/properties/string2Integer", "object");
        assertEquals (schema.at("/properties/string2Integer/oneOf/1/additionalProperties/type").asText(), "integer");

        assertNullableType(schema, "/properties/string2String", "object");
        assertEquals (schema.at("/properties/string2String/oneOf/1/additionalProperties/type").asText(), "string");

        assertNullableType(schema, "/properties/string2PojoUsingJsonTypeInfo", "object");
        assertEquals (schema.at("/properties/string2PojoUsingJsonTypeInfo/oneOf/1/additionalProperties/oneOf/0/$ref").asText(), "#/definitions/Child1");
        assertEquals (schema.at("/properties/string2PojoUsingJsonTypeInfo/oneOf/1/additionalProperties/oneOf/1/$ref").asText(), "#/definitions/Child2");
    }

    @Test void pojoUsingCustomAnnotations() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingFormat);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingFormat.getClass(), jsonNode);
        JsonNode schemaHTML5Date = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingFormat.getClass(), jsonNode);
        JsonNode schemaHTML5DateNullable = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingFormat.getClass(), jsonNode);

        assertEquals (schema.at("/format").asText(), "grid");
        assertEquals (schema.at("/description").asText(), "This is our pojo");
        assertEquals (schema.at("/title").asText(), "Pojo using format");


        assertEquals (schema.at("/properties/emailValue/type").asText(), "string");
        assertEquals (schema.at("/properties/emailValue/format").asText(), "email");
        assertEquals (schema.at("/properties/emailValue/description").asText(), "This is our email value");
        assertEquals (schema.at("/properties/emailValue/title").asText(), "Email value");

        assertEquals (schema.at("/properties/choice/type").asText(), "boolean");
        assertEquals (schema.at("/properties/choice/format").asText(), "checkbox");

        assertEquals (schema.at("/properties/dateTime/type").asText(), "string");
        assertEquals (schema.at("/properties/dateTime/format").asText(), "date-time");
        assertEquals (schema.at("/properties/dateTime/description").asText(), "This is description from @JsonPropertyDescription");
        assertEquals (schemaHTML5Date.at("/properties/dateTime/format").asText(), "datetime");
        assertEquals (schemaHTML5DateNullable.at("/properties/dateTime/oneOf/1/format").asText(), "datetime");


        assertEquals (schema.at("/properties/dateTimeWithAnnotation/type").asText(), "string");
        assertEquals (schema.at("/properties/dateTimeWithAnnotation/format").asText(), "text");

        // Make sure autoGenerated title is correct;
        assertEquals (schemaHTML5Date.at("/properties/dateTimeWithAnnotation/title").asText(), "Date Time With Annotation");
    }

    @Test void usingJavaType() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingFormat);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, objectMapper.constructType(testData.pojoUsingFormat.getClass()), jsonNode);

        assertEquals (schema.at("/format").asText(), "grid");
        assertEquals (schema.at("/description").asText(), "This is our pojo");
        assertEquals (schema.at("/title").asText(), "Pojo using format");


        assertEquals (schema.at("/properties/emailValue/type").asText(), "string");
        assertEquals (schema.at("/properties/emailValue/format").asText(), "email");
        assertEquals (schema.at("/properties/emailValue/description").asText(), "This is our email value");
        assertEquals (schema.at("/properties/emailValue/title").asText(), "Email value");

        assertEquals (schema.at("/properties/choice/type").asText(), "boolean");
        assertEquals (schema.at("/properties/choice/format").asText(), "checkbox");

        assertEquals (schema.at("/properties/dateTime/type").asText(), "string");
        assertEquals (schema.at("/properties/dateTime/format").asText(), "date-time");
        assertEquals (schema.at("/properties/dateTime/description").asText(), "This is description from @JsonPropertyDescription");


        assertEquals (schema.at("/properties/dateTimeWithAnnotation/type").asText(), "string");
        assertEquals (schema.at("/properties/dateTimeWithAnnotation/format").asText(), "text");
    }
    
    @Test void usingJavaTypeWithJsonTypeName() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator g = new JsonSchemaGenerator(objectMapper, config);

        BoringContainer instance = new BoringContainer();
        instance.child1 = new PojoUsingJsonTypeName();
        instance.child1.stringWithDefault = "test";
        JsonNode jsonNode = assertToFromJson(g, instance);
        assertToFromJson(g, instance, BoringContainer.class);

        JsonNode schema = generateAndValidateSchema(g, BoringContainer.class, jsonNode);

        assertEquals (schema.at("/definitions/OtherTypeName/type").asText(), "object");
    }

    @Test void javaOptionalJsonEditor() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava.getClass(), jsonNode);

        assertNullableType(schema, "/properties/_string", "string");
        assertEquals (schema.at("/properties/_string/title").asText(), "_string");

        assertNullableType(schema, "/properties/_integer", "integer");
        assertEquals (schema.at("/properties/_integer/title").asText(), "_integer");

        assertEquals (schema.at("/properties/child1/oneOf/0/type").asText(), "null");
        assertEquals (schema.at("/properties/child1/oneOf/0/title").asText(), "Not included");
        JsonNode child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1");
        assertEquals (schema.at("/properties/child1/title").asText(), "Child 1");

        assertJsonSubTypesInfo(child1, "type", "child1", true);
        assertEquals (child1.at("/properties/parentString/type").asText(), "string");
        assertEquals (child1.at("/properties/child1String/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String2/type").asText(), "string");
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");

        assertNullableType(schema, "/properties/optionalList", "array");
        assertEquals (schema.at("/properties/optionalList/oneOf/1/items/$ref").asText(), "#/definitions/ClassNotExtendingAnything");
        assertEquals (schema.at("/properties/optionalList/title").asText(), "Optional List");
    }

    @Test void javaOptionalJsonEditorNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingOptionalJava);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingOptionalJava.getClass(), jsonNode);

        assertNullableType(schema, "/properties/_string", "string");
        assertNullableType(schema, "/properties/_integer", "integer");

        assertEquals (schema.at("/properties/child1/oneOf/0/type").asText(), "null");
        assertEquals (schema.at("/properties/child1/oneOf/0/title").asText(), "Not included");
        JsonNode child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1");

        assertJsonSubTypesInfo(child1, "type", "child1", true);
        assertNullableType(child1, "/properties/parentString", "string");
        assertNullableType(child1, "/properties/child1String", "string");
        assertNullableType(child1, "/properties/_child1String2", "string");

        // This is required as we have a @JsonProperty marking it as so.;
        assertEquals (child1.at("/properties/_child1String3/type").asText(), "string");
        assertPropertyRequired(child1, "_child1String3", true);

        assertNullableType(schema, "/properties/optionalList", "array");
        assertEquals (schema.at("/properties/optionalList/oneOf/1/items/$ref").asText(), "#/definitions/ClassNotExtendingAnything");
        assertEquals (schema.at("/properties/optionalList/title").asText(), "Optional List");
    }

    @Test void propertyOrdering() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass(), jsonNode);

        assertTrue (schema.at("/properties/someString/propertyOrder").isMissingNode());
    }

    @Test void propertyOrderingNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything.getClass(), jsonNode);

        assertTrue (schema.at("/properties/someString/propertyOrder").isMissingNode());
    }

    @Test void propertyOrderingJsonEditor() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.classNotExtendingAnything.getClass(), jsonNode);

        assertEquals (schema.at("/properties/someString/propertyOrder").asInt(), 1);
        assertEquals (schema.at("/properties/myEnum/propertyOrder").asInt(), 2);
    }

    @Test void propertyOrderingJsonEditorNullable() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.classNotExtendingAnything.getClass(), jsonNode);

        assertEquals (schema.at("/properties/someString/propertyOrder").asInt(), 1);
        assertEquals (schema.at("/properties/myEnum/propertyOrder").asInt(), 2);
    }

    @Test void dates() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.manyDates);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.manyDates.getClass(), jsonNode);

        assertEquals (schema.at("/properties/javaLocalDateTime/format").asText(), "datetime-local");
        assertEquals (schema.at("/properties/javaOffsetDateTime/format").asText(), "datetime");
        assertEquals (schema.at("/properties/javaLocalDate/format").asText(), "date");
        assertEquals (schema.at("/properties/jodaLocalDate/format").asText(), "date");
    }

    @Test void defaultAndExamples() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.defaultAndExamples);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.defaultAndExamples.getClass(), jsonNode);

        assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/emailValue/examples")), Collections.unmodifiableList( Arrays.asList("user@example.com") ));
        assertEquals (schema.at("/properties/fontSize/default").asText(), "12");
        assertEquals (getArrayNodeAsListOfStrings(schema.at("/properties/fontSize/examples")), Collections.unmodifiableList( Arrays.asList("10", "14", "18") ));

        assertEquals (schema.at("/properties/defaultStringViaJsonValue/default").asText(), "ds");
        assertEquals (schema.at("/properties/defaultIntViaJsonValue/default").asText(), "1");
        assertEquals (schema.at("/properties/defaultBoolViaJsonValue/default").asText(), "true");
    }
    
    @Test void validation1() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.classUsingValidation);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.classUsingValidation.getClass(), jsonNode);

        verifyStringProperty(schema, "stringUsingNotNull", 1, null, null, true);
        verifyStringProperty(schema, "stringUsingNotBlank", 1, null, "^.*\\S+.*$", true);
        verifyStringProperty(schema, "stringUsingNotBlankAndNotNull", 1, null, "^.*\\S+.*$", true);
        verifyStringProperty(schema, "stringUsingNotEmpty", 1, null, null, true);
        verifyStringProperty(schema, "stringUsingSize", 1, 20, null, false);
        verifyStringProperty(schema, "stringUsingSizeOnlyMin", 1, null, null, false);
        verifyStringProperty(schema, "stringUsingSizeOnlyMax", null, 30, null, false);
        verifyStringProperty(schema, "stringUsingPattern", null, null, "_stringUsingPatternA|_stringUsingPatternB", false);
        verifyStringProperty(schema, "stringUsingPatternList", null, null, "^(?=^_stringUsing.*)(?=.*PatternList$).*$", false);

        verifyNumericProperty(schema, "intMin", 1, null, true);
        verifyNumericProperty(schema, "intMax", null, 10, true);
        verifyNumericProperty(schema, "doubleMin", 1, null, true);
        verifyNumericProperty(schema, "doubleMax", null, 10, true);
        verifyNumericDoubleProperty(schema, "decimalMin", 1.5, null, true);
        verifyNumericDoubleProperty(schema, "decimalMax", null, 2.5, true);
        assertEquals (schema.at("/properties/email/format").asText(), "email");

        verifyArrayProperty(schema, "notEmptyStringArray", 1, null, true);

        verifyObjectProperty(schema, "notEmptyMap", "string", 1, null, true);
    }

    @Test void validation2() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoUsingValidation);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingValidation.getClass(), jsonNode);

        verifyStringProperty(schema, "stringUsingNotNull", 1, null, null, true);
        verifyStringProperty(schema, "stringUsingNotBlank", 1, null, "^.*\\S+.*$", true);
        verifyStringProperty(schema, "stringUsingNotBlankAndNotNull", 1, null, "^.*\\S+.*$", true);
        verifyStringProperty(schema, "stringUsingNotEmpty", 1, null, null, true);
        verifyStringProperty(schema, "stringUsingSize", 1, 20, null, false);
        verifyStringProperty(schema, "stringUsingSizeOnlyMin", 1, null, null, false);
        verifyStringProperty(schema, "stringUsingSizeOnlyMax", null, 30, null, false);
        verifyStringProperty(schema, "stringUsingPattern", null, null, "_stringUsingPatternA|_stringUsingPatternB", false);
        verifyStringProperty(schema, "stringUsingPatternList", null, null, "^(?=^_stringUsing.*)(?=.*PatternList$).*$", false);

        verifyNumericProperty(schema, "intMin", 1, null, true);
        verifyNumericProperty(schema, "intMax", null, 10, true);
        verifyNumericProperty(schema, "doubleMin", 1, null, true);
        verifyNumericProperty(schema, "doubleMax", null, 10, true);
        verifyNumericDoubleProperty(schema, "decimalMin", 1.5, null, true);
        verifyNumericDoubleProperty(schema, "decimalMax", null, 2.5, true);

        verifyArrayProperty(schema, "notEmptyStringArray", 1, null, true);
        verifyArrayProperty(schema, "notEmptyStringList", 1, null, true);

        verifyObjectProperty(schema, "notEmptyStringMap", "string", 1, null, true);
    }

    void verifyStringProperty(JsonNode schema, String propertyName, Integer minLength, Integer maxLength, 
            String pattern, boolean required) {
        assertNumericPropertyValidation(schema, propertyName, "minLength", minLength);
        assertNumericPropertyValidation(schema, propertyName, "maxLength", maxLength);

        JsonNode matchNode = schema.at("/properties/"+propertyName+"/pattern");
        if (pattern != null)
            assertEquals (matchNode.asText(), pattern);
        else
            assertTrue (matchNode.isMissingNode());

        assertPropertyRequired(schema, propertyName, required);
    }

    void verifyNumericProperty(JsonNode schema, String propertyName, Integer minimum, Integer maximum, boolean required) {
        assertNumericPropertyValidation(schema, propertyName, "minimum", minimum);
        assertNumericPropertyValidation(schema, propertyName, "maximum", maximum);
        assertPropertyRequired(schema, propertyName, required);
    }

    void verifyNumericDoubleProperty(JsonNode schema, String propertyName, Double minimum, Double maximum, boolean required) {
        assertNumericDoublePropertyValidation(schema, propertyName, "minimum", minimum);
        assertNumericDoublePropertyValidation(schema, propertyName, "maximum", maximum);
        assertPropertyRequired(schema, propertyName, required);
    }

    void verifyArrayProperty(JsonNode schema, String propertyName, Integer minItems, Integer maxItems, boolean required) {
        assertNumericPropertyValidation(schema, propertyName, "minItems", minItems);
        assertNumericPropertyValidation(schema, propertyName, "maxItems", maxItems);
        assertPropertyRequired(schema, propertyName, required);
    }

    void verifyObjectProperty(JsonNode schema, String propertyName, String additionalPropertiesType, Integer minProperties, Integer maxProperties, boolean required) {
        assertEquals (schema.at("/properties/"+propertyName+"/additionalProperties/type").asText(), additionalPropertiesType);
        assertNumericPropertyValidation(schema, propertyName, "minProperties", minProperties);
        assertNumericPropertyValidation(schema, propertyName, "maxProperties", maxProperties);
        assertPropertyRequired(schema, propertyName, required);
    }

    void assertNumericPropertyValidation(JsonNode schema, String propertyName, String validationName, Integer value) {
        JsonNode jsonNode = schema.at("/properties/"+propertyName+"/"+validationName+"");
        if (value != null)
            assertEquals (jsonNode.asInt(), value);
        else
            assertTrue (jsonNode.isMissingNode());
    }

    void assertNumericDoublePropertyValidation(JsonNode schema, String propertyName, String validationName, Double value) {
        JsonNode jsonNode = schema.at("/properties/"+propertyName+"/"+validationName+"");
        if (value != null)
            assertEquals (jsonNode.asDouble(), value);
        else
            assertTrue (jsonNode.isMissingNode());
    }
    
    ClassUsingValidationWithGroups objectUsingGroups = testData.classUsingValidationWithGroups;

    @Test void validationUsingNoGroups() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(new ArrayList<Class<?>>()).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", true);
        checkInjected(schema, "defaultGroup", true);
        checkInjected(schema, "group1", false);
        checkInjected(schema, "group2", false);
        checkInjected(schema, "group12", false);

        // Make sure inject on class-level is not included
        assertTrue (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingDefaultGroup() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(Default.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", true);
        checkInjected(schema, "defaultGroup", true);
        checkInjected(schema, "group1", false);
        checkInjected(schema, "group2", false);
        checkInjected(schema, "group12", false);

        // Make sure inject on class-level is not included
        assertTrue (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingGroup1() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(ValidationGroup1.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", false);
        checkInjected(schema, "defaultGroup", false);
        checkInjected(schema, "group1", true);
        checkInjected(schema, "group2", false);
        checkInjected(schema, "group12", true);

        // Make sure inject on class-level is included
        assertFalse (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingGroup1AndDefault() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(ValidationGroup1.class, Default.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", true);
        checkInjected(schema, "defaultGroup", true);
        checkInjected(schema, "group1", true);
        checkInjected(schema, "group2", false);
        checkInjected(schema, "group12", true);

        // Make sure inject on class-level is included
        assertFalse (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingGroup2() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(ValidationGroup2.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", false);
        checkInjected(schema, "defaultGroup", false);
        checkInjected(schema, "group1", false);
        checkInjected(schema, "group2", true);
        checkInjected(schema, "group12", true);

        // Make sure inject on class-level is not included;
        assertTrue (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingGroup1and2() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(ValidationGroup1.class, ValidationGroup2.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", false);
        checkInjected(schema, "defaultGroup", false);
        checkInjected(schema, "group1", true);
        checkInjected(schema, "group2", true);
        checkInjected(schema, "group12", true);

        // Make sure inject on class-level is included
        assertFalse (schema.at("/injected").isMissingNode());
    }

    @Test void validationUsingGroup3() {
        JsonSchemaGenerator jsonSchemaGenerator_Group = new JsonSchemaGenerator(objectMapper,
          JsonSchemaConfig.DEFAULT.toBuilder().javaxValidationGroups(Collections.unmodifiableList( Arrays.asList(ValidationGroup3_notInUse.class) )).build());

        JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator_Group, objectUsingGroups);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator_Group, objectUsingGroups.getClass(), jsonNode);

        checkInjected(schema, "noGroup", false);
        checkInjected(schema, "defaultGroup", false);
        checkInjected(schema, "group1", false);
        checkInjected(schema, "group2", false);
        checkInjected(schema, "group12", false);

        // Make sure inject on class-level is not included
        assertTrue (schema.at("/injected").isMissingNode());
    }
    
    void checkInjected(JsonNode schema, String propertyName, boolean included) {
        assertPropertyRequired(schema, propertyName, included);
        assertNotEquals (schema.at("/properties/"+propertyName+"/injected").isMissingNode(), included);
    }

    @Test void polymorphismUsingMixin() {
      JsonNode jsonNode = assertToFromJson(jsonSchemaGenerator, testData.mixinChild1);
      assertToFromJson(jsonSchemaGenerator, testData.mixinChild1, MixinParent.class);

      JsonNode schema = generateAndValidateSchema(jsonSchemaGenerator, MixinParent.class, jsonNode);

      assertChild1(schema, "/oneOf", "MixinChild1");
      assertChild2(schema, "/oneOf", "MixinChild2");
    }

    @Test void polymorphismUsingMixinNullable() {
      JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.mixinChild1);
      assertToFromJson(jsonSchemaGeneratorNullable, testData.mixinChild1, MixinParent.class);

      JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, MixinParent.class, jsonNode);

      assertNullableChild1(schema, "/oneOf", "MixinChild1");
      assertNullableChild2(schema, "/oneOf", "MixinChild2");
    }

    @Test void issue24() throws JsonMappingException {
        jsonSchemaGenerator.generateJsonSchema(EntityWrapper.class);
        jsonSchemaGeneratorNullable.generateJsonSchema(EntityWrapper.class);
    }

    @Test void polymorphismOneOfOrdering() {
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, PolymorphismOrdering.class, null);
        List<String> oneOfList = toList(schema.at("/oneOf").iterator()).stream().map(e -> e.at("/$ref").asText()).collect(Collectors.toList());
        assertEquals (Collections.unmodifiableList( Arrays.asList("#/definitions/PolymorphismOrderingChild3", "#/definitions/PolymorphismOrderingChild1", "#/definitions/PolymorphismOrderingChild4", "#/definitions/PolymorphismOrderingChild2") ), oneOfList);
    }

    @Test void notNullAnnotationsAndNullableTypes() {
        JsonNode jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.notNullableButNullBoolean);
        JsonNode schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.notNullableButNullBoolean.getClass(), null);

        Exception exception = null;
        try {
            useSchema(schema, jsonNode);
        }
        catch (Exception e) {
            exception = e;
        }

        // While our compiler will let us do what we're about to do, the validator should give us a message that looks like this...
        assertTrue (exception.getMessage().contains("json does not validate against schema"));
        assertTrue (exception.getMessage().contains("error: instance type (null) does not match any allowed primitive type (allowed: [\"boolean\"])"));

        assertEquals (schema.at("/properties/notNullBooleanObject/type").asText(), "boolean");
        assertPropertyRequired(schema, "notNullBooleanObject", true);
    }

    @Test void usingSchemaInject() throws JsonMappingException {
        String customUserNameLoaderVariable = "xx";
        CustomUserNamesLoader customUserNamesLoader = new CustomUserNamesLoader(customUserNameLoaderVariable);

        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT.toBuilder().jsonSuppliers(Stream.of(
        		  new AbstractMap.SimpleImmutableEntry<>("myCustomUserNamesLoader", customUserNamesLoader))
        		  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).build();
        JsonSchemaGenerator _jsonSchemaGeneratorScala = new JsonSchemaGenerator(objectMapper, config);
        JsonNode schema = _jsonSchemaGeneratorScala.generateJsonSchema(UsingJsonSchemaInject.class);

        out.println("--------------------------------------------");
        out.println(asPrettyJson(schema, _jsonSchemaGeneratorScala.objectMapper));

        assertEquals (schema.at("/patternProperties/^s[a-zA-Z0-9]+/type").asText(), "string");
        assertEquals (schema.at("/patternProperties/^i[a-zA-Z0-9]+/type").asText(), "integer");
        assertEquals (schema.at("/properties/sa/type").asText(), "string");
        assertEquals (schema.at("/properties/injectedInProperties").asText(), "true");
        assertEquals (schema.at("/properties/sa/options/hidden").asText(), "true");
        assertEquals (schema.at("/properties/saMergeFalse/type").asText(), "integer");
        assertEquals (schema.at("/properties/saMergeFalse/default").asText(), "12");
        assertTrue (schema.at("/properties/saMergeFalse/pattern").isMissingNode());
        assertEquals (schema.at("/properties/ib/type").asText(), "integer");
        assertEquals (schema.at("/properties/ib/multipleOf").asInt(), 7);
        assertTrue (schema.at("/properties/ib/exclusiveMinimum").asBoolean());
        assertEquals (schema.at("/properties/uns/items/enum/0").asText(), "foo");
        assertEquals (schema.at("/properties/uns/items/enum/1").asText(), "bar");
        assertEquals (schema.at("/properties/uns2/items/enum/0").asText(), "foo_" + customUserNameLoaderVariable);
        assertEquals (schema.at("/properties/uns2/items/enum/1").asText(), "bar_" + customUserNameLoaderVariable);
    }

    @Test void usingJsonSchemaInjectWithTopLevelMergeFalse() throws JsonMappingException {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT;
        JsonSchemaGenerator _jsonSchemaGeneratorScala = new JsonSchemaGenerator(objectMapper, config);
        JsonNode schema = _jsonSchemaGeneratorScala.generateJsonSchema(UsingJsonSchemaInjectWithTopLevelMergeFalse.class);

        String schemaJson = asPrettyJson(schema, _jsonSchemaGeneratorScala.objectMapper);
        out.println("--------------------------------------------");
        out.println(schemaJson);

        String fasit = 
            "{\n" +
            "  \"everything\" : \"should be replaced\"\n" +
            "}";
        assertEquals(fasit.replace("\r\n", "\n"), schemaJson.replaceAll("\r\n", "\n"));
    }
    
    @Test void preventingPolymorphismWithClassTypeRemapping_classWithProperty() {
    	JsonSchemaConfig config = JsonSchemaConfig.DEFAULT.toBuilder().classTypeReMapping(Stream.of(
      		  new AbstractMap.SimpleImmutableEntry<>(Parent.class, Child1.class))
      		  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).build();
        JsonSchemaGenerator _jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);

        // PojoWithParent has a property of type Parent (which uses polymorphism).
        // Default rendering schema will make this property oneOf Child1 and Child2.
        // In this test we're preventing this by remapping Parent to Child1.
        // Now, when generating the schema, we should generate it as if the property where of type Child1

        JsonNode jsonNode = assertToFromJson(_jsonSchemaGenerator, testData.pojoWithParent);
        assertToFromJson(_jsonSchemaGenerator, testData.pojoWithParent, PojoWithParent.class);

        JsonNode schema = generateAndValidateSchema(_jsonSchemaGenerator, PojoWithParent.class, jsonNode);

        assertTrue (!schema.at("/additionalProperties").asBoolean());
        assertEquals (schema.at("/properties/pojoValue/type").asText(), "boolean");
        assertDefaultValues(schema);

        assertChild1(schema, "/properties/child");
    }

    @Test void preventingPolymorphismWithClassTypeRemapping_rootClass() {
    	JsonSchemaConfig config = JsonSchemaConfig.DEFAULT.toBuilder().classTypeReMapping(Stream.of(
        		  new AbstractMap.SimpleImmutableEntry<>(Parent.class, Child1.class))
        		  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).build();
        JsonSchemaGenerator _jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);
        
        preventingPolymorphismWithClassTypeRemapping_rootClass_doTest(testData.child1, Parent.class, _jsonSchemaGenerator);
    }
    void preventingPolymorphismWithClassTypeRemapping_rootClass_doTest(Object pojo, Class<?> clazz, JsonSchemaGenerator g) {
      JsonNode jsonNode = assertToFromJson(g, pojo);
      JsonNode schema = generateAndValidateSchema(g, clazz, jsonNode);

      assertTrue (!schema.at("/additionalProperties").asBoolean());
      assertEquals (schema.at("/properties/parentString/type").asText(), "string");
      assertJsonSubTypesInfo(schema, "type", "child1");
    }

    @Test void preventingPolymorphismWithClassTypeRemapping_arrays() {
        JsonSchemaConfig config = JsonSchemaConfig.DEFAULT.toBuilder().classTypeReMapping(Stream.of(
      		  new AbstractMap.SimpleImmutableEntry<>(Parent.class, Child1.class))
      		  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).build();
        JsonSchemaGenerator _jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);
        
        Child1 c = new Child1();
        c.parentString = "pv";
        c.child1String = "cs";
        c.child1String2 = "cs2";
        c.child1String3 = "cs3";

        ClassNotExtendingAnything _classNotExtendingAnything = new ClassNotExtendingAnything();
        _classNotExtendingAnything.someString = "Something";
        _classNotExtendingAnything.myEnum = MyEnum.C;

        PojoWithArrays _pojoWithArrays = new PojoWithArrays(
            new int[] {1,2,3},
            new String[] {"a1","a2","a3"},
            Collections.unmodifiableList( Arrays.asList("l1", "l2", "l3") ),
            Collections.unmodifiableList( Arrays.asList(c, c) ),
            new Parent[] {c, c},
            Collections.unmodifiableList( Arrays.asList(_classNotExtendingAnything, _classNotExtendingAnything) ),
            Arrays.asList(Arrays.asList("1","2"), Arrays.asList("3")),
            Collections.unmodifiableSet( Stream.of(MyEnum.B).collect(Collectors.toSet()) )
        );

        preventingPolymorphismWithClassTypeRemapping_arrays_doTest(_pojoWithArrays, _pojoWithArrays.getClass(), _jsonSchemaGenerator, false);
    }
    
    void preventingPolymorphismWithClassTypeRemapping_arrays_doTest(Object pojo, Class<?> clazz, JsonSchemaGenerator g, boolean html5Checks) {
    	JsonSchemaConfig config = JsonSchemaConfig.DEFAULT.toBuilder().classTypeReMapping(Stream.of(
      		  new AbstractMap.SimpleImmutableEntry<>(Parent.class, Child1.class))
      		  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).build();
        JsonSchemaGenerator _jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);
        
        JsonNode jsonNode = assertToFromJson(g, pojo);
        JsonNode schema = generateAndValidateSchema(g, clazz, jsonNode);

        assertEquals (schema.at("/properties/intArray1/type").asText(), "array");
        assertEquals (schema.at("/properties/intArray1/items/type").asText(), "integer");

        assertEquals (schema.at("/properties/stringArray/type").asText(), "array");
        assertEquals (schema.at("/properties/stringArray/items/type").asText(), "string");

        assertEquals (schema.at("/properties/stringList/type").asText(), "array");
        assertEquals (schema.at("/properties/stringList/items/type").asText(), "string");
        assertEquals (schema.at("/properties/stringList/minItems").asInt(), 1);
        assertEquals (schema.at("/properties/stringList/maxItems").asInt(), 10);

        assertEquals (schema.at("/properties/polymorphismList/type").asText(), "array");
        assertChild1(schema, "/properties/polymorphismList/items", html5Checks);


        assertEquals (schema.at("/properties/polymorphismArray/type").asText(), "array");
        assertChild1(schema, "/properties/polymorphismArray/items", html5Checks);

        assertEquals (schema.at("/properties/listOfListOfStrings/type").asText(), "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/items/type").asText(), "array");
        assertEquals (schema.at("/properties/listOfListOfStrings/items/items/type").asText(), "string");

        assertEquals (schema.at("/properties/setOfUniqueValues/type").asText(), "array");
        assertEquals (schema.at("/properties/setOfUniqueValues/items/type").asText(), "string");

        if (html5Checks) {
            assertEquals (schema.at("/properties/setOfUniqueValues/uniqueItems").asText(), "true");
            assertEquals (schema.at("/properties/setOfUniqueValues/format").asText(), "checkbox");
        }
    }

    @Test void draft06() {
    	JsonSchemaGenerator jsg = jsonSchemaGenerator_draft_06;
        JsonNode jsonNode = assertToFromJson(jsg, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsg, testData.classNotExtendingAnything.getClass(), jsonNode, JsonSchemaDraft.DRAFT_06);

        // Currently there are no differences in the generated jsonSchema other than the $schema-url
      }

    @Test void draft07() {
    	JsonSchemaGenerator jsg = jsonSchemaGenerator_draft_07;
        JsonNode jsonNode = assertToFromJson(jsg, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsg, testData.classNotExtendingAnything.getClass(), jsonNode, JsonSchemaDraft.DRAFT_07);

        // Currently there are no differences in the generated jsonSchema other than the $schema-url
    }

    @Test void draft201909() {
    	JsonSchemaGenerator jsg = jsonSchemaGenerator_draft_2019_09;
        JsonNode jsonNode = assertToFromJson(jsg, testData.classNotExtendingAnything);
        JsonNode schema = generateAndValidateSchema(jsg, testData.classNotExtendingAnything.getClass(), jsonNode, JsonSchemaDraft.DRAFT_2019_09);

        // Currently there are no differences in the generated jsonSchema other than the $schema-url
    }
}
