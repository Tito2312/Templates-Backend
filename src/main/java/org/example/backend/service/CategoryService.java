package org.example.backend.service;

import org.example.backend.dto.CategoryDto;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.ForbiddenOperationException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Category;
import org.example.backend.model.User;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        User currentUser = getAuthenticatedUser();
        return categoryRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        User currentUser = getAuthenticatedUser();

        Category category = new Category();
        category.setName(categoryDto.getName().trim());
        category.setUser(currentUser);

        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        // Antes el controlador devolvía 400 ante cualquier error; se mantiene para nombre vacío
        if (categoryDto.getName() == null || categoryDto.getName().isBlank()) {
            throw new BadRequestException("El nombre es obligatorio");
        }

        category.setName(categoryDto.getName());
        Category updated = categoryRepository.save(category);

        return new CategoryDto(updated.getId(), updated.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        User currentUser = getAuthenticatedUser();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (!category.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("No tienes permisos para eliminar esta categoría");
        }

        categoryRepository.delete(category);
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}