package org.example.backend.service;

import org.example.backend.dto.CategoryDto;
import org.example.backend.exception.ConflictException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Category;
import org.example.backend.model.User;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Categorías del catálogo global. La autorización (solo ROLE_ADMIN escribe)
 * se aplica en SecurityConfig; user_id solo registra quién la creó.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository, TemplateRepository templateRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        String name = categoryDto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoría con el nombre \"" + name + "\"");
        }

        Category category = new Category();
        category.setName(name);
        category.setUser(getAuthenticatedUser());

        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        String name = categoryDto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una categoría con el nombre \"" + name + "\"");
        }

        category.setName(name);
        Category updated = categoryRepository.save(category);

        return mapToDto(updated);
    }

    /**
     * Elimina la categoría conservando sus plantillas: quedan sin categoría
     * y siguen visibles en "Todas las categorías".
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        templateRepository.unlinkCategory(category.getId());
        categoryRepository.delete(category);
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
