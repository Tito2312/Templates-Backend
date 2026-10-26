package org.example.backend.service;

import org.example.backend.dto.BrandDto;
import org.example.backend.exception.ConflictException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Brand;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Marcas del catálogo global. La autorización (solo ROLE_ADMIN escribe)
 * se aplica en SecurityConfig; user_id solo registra quién la creó.
 */
@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository, TemplateRepository templateRepository) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<BrandDto> getBrands() {
        return brandRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BrandDto createBrand(BrandDto brandDto) {
        String name = brandDto.getName().trim();
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una marca con el nombre \"" + name + "\"");
        }

        Brand brand = new Brand();
        brand.setName(name);
        brand.setColorHex(brandDto.getColorHex());
        brand.setUser(getAuthenticatedUser());

        Brand saved = brandRepository.save(brand);
        return mapToDto(saved);
    }

    @Transactional
    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con ID: " + id));

        String name = brandDto.getName().trim();
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una marca con el nombre \"" + name + "\"");
        }

        brand.setName(name);
        if (brandDto.getColorHex() != null) {
            brand.setColorHex(brandDto.getColorHex());
        }

        Brand updated = brandRepository.save(brand);
        return mapToDto(updated);
    }

    /**
     * Elimina la marca conservando sus plantillas: quedan sin marca
     * y siguen visibles en "Todas las marcas".
     */
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con ID: " + id));

        templateRepository.unlinkBrand(brand.getId());
        brandRepository.delete(brand);
    }

    private BrandDto mapToDto(Brand brand) {
        return BrandDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .colorHex(brand.getColorHex())
                .build();
    }
}
