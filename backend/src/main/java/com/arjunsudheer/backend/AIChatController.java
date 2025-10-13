package com.arjunsudheer.backend;

import com.microsoft.semantickernel.Kernel;
import com.google.cloud.vertexai.VertexAI;
import com.microsoft.semantickernel.aiservices.google.chatcompletion.GeminiChatCompletion;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;

import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

                        KernelPlugin toDoPlugin = KernelPluginFactory.createFromObject(new ToDoPlugin(), "ToDoPlugin");

                        this.chatKernel = Kernel.builder().withAIService(ChatCompletionService.class, this.chatService)
                                        .withPlugin(toDoPlugin)
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
                        if (chatHistory.getMessages().isEmpty()) {
                                String currentDate = LocalDate.now().toString();
                                String systemPrompt = "You are a helpful AI assistant. The current date is "
                                                + currentDate
                                                + ". Use this date for reference if the user does not give a specific date in the YYYY-MM-DD format."
                                                + "When a user gives a large task, break it down into smaller, actionable steps. Make sure to adjust the to do item name and due date accordingly."
                                                + "For each step, suggest a tool call to perform the action. ";
                                userPrompt = systemPrompt + "\n\n" + userPrompt;
                        }
                        chatHistory.addUserMessage(userPrompt);

                        InvocationContext chatContext = InvocationContext.builder()
                                        .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
                                        .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                                        .build();

                        ChatMessageContent<?> llmResponse = chatService
                                        .getChatMessageContentsAsync(chatHistory, chatKernel, chatContext)
                                        .block()
                                        .get(0);

                        String responseText = llmResponse.getContent();
                        chatHistory.addAssistantMessage(responseText);

                        return ResponseEntity.ok(Map.of("response", responseText, "reload", "true"));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
                }
        }
}
