package com.dabai.easy_lowcode.etl.service.impl;

import com.dabai.easy_lowcode.etl.model.TransformRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransformRuleProcessorImpl 单元测试
 */
class TransformRuleProcessorImplTest {

    private TransformRuleProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new TransformRuleProcessorImpl();
    }

    @Test
    void testParseFieldMapping_validJson() {
        String json = "[{\"source\": \"user_name\", \"target\": \"username\"}, {\"source\": \"email\", \"target\": \"user_email\"}]";
        List<String> sourceColumns = Arrays.asList("user_name", "email", "phone");

        Map<String, String> result = processor.parseFieldMapping(json, sourceColumns);

        assertEquals(2, result.size());
        assertEquals("username", result.get("user_name"));
        assertEquals("user_email", result.get("email"));
    }

    @Test
    void testParseFieldMapping_nullJson() {
        List<String> sourceColumns = Arrays.asList("col1", "col2");

        Map<String, String> result = processor.parseFieldMapping(null, sourceColumns);

        assertEquals(2, result.size());
        assertEquals("col1", result.get("col1"));
        assertEquals("col2", result.get("col2"));
    }

    @Test
    void testParseFieldMapping_emptyJson() {
        List<String> sourceColumns = Arrays.asList("col1", "col2");

        Map<String, String> result = processor.parseFieldMapping("", sourceColumns);

        assertEquals(2, result.size());
        assertEquals("col1", result.get("col1"));
    }

    @Test
    void testParseFieldMapping_invalidJson_fallbackToSourceColumns() {
        String json = "invalid json";
        List<String> sourceColumns = Arrays.asList("col1", "col2");

        Map<String, String> result = processor.parseFieldMapping(json, sourceColumns);

        assertEquals(2, result.size());
        assertEquals("col1", result.get("col1"));
        assertEquals("col2", result.get("col2"));
    }

    @Test
    void testParseTransformRules_validJson() {
        String json = "[{\"sourceField\": \"name\", \"targetField\": \"NAME\", \"transformType\": \"UPPER\"}]";

        List<TransformRule> result = processor.parseTransformRules(json);

        assertEquals(1, result.size());
        assertEquals("name", result.get(0).getSourceField());
        assertEquals("UPPER", result.get(0).getTransformType());
    }

    @Test
    void testParseTransformRules_nullJson() {
        List<TransformRule> result = processor.parseTransformRules(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testParseTransformRules_blankJson() {
        List<TransformRule> result = processor.parseTransformRules("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void testParseTransformRules_invalidJson() {
        List<TransformRule> result = processor.parseTransformRules("not json");

        assertTrue(result.isEmpty());
    }

    @Test
    void testApplyTransforms_UPPER() {
        List<TransformRule> rules = createRules("name", "name", "UPPER", null, null);

        Object result = processor.applyTransforms("name", "name", "hello", rules);

        assertEquals("HELLO", result);
    }

    @Test
    void testApplyTransforms_LOWER() {
        List<TransformRule> rules = createRules("name", "name", "LOWER", null, null);

        Object result = processor.applyTransforms("name", "name", "HELLO", rules);

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_TRIM() {
        List<TransformRule> rules = createRules("name", "name", "TRIM", null, null);

        Object result = processor.applyTransforms("name", "name", "  hello  ", rules);

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_DEFAULT_withNullValue() {
        List<TransformRule> rules = createRules("status", "status", "DEFAULT", null, "active");

        Object result = processor.applyTransforms("status", "status", null, rules);

        assertEquals("active", result);
    }

    @Test
    void testApplyTransforms_DEFAULT_withEmptyValue() {
        List<TransformRule> rules = createRules("status", "status", "DEFAULT", null, "active");

        Object result = processor.applyTransforms("status", "status", "", rules);

        assertEquals("active", result);
    }

    @Test
    void testApplyTransforms_DEFAULT_withNonEmptyValue() {
        List<TransformRule> rules = createRules("status", "status", "DEFAULT", null, "active");

        Object result = processor.applyTransforms("status", "status", "inactive", rules);

        assertEquals("inactive", result);
    }

    @Test
    void testApplyTransforms_CONCAT() {
        List<TransformRule> rules = createRules("name", "name", "CONCAT", "PREFIX_${value}_SUFFIX", null);

        Object result = processor.applyTransforms("name", "name", "test", rules);

        assertEquals("PREFIX_test_SUFFIX", result);
    }

    @Test
    void testApplyTransforms_CONCAT_withNullValue() {
        List<TransformRule> rules = createRules("name", "name", "CONCAT", "PREFIX_${value}", null);

        Object result = processor.applyTransforms("name", "name", null, rules);

        assertEquals("PREFIX_", result);
    }

    @Test
    void testApplyTransforms_SUBSTRING() {
        List<TransformRule> rules = createRules("name", "name", "SUBSTRING", "0,5", null);

        Object result = processor.applyTransforms("name", "name", "hello world", rules);

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_DATE_FORMAT() {
        List<TransformRule> rules = createRules("date", "date", "DATE_FORMAT", "yyyy-MM-dd", null);
        Date date = new Date(123, 0, 15);

        Object result = processor.applyTransforms("date", "date", date, rules);

        assertTrue(result instanceof String);
        assertTrue(((String) result).matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void testApplyTransforms_noMatchingRule() {
        List<TransformRule> rules = createRules("other_field", "other_field", "UPPER", null, null);

        Object result = processor.applyTransforms("name", "name", "hello", rules);

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_emptyRules() {
        Object result = processor.applyTransforms("name", "name", "hello", Collections.emptyList());

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_NONE_type() {
        List<TransformRule> rules = createRules("name", "name", "NONE", null, null);

        Object result = processor.applyTransforms("name", "name", "hello", rules);

        assertEquals("hello", result);
    }

    @Test
    void testApplyTransforms_UPPER_nonStringValue() {
        List<TransformRule> rules = createRules("count", "count", "UPPER", null, null);

        Object result = processor.applyTransforms("count", "count", 123, rules);

        assertEquals(123, result);
    }

    @Test
    void testApplyTransforms_matchByTargetField() {
        List<TransformRule> rules = createRules(null, "name", "UPPER", null, null);

        Object result = processor.applyTransforms("other", "name", "hello", rules);

        assertEquals("HELLO", result);
    }

    @Test
    void testApplyTransforms_multipleRules_firstMatchWins() {
        List<TransformRule> rules = new ArrayList<>();
        rules.add(createRule("name", "name", "UPPER", null, null));
        rules.add(createRule("name", "name", "LOWER", null, null));

        Object result = processor.applyTransforms("name", "name", "Hello", rules);

        assertEquals("HELLO", result);
    }

    private List<TransformRule> createRules(String sourceField, String targetField, String transformType, String expression, String defaultValue) {
        List<TransformRule> rules = new ArrayList<>();
        rules.add(createRule(sourceField, targetField, transformType, expression, defaultValue));
        return rules;
    }

    private TransformRule createRule(String sourceField, String targetField, String transformType, String expression, String defaultValue) {
        TransformRule rule = new TransformRule();
        rule.setSourceField(sourceField);
        rule.setTargetField(targetField);
        rule.setTransformType(transformType);
        rule.setExpression(expression);
        rule.setDefaultValue(defaultValue);
        return rule;
    }
}
