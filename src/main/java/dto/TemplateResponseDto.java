package dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponseDto {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private Integer usageCount;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
