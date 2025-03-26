package com.hackthennnow.gaied.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailRequest {
    private String emailName;
    private String emailSubject;
    private String emailBody;
    private String attachmentText;
}
