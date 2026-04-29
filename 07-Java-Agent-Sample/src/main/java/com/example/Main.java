package com.example;

import com.example.prompt.AgenticMathFuncPrompt;
import com.example.tool.MathTools;
import com.example.util.FileOps;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.checkpoint.MemorySaver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    static Dotenv dotenv = Dotenv.configure().directory("../").load();
    static ObjectMapper mapper = new ObjectMapper();
    static HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @FunctionalInterface
    interface ReactAgent {
        Map<String, Object> invoke(Map<String, Object> input) throws Exception;
    }

    static String getAccessToken() throws Exception {
        String credentials = dotenv.get("AICORE_CLIENT_ID") + ":" + dotenv.get("AICORE_CLIENT_SECRET");
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(dotenv.get("AICORE_AUTH_URL")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedCredentials)
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> tokenData = mapper.readValue(tokenResponse.body(), Map.class);
        return (String) tokenData.get("access_token");
    }

    static ReactAgent createReactAgent(OpenAiChatModel chatModel, MathTools mathTools) {
        return (input) -> {
            /**
             * Build LangGraph4j AgentExecutor (prebuilt ReAct agent)
             */
            var agent = AgentExecutor.builder()
                    .chatModel(chatModel)
                    .toolsFromObject(mathTools)
                    .build()
                    .compile(CompileConfig.builder()
                            .checkpointSaver(new MemorySaver())
                            .build());

            var config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();

            /**
             * Extract user message content from input
             */
            List<?> messages = (List<?>) input.get("messages");
            Map<?, ?> firstMsg = (Map<?, ?>) messages.get(0);
            String content = (String) firstMsg.get("content");

            /**
             * Invoke agent — streams node outputs, reduce to final state
             */
            var result = agent.stream(
                    Map.of("messages", UserMessage.from(content)),
                    config
            )
            .stream()
            .reduce((a, b) -> b)
            .orElseThrow();

            /**
             * Build response from full message history
             */
            List<Map<String, Object>> messageHistory = new ArrayList<>();
            List<ChatMessage> chatMessages = result.state().messages();
            if (chatMessages != null) {
                for (ChatMessage msg : chatMessages) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", msg.type().name().toLowerCase());

                    String msgContent;
                    if (msg instanceof UserMessage um) {
                        msgContent = um.singleText();
                    } else if (msg instanceof AiMessage am) {
                        msgContent = am.text() != null ? am.text() : "[tool call]";
                    } else if (msg instanceof ToolExecutionResultMessage tm) {
                        msgContent = tm.text();
                    } else {
                        msgContent = msg.toString();
                    }
                    m.put("content", msgContent);
                    messageHistory.add(m);
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("messages", messageHistory);
            return response;
        };
    }

    public static void main(String[] args) throws Exception {
        String token = getAccessToken();

        /**
         * Create LangChain4j OpenAI-compatible model pointing to SAP AI Core
         */
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(dotenv.get("AICORE_BASE_URL") + "/inference/deployments/" + dotenv.get("LLM_DEPLOYMENT_ID") + "/")
                .apiKey(token)
                .customHeaders(Map.of("AI-Resource-Group", dotenv.get("AICORE_RESOURCE_GROUP")))
                .modelName("gpt-4o")
                .temperature(0.0)
                .maxTokens(2000)
                .build();

        /**
         * Create agent
         */
        MathTools mathTools = new MathTools();
        ReactAgent agent = createReactAgent(chatModel, mathTools);

        /**
         * Prompt object
         */
        Map<String, String> prompt = new LinkedHashMap<>();
        prompt.put("instructions", AgenticMathFuncPrompt.format(1, 2));

        /**
         * Build initial user message
         */
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", mapper.writeValueAsString(prompt));

        /**
         * Invoke agent
         */
        Map<String, Object> response = agent.invoke(
                Map.of("messages", List.of(userMessage))
        );

        System.out.println("Type of response: " + response.getClass().getName());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

        /**
         * Save metadata to JSON file
         */
        FileOps.saveMetadataToFile(response, "_temp/agentic_math_func_response.json", mapper);

        /**
         * Convert to string for API payload
         */
        String metadata = mapper.writeValueAsString(response);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("metadata", metadata);

        /**
         * API URL
         */
        String apiUrl = dotenv.get("URL") + "/log-metadata/?app_id=4&call_type=a_invoke";

        /**
         * Send POST request
         */
        HttpRequest apiRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer 59a7d10e140fabe8ee26f96ac5043f19a66e4e30f1895384a6029bdc5347e0dc")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> apiResponse = httpClient.send(apiRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status Code: " + apiResponse.statusCode());
        System.out.println("Response Data: " + apiResponse.body());
    }
}
