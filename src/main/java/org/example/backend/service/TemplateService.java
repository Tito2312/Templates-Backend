package org.example.backend.service;

import org.example.backend.dto.TemplateRequestDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Template;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public TemplateService(TemplateRepository templateRepository,
                           UserRepository userRepository,
                           BrandRepository brandRepository,
                           CategoryRepository categoryRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponseDto> searchTemplates(Long userId, String query, Long brandId, Long categoryId) {
        List<Template> templates = templateRepository.searchTemplates(userId, query, brandId, categoryId);
        return templates.stream().map(this::mapToResponseDto).toList();
    }

    @Transactional
    public TemplateResponseDto createTemplate(Long userId, TemplateRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Brand brand = dto.getBrandId() != null ?
                brandRepository.findById(dto.getBrandId()).orElse(null) : null;

        Category category = dto.getCategoryId() != null ?
                categoryRepository.findById(dto.getCategoryId()).orElse(null) : null;

        Template template = Template.builder()
                .user(user)
                .brand(brand)
                .category(category)
                .title(dto.getTitle())
                .content(dto.getContent())
                .imageUrl(dto.getImageUrl())
                .usageCount(0)
                .build();

        return mapToResponseDto(templateRepository.save(template));
    }

    @Transactional
    public void incrementUsage(Long templateId, Long userId) {
        templateRepository.incrementUsageCount(templateId, userId);
    }

    @Transactional
    public void deleteTemplate(Long templateId, Long userId) {
        templateRepository.findById(templateId).ifPresent(t -> {
            if (t.getUser().getId().equals(userId)) {
                templateRepository.delete(t);
            }
        });
    }

    private TemplateResponseDto mapToResponseDto(Template t) {
        return TemplateResponseDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .content(t.getContent())
                .imageUrl(t.getImageUrl())
                .usageCount(t.getUsageCount())
                .brandId(t.getBrand() != null ? t.getBrand().getId() : null)
                .brandName(t.getBrand() != null ? t.getBrand().getName() : null)
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
