package org.example.backend.service;

import org.example.backend.dto.PageResponseDto;
import org.example.backend.dto.TemplateRequestDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.exception.ForbiddenOperationException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Template;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public PageResponseDto<TemplateResponseDto> searchTemplates(
            String query,
            String brandName,
            String categoryName,
            int page,
            int size) {

        if (query != null && query.trim().isEmpty()) query = null;
        if (brandName != null && brandName.trim().isEmpty()) brandName = null;
        if (categoryName != null && categoryName.trim().isEmpty()) categoryName = null;

        Long userId = getAuthenticatedUserId();

        // 2. Ordenar por mayor número de usos primero, y luego por fecha de actualización
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "usageCount")
                        .and(Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        // 3. Ejecutar la búsqueda con los parámetros limpios
        Page<Template> templatePage = templateRepository.searchTemplates(
                userId, query, brandName, categoryName, pageable
        );

        List<TemplateResponseDto> content = templatePage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PageResponseDto.<TemplateResponseDto>builder()
                .content(content)
                .pageNumber(templatePage.getNumber())
                .pageSize(templatePage.getSize())
                .totalElements(templatePage.getTotalElements())
                .totalPages(templatePage.getTotalPages())
                .isLast(templatePage.isLast())
                .build();
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
                    .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
            template.setBrand(brand);
        }

        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            template.setCategory(category);
        }

        Template saved = templateRepository.save(template);
        return mapToResponseDto(saved);
    }

    @Transactional
    public TemplateResponseDto updateTemplate(Long id, TemplateRequestDto requestDto) {
        User currentUser = getAuthenticatedUser();
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con ID: " + id));

        if (!template.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("No tienes permisos para modificar esta plantilla");
        }

        template.setTitle(requestDto.getTitle().trim());
        template.setContent(requestDto.getContent());
        template.setImageUrl(requestDto.getImageUrl());

        if (requestDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(requestDto.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
            template.setBrand(brand);
        } else {
            template.setBrand(null);
        }

        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con ID: " + id));

        if (!template.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("No tienes permisos para eliminar esta plantilla");
        }

        templateRepository.delete(template);
    }

    @Transactional
    public void incrementUsage(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
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

    private Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            throw new RuntimeException("No hay un usuario autenticado en la sesión");
        }

        String email = principal.toString();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return user.getId();
    }
}