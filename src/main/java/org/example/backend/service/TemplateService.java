package org.example.backend.service;

import org.example.backend.dto.PageResponseDto;
import org.example.backend.dto.TemplateRequestDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Template;
import org.example.backend.model.TemplateUsage;
import org.example.backend.model.TemplateUsageId;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.TemplateUsageRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Plantillas del catálogo global: todos los usuarios autenticados las consultan y copian;
 * crear, editar y eliminar está restringido a ROLE_ADMIN en SecurityConfig.
 * user_id solo registra quién creó la plantilla.
 * El uso (veces copiada) es individual: cada usuario ve y ordena por su propio contador.
 */
@Service
public class TemplateService {

    private static final int MAX_PAGE_SIZE = 50;

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateUsageRepository templateUsageRepository;

    public TemplateService(
            TemplateRepository templateRepository,
            UserRepository userRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            TemplateUsageRepository templateUsageRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.templateUsageRepository = templateUsageRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
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

        Long userId = getAuthenticatedUser().getId();

        // Paginación acotada para evitar consultas enormes.
        // Sin Sort: el orden (uso del usuario y luego fecha de actualización) va en la consulta.
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Template> templatePage = templateRepository.searchTemplates(
                userId, query, brandName, categoryName, PageRequest.of(safePage, safeSize)
        );

        // Uso personal de las plantillas de esta página
        Map<Long, Integer> usageByTemplate = templatePage.getContent().isEmpty()
                ? Map.of()
                : templateUsageRepository.findByUserAndTemplates(
                                userId,
                                templatePage.getContent().stream().map(Template::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(u -> u.getId().getTemplateId(), TemplateUsage::getUsageCount));

        List<TemplateResponseDto> content = templatePage.getContent().stream()
                .map(t -> mapToResponseDto(t, usageByTemplate.getOrDefault(t.getId(), 0)))
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
        return mapToResponseDto(saved, 0);
    }

    @Transactional
    public TemplateResponseDto updateTemplate(Long id, TemplateRequestDto requestDto) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con ID: " + id));

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
        return mapToResponseDto(updated, getUsageOfCurrentUser(updated.getId()));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con ID: " + id));

        // Los contadores de uso de la plantilla se van con ella
        templateUsageRepository.deleteByTemplateId(template.getId());
        templateRepository.delete(template);
    }

    /**
     * Registra que el usuario actual copió la plantilla (contador individual).
     * No modifica la plantilla, así su fecha de actualización no cambia al copiarla.
     */
    @Transactional
    public void incrementUsage(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plantilla no encontrada");
        }
        Long userId = getAuthenticatedUser().getId();
        LocalDateTime now = LocalDateTime.now();

        if (templateUsageRepository.increment(userId, id, now) == 0) {
            // Primera vez que este usuario copia esta plantilla
            templateUsageRepository.save(TemplateUsage.builder()
                    .id(new TemplateUsageId(userId, id))
                    .usageCount(1)
                    .lastUsedAt(now)
                    .build());
        }
    }

    private int getUsageOfCurrentUser(Long templateId) {
        Long userId = getAuthenticatedUser().getId();
        return templateUsageRepository.findById(new TemplateUsageId(userId, templateId))
                .map(TemplateUsage::getUsageCount)
                .orElse(0);
    }

    private TemplateResponseDto mapToResponseDto(Template template, int usageCount) {
        return TemplateResponseDto.builder()
                .id(template.getId())
                .title(template.getTitle())
                .content(template.getContent())
                .imageUrl(template.getImageUrl())
                .usageCount(usageCount)
                .brandId(template.getBrand() != null ? template.getBrand().getId() : null)
                .brandName(template.getBrand() != null ? template.getBrand().getName() : null)
                .categoryId(template.getCategory() != null ? template.getCategory().getId() : null)
                .categoryName(template.getCategory() != null ? template.getCategory().getName() : null)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
