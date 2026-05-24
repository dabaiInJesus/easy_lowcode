package com.dabai.easy_lowcode.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptProcessorImpl 单元测试
 */
class PromptProcessorImplTest {

    private PromptProcessorImpl promptProcessor;

    @BeforeEach
    void setUp() {
        promptProcessor = new PromptProcessorImpl(null);
    }

    @Test
    void testSubstituteVariables_singleVariable() {
        String template = "Hello {{name}}, welcome!";
        String variablesConfig = "{\"name\": \"张三\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello 张三, welcome!", result);
    }

    @Test
    void testSubstituteVariables_multipleVariables() {
        String template = "我叫 {{name}}，今年 {{age}} 岁";
        String variablesConfig = "{\"name\": \"李四\", \"age\": \"25\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("我叫 李四，今年 25 岁", result);
    }

    @Test
    void testSubstituteVariables_missingVariable_keepsPlaceholder() {
        String template = "Hello {{name}}, your email is {{email}}";
        String variablesConfig = "{\"name\": \"张三\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello 张三, your email is {{email}}", result);
    }

    @Test
    void testSubstituteVariables_noVariablesInTemplate() {
        String template = "Hello world";
        String variablesConfig = "{\"name\": \"张三\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello world", result);
    }

    @Test
    void testSubstituteVariables_emptyVariablesConfig() {
        String template = "Hello {{name}}";
        String variablesConfig = "{}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello {{name}}", result);
    }

    @Test
    void testSubstituteVariables_nullVariablesConfig() {
        String template = "Hello {{name}}";

        String result = promptProcessor.substituteVariables(template, null);

        assertEquals("Hello {{name}}", result);
    }

    @Test
    void testSubstituteVariables_invalidJsonVariablesConfig() {
        String template = "Hello {{name}}";
        String variablesConfig = "not json";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello {{name}}", result);
    }

    @Test
    void testSubstituteVariables_singleQuotes() {
        String template = "Hello {{name}}";
        String variablesConfig = "{'name': '王五'}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Hello 王五", result);
    }

    @Test
    void testSubstituteVariables_unquotedValues() {
        String template = "Count: {{count}}";
        String variablesConfig = "{\"count\": 100}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Count: 100", result);
    }

    @Test
    void testSubstituteVariables_specialCharacters() {
        String template = "Message: {{msg}}";
        String variablesConfig = "{\"msg\": \"Hello & welcome!\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Message: Hello & welcome!", result);
    }

    @Test
    void testSubstituteVariables_repeatedVariable() {
        String template = "{{name}} says hello to {{name}}";
        String variablesConfig = "{\"name\": \"张三\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("张三 says hello to 张三", result);
    }

    @Test
    void testSubstituteVariables_emptyStringVariable() {
        String template = "Name: [{{name}}]";
        String variablesConfig = "{\"name\": \"\"}";

        String result = promptProcessor.substituteVariables(template, variablesConfig);

        assertEquals("Name: []", result);
    }
}
