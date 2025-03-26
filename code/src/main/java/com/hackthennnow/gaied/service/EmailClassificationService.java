package com.hackthennnow.gaied.service;

import com.hackthennnow.gaied.entity.EmailRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailClassificationService {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    @Autowired
    EmailProcessorService emailProcessorService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailClassificationService(WebClient.Builder webClientBuilder, @Value("${llama_base_url}") String llmUrl) {
        this.webClient = webClientBuilder.baseUrl(llmUrl).build();
    }

    public String classifyInputFromCustomLlamaModel(EmailRequest emailRequest) throws JsonProcessingException {
        // Create prompt for Mistral
        String prompt = "You are a Loan Department email classifier. Classify the following emails. ###Email Name : ### "+emailRequest.getEmailName()+",### Email Body: ### "+emailRequest.getEmailBody()+" ### Email Subject : ### "+emailRequest.getEmailSubject()+" ### Email Attachment Text : ### "+emailRequest.getAttachmentText();
        prompt = prompt.replace("\r", "").replace("\n", " ");
        log.info(prompt);
        // Call Ollama  API - as of now local host
        String response = webClient.post()
                .uri("/api/generate")
                .header("Content-Type", "application/json")
                .bodyValue("{\"model\":\"Matheswaran/email-classifier\", \"prompt\":\"" + prompt + "\"}")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String formattedJsonResponse = mapToJsonResponse(response);
        log.info("Llama Response --> "+formattedJsonResponse);
        return formattedJsonResponse;
//        return objectMapper.readValue(formattedJsonResponse,
//                objectMapper.getTypeFactory().constructCollectionType(List.class, EmailResponse.class));
    }

    public List<String> classifyEmails(List<EmailRequest> emails) throws Exception {
        if(emails==null || emails.isEmpty())
        {
            emails = fetchEmails();
        }

        List<CompletableFuture<String>> futures = emails.stream().map(emailRequest -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return classifyInputFromCustomLlamaModel(emailRequest);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();
        // Combine all futures
        List<String> results = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // Get results
                        .flatMap(String::lines)
                        .collect(Collectors.toList())) // Convert to List
                .get();
        log.info("Results: " + results);
        return results;
    }

    private List<EmailRequest> fetchEmails(){
        return emailProcessorService.getEmailsFromPath();
    }

    private String mapToJsonResponse(String response) {
        List<String> formattedResponses = new ArrayList<>();
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                JsonNode jsonNode = objectMapper.readTree(line);
                if (jsonNode.has("response")) {
                    formattedResponses.add(jsonNode.get("response").asText());
                }
            }
        } catch (Exception e) {
            return "Error while converting response to json string: " + e.getMessage();
        }
        return String.join("", formattedResponses).trim();
    }
}
