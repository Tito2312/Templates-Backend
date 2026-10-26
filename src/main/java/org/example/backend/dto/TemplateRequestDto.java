package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequestDto {
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar 150 caracteres")
    private String title;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;
    private String imageUrl;
    private Long brandId;
    private Long categoryId;
}
