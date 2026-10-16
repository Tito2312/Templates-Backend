package org.example.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequestDto {
    private String title;
    private String content;
    private String imageUrl;
    private Long brandId;
    private Long categoryId;
}
