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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public TemplateService(
            TemplateRepository templateRepository,
            UserRepository userRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponseDto> searchTemplates(String query, Long brandId, Long categoryId) {
        User currentUser = getAuthenticatedUser();
        List<Template> templates = templateRepository.findByUserId(currentUser.getId());

        return templates.stream()
                .filter(t -> (query == null || query.isBlank()
                        || t.getTitle().toLowerCase().contains(query.toLowerCase())
                        || t.getContent().toLowerCase().contains(query.toLowerCase())))
                .filter(t -> (brandId == null || (t.getBrand() != null && t.getBrand().getId().equals(brandId))))
                .filter(t -> (categoryId == null || (t.getCategory() != null && t.getCategory().getId().equals(categoryId))))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TemplateResponseDto createTemplate(TemplateRequestDto requestDto) {
        User currentUser = getAuthenticatedUser();

        Template template = new Template();
        template.setTitle(requestDto.getTitle().trim());
        template.setContent(requestDto.getContent());
        template.setImageUrl(requestDto.getImageUrl());
        template.setUsageCount(0);
        template.setUser(currentUser);

        if (requestDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(requestDto.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
            template.setBrand(brand);
        }

        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            template.setCategory(category);
        }

        Template saved = templateRepository.save(template);
        return mapToResponseDto(saved);
    }

    @Transactional
    public TemplateResponseDto updateTemplate(Long id, TemplateRequestDto requestDto) {
        User currentUser = getAuthenticatedUser();
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con ID: " + id));

        if (!template.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("No tienes permisos para modificar esta plantilla");
        }

        template.setTitle(requestDto.getTitle().trim());
        template.setContent(requestDto.getContent());
        template.setImageUrl(requestDto.getImageUrl());

        if (requestDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(requestDto.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
            template.setBrand(brand);
        } else {
            template.setBrand(null);
        }

        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            template.setCategory(category);
        } else {
            template.setCategory(null);
        }

        Template updated = templateRepository.save(template);
        return mapToResponseDto(updated);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        User currentUser = getAuthenticatedUser();
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con ID: " + id));

        if (!template.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("No tienes permisos para eliminar esta plantilla");
        }

        templateRepository.delete(template);
    }

    @Transactional
    public void incrementUsage(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);
    }

    private TemplateResponseDto mapToResponseDto(Template template) {
        return TemplateResponseDto.builder()
                .id(template.getId())
                .title(template.getTitle())
                .content(template.getContent())
                .imageUrl(template.getImageUrl())
                .usageCount(template.getUsageCount())
                .brandId(template.getBrand() != null ? template.getBrand().getId() : null)
                .brandName(template.getBrand() != null ? template.getBrand().getName() : null)
                .categoryId(template.getCategory() != null ? template.getCategory().getId() : null)
                .categoryName(template.getCategory() != null ? template.getCategory().getName() : null)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}