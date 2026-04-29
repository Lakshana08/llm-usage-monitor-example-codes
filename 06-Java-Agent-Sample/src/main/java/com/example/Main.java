package com.example;

import com.example.prompt.AgenticMathFuncPrompt;
import com.example.tool.AdditionTool;
import com.example.tool.MultiplyTool;
import com.example.tool.SubtractionTool;
import com.example.util.FileOps;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    static Dotenv dotenv = Dotenv.configure().directory("../").load();
    static ObjectMapper mapper = new ObjectMapper();
    static HttpClient client = HttpClient.newBuilder()
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

        HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> tokenData = mapper.readValue(tokenResponse.body(), Map.class);
        return (String) tokenData.get("access_token");
    }

    static Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    static Map<String, Object> buildToolParameters() {
        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> paramA = new LinkedHashMap<>();
        paramA.put("type", "number");
        paramA.put("description", "First number");
        properties.put("a", paramA);

        Map<String, Object> paramB = new LinkedHashMap<>();
        paramB.put("type", "number");
        paramB.put("description", "Second number");
        properties.put("b", paramB);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("a", "b"));
        return parameters;
    }

    static String executeTool(String name, String argsJson) throws Exception {
        Map<?, ?> args = mapper.readValue(argsJson, Map.class);
        int a = ((Number) args.get("a")).intValue();
        int b = ((Number) args.get("b")).intValue();

        return switch (name) {
            case AdditionTool.NAME    -> mapper.writeValueAsString(AdditionTool.add(a, b));
            case SubtractionTool.NAME -> mapper.writeValueAsString(SubtractionTool.subtract(a, b));
            case MultiplyTool.NAME    -> mapper.writeValueAsString(MultiplyTool.multiply(a, b));
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
    }

    static ReactAgent createReactAgent(String token, List<Map<String, Object>> tools) {
        return (input) -> {
            List<Map<String, Object>> messages = new ArrayList<>((List<Map<String, Object>>) input.get("messages"));

            String llmUrl = dotenv.get("AICORE_BASE_URL") + "/inference/deployments/"
                    + dotenv.get("LLM_DEPLOYMENT_ID") + "/chat/completions?api-version=2023-05-15";

            /**
             * ReAct agent loop
             */
            while (true) {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("messages", messages);
                requestBody.put("tools", tools);
                requestBody.put("temperature", 0.2);

                HttpRequest llmRequest = HttpRequest.newBuilder()
                        .uri(URI.create(llmUrl))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .header("AI-Resource-Group", dotenv.get("AICORE_RESOURCE_GROUP"))
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                        .build();

                HttpResponse<String> llmResponse = client.send(llmRequest, HttpResponse.BodyHandlers.ofString());
                Map<?, ?> llmResult = mapper.readValue(llmResponse.body(), Map.class);

                List<?> choices = (List<?>) llmResult.get("choices");
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                String finishReason = (String) choice.get("finish_reason");
                Map<?, ?> assistantMessage = (Map<?, ?>) choice.get("message");

                messages.add(new LinkedHashMap<>((Map<String, Object>) assistantMessage));

                if ("tool_calls".equals(finishReason)) {
                    List<?> toolCalls = (List<?>) assistantMessage.get("tool_calls");

                    for (Object tc : toolCalls) {
                        Map<?, ?> toolCall = (Map<?, ?>) tc;
                        String toolCallId = (String) toolCall.get("id");
                        Map<?, ?> function = (Map<?, ?>) toolCall.get("function");
                        String toolName = (String) function.get("name");
                        String toolArgs = (String) function.get("arguments");

                        System.out.println("Calling tool: " + toolName + " with args: " + toolArgs);
                        String toolResult = executeTool(toolName, toolArgs);
                        System.out.println("Tool result: " + toolResult);

                        Map<String, Object> toolResultMessage = new LinkedHashMap<>();
                        toolResultMessage.put("role", "tool");
                        toolResultMessage.put("tool_call_id", toolCallId);
                        toolResultMessage.put("content", toolResult);
                        messages.add(toolResultMessage);
                    }
                } else {
                    break;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("messages", messages);
            return result;
        };
    }

    public static void main(String[] args) throws Exception {
        String token = getAccessToken();

        /**
         * Tool definitions
         */
        Map<String, Object> toolParams = buildToolParameters();
        List<Map<String, Object>> tools = List.of(
                buildTool(AdditionTool.NAME,    AdditionTool.DESCRIPTION,    toolParams),
                buildTool(SubtractionTool.NAME, SubtractionTool.DESCRIPTION, toolParams),
                buildTool(MultiplyTool.NAME,    MultiplyTool.DESCRIPTION,    toolParams)
        );

        /**
         * Create agent
         */
        ReactAgent agent = createReactAgent(token, tools);

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

        HttpResponse<String> apiResponse = client.send(apiRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status Code: " + apiResponse.statusCode());
        System.out.println("Response Data: " + apiResponse.body());
    }
}
