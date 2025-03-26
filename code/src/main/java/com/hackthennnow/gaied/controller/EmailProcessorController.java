package com.hackthennnow.gaied.controller;

import com.hackthennnow.gaied.entity.EmailRequest;
import com.hackthennnow.gaied.service.EmailClassificationService;
import com.hackthennnow.gaied.service.EmailProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/email")
@Slf4j
public class EmailProcessorController {

    private final EmailClassificationService emailClassificationService;

    public EmailProcessorController(EmailClassificationService emailClassificationService) {
        this.emailClassificationService = emailClassificationService;
    }

    @GetMapping("/process")
    public ResponseEntity<?> processEmail(@RequestBody(required = false) List<EmailRequest> emails) {
        try {
            var result = emailClassificationService.classifyEmails(emails);
            if(result != null && !result.isEmpty())
            {
                result.forEach(emailResponse -> {
                log.info("Email Response: "+emailResponse);
                });
            }
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while processing email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
