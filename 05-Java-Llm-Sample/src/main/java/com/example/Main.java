package com.example;

import com.example.messages.HumanMessage;
import com.example.util.FileOps;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    static Dotenv dotenv = Dotenv.configure().directory("../").load();
    static ObjectMapper mapper = new ObjectMapper();
    static HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    @FunctionalInterface
    interface LLM {
        Object invoke(List<HumanMessage> messages) throws Exception;
    }

    static LLM getLLM(double temperature) {
        return (messages) -> {
            /**
             * Get OAuth token using client credentials
             */
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
            String accessToken = (String) tokenData.get("access_token");

            /**
             * Build request body from messages
             */
            List<Map<String, Object>> messageList = messages.stream()
                    .map(m -> {
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("role", "user");
                        msg.put("content", m.content);
                        return msg;
                    })
                    .toList();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messageList);
            requestBody.put("temperature", temperature);

            String llmUrl = dotenv.get("AICORE_BASE_URL") + "/inference/deployments/"
                    + dotenv.get("LLM_DEPLOYMENT_ID") + "/chat/completions?api-version=2023-05-15";

            HttpRequest llmRequest = HttpRequest.newBuilder()
                    .uri(URI.create(llmUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("AI-Resource-Group", dotenv.get("AICORE_RESOURCE_GROUP"))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> llmResponse = client.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(llmResponse.body(), Object.class);
        };
    }

    public static void main(String[] args) throws Exception {
        LLM dummyLlm = getLLM(0.2);

        /**
         * Prompt object
         */
        Map<String, String> prompt = new HashMap<>();
        prompt.put("instructions", "capital of america");

        /**
         * Call LLM
         */
        Object response = dummyLlm.invoke(
                List.of(new HumanMessage(mapper.writeValueAsString(prompt)))
        );

        System.out.println("Type of response: " + response.getClass().getName());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

        /**
         * Save metadata to JSON file
         */
        FileOps.saveMetadataToFile(response, "_temp/llm_response.json", mapper);

        /**
         * Convert to string for API payload
         */
        String metadata = mapper.writeValueAsString(response);

        Map<String, String> payload = new HashMap<>();
        payload.put("metadata", metadata);

        /**
         * API URL
         */
        String apiUrl = "https://llm-usage-monitor-egregious-armadillo-bt.cfapps.us10-001.hana.ondemand.com/log-metadata/?app_id=4&call_type=l_invoke";

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
