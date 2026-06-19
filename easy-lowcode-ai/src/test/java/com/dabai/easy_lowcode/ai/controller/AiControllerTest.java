package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiServiceFactory aiServiceFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private AiService mockAiService;

    @BeforeEach
    void setUp() {
        mockAiService = mock(AiService.class);
        when(mockAiService.getProvider()).thenReturn(AiProvider.OPENAI);
    }

    @Test
    void chat_defaultProvider_success() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setContent("你好！有什么可以帮你的？");
        response.setModel("gpt-3.5-turbo");

        when(aiServiceFactory.getDefaultService()).thenReturn(mockAiService);
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(response);

        ChatRequest request = new ChatRequest();
        request.setMessage("你好");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", is("你好！有什么可以帮你的？")))
                .andExpect(jsonPath("$.data.model", is("gpt-3.5-turbo")));
    }

    @Test
    void chat_noDefaultService_returnsError() throws Exception {
        when(aiServiceFactory.getDefaultService()).thenReturn(null);

        ChatRequest request = new ChatRequest();
        request.setMessage("你好");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("AI 服务未配置，请先在配置文件中启用至少一个 AI 厂商"));
    }

    @Test
    void getProviders_returnsProviderList() throws Exception {
        when(aiServiceFactory.getSupportedProviders()).thenReturn(List.of(AiProvider.OPENAI, AiProvider.DEEPSEEK));
        when(aiServiceFactory.getDefaultService()).thenReturn(mockAiService);

        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.default", is("openai")))
                .andExpect(jsonPath("$.data.supported", hasSize(2)))
                .andExpect(jsonPath("$.data.supported[0].code", is("openai")))
                .andExpect(jsonPath("$.data.supported[1].code", is("deepseek")));
    }

    @Test
    void health_withProviders_returnsUp() throws Exception {
        when(aiServiceFactory.getSupportedProviders()).thenReturn(List.of(AiProvider.OPENAI));
        when(aiServiceFactory.getDefaultService()).thenReturn(mockAiService);

        mockMvc.perform(get("/api/ai/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.providers", hasSize(1)))
                .andExpect(jsonPath("$.data.defaultProvider", is("openai")));
    }

    @Test
    void health_noProviders_returnsDown() throws Exception {
        when(aiServiceFactory.getSupportedProviders()).thenReturn(List.of());
        when(aiServiceFactory.getDefaultService()).thenReturn(null);

        mockMvc.perform(get("/api/ai/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status", is("DOWN")))
                .andExpect(jsonPath("$.data.defaultProvider").doesNotExist());
    }

    @Test
    void simpleChat_success() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setContent("你好！");
        when(aiServiceFactory.getDefaultService()).thenReturn(mockAiService);
        when(mockAiService.chat(any())).thenReturn(response);

        Map<String, String> body = new HashMap<>();
        body.put("message", "你好");

        mockMvc.perform(post("/api/ai/simple-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
