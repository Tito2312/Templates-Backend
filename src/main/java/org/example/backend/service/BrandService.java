package org.example.backend.service;

import org.example.backend.dto.BrandDto;
import org.example.backend.model.Brand;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<BrandDto> getBrands() {
        User currentUser = getAuthenticatedUser();
        return brandRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BrandDto createBrand(BrandDto brandDto) {
        User currentUser = getAuthenticatedUser();

        Brand brand = new Brand();
        brand.setName(brandDto.getName().trim());
        brand.setColorHex(brandDto.getColorHex());
        brand.setUser(currentUser);

        Brand saved = brandRepository.save(brand);
        return mapToDto(saved);
    }

    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada con ID: " + id));

        brand.setName(brandDto.getName());
        if (brandDto.getColorHex() != null) {
            brand.setColorHex(brandDto.getColorHex());
        }

        Brand updated = brandRepository.save(brand);
        return new BrandDto(updated.getId(), updated.getName(), updated.getColorHex());
    }

    @Transactional
    public void deleteBrand(Long id) {
        User currentUser = getAuthenticatedUser();
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada con ID: " + id));

        if (!brand.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("No tienes permisos para eliminar esta marca");
        }

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