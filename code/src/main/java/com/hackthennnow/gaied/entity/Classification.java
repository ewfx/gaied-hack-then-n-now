package com.hackthennnow.gaied.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Classification {
    private String requestType;
    private List<String> requestSubType;
}
