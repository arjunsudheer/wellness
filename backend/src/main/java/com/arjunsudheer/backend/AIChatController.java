package com.arjunsudheer.backend;

import com.google.cloud.vertexai.VertexAI;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.google.chatcompletion.GeminiChatCompletion;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import com.microsoft.semantickernel.orchestration.InvocationContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class AIChatController {
        private final ChatCompletionService chatService;
        private final Kernel chatKernel;

        private final Map<String, ChatHistory> userChatHistories = new ConcurrentHashMap<>();

        public AIChatController(
                        @Value("${vertexai.project.id}") String projectId,
                        @Value("${vertexai.location}") String location) {

                try {
                        // Create the VertexAI client object
                        VertexAI vertexAIClient = new VertexAI(projectId, location);

                        // Initialize Gemini Chat Service with the VertexAI client
                        this.chatService = GeminiChatCompletion.builder()
                                        .withVertexAIClient(vertexAIClient)
                                        .withModelId("gemini-2.0-flash-lite-001")
                                        .build();
                        this.chatKernel = Kernel.builder().withAIService(ChatCompletionService.class, this.chatService)
                                        .build();
                } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize Vertex AI client", e);
                }
        }

        @PostMapping("/ai-chat")
        public ResponseEntity<?> chat(@RequestBody Map<String, String> payload) {
                String userPrompt = payload.get("prompt");
                String sessionId = payload.getOrDefault("sessionId", "default-session");

                if (userPrompt == null || userPrompt.isEmpty()) {
                        return ResponseEntity.badRequest().body("Prompt is required.");
                }

                ChatHistory chatHistory = userChatHistories.computeIfAbsent(sessionId, k -> new ChatHistory());

                try {
                        // LLM response
                        chatHistory.addUserMessage(userPrompt);

                        InvocationContext chatContext = InvocationContext.builder().build();
                        ChatMessageContent<?> llmResponse = chatService
                                        .getChatMessageContentsAsync(chatHistory, chatKernel, chatContext).block()
                                        .get(0);

                        String responseText = llmResponse.getContent();

                        chatHistory.addAssistantMessage(responseText);

                        return ResponseEntity.ok(Map.of("response", responseText));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
                }
        }
}
