package org.example.backend.service;

import org.example.backend.dto.BrandDto;
import org.example.backend.dto.CategoryDto;
import org.example.backend.exception.ConflictException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Role;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceTest {

    private static final String ADMIN_EMAIL = "admin@test.com";

    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private UserRepository userRepository;

    private BrandService brandService;
    private CategoryService categoryService;
    private User admin;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository, userRepository, templateRepository);
        categoryService = new CategoryService(categoryRepository, userRepository, templateRepository);

        admin = User.builder().id(1L).email(ADMIN_EMAIL).role(Role.ROLE_ADMIN).build();
        when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Marcas ----------

    @Test
    void getBrandsIsGlobalAndSortedByName() {
        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                Brand.builder().id(3L).name("BET593").build(),
                Brand.builder().id(2L).name("LOTBET").build()));

        List<BrandDto> brands = brandService.getBrands();

        assertEquals(List.of("BET593", "LOTBET"), brands.stream().map(BrandDto::getName).toList());
        verifyNoInteractions(userRepository);
    }

    @Test
    void createBrandTrimsNameAndRecordsCreator() {
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandDto created = brandService.createBrand(new BrandDto(null, "  Nueva  ", "#112233"));

        assertEquals("Nueva", created.getName());
        verify(brandRepository).save(argThat(b -> b.getUser() == admin && "#112233".equals(b.getColorHex())));
    }

    @Test
    void createBrandWithDuplicateNameIs409() {
        when(brandRepository.existsByNameIgnoreCase("bet593")).thenReturn(true);

        assertThrows(ConflictException.class, () -> brandService.createBrand(new BrandDto(null, "bet593", null)));
        verify(brandRepository, never()).save(any());
    }

    @Test
    void updateBrandOfAnotherCreatorIsAllowedButDuplicateIs409() {
        User otro = User.builder().id(2L).build();
        Brand brand = Brand.builder().id(5L).name("Vieja").user(otro).build();
        when(brandRepository.findById(5L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandDto updated = brandService.updateBrand(5L, new BrandDto(null, " Nueva ", null));
        assertEquals("Nueva", updated.getName());

        when(brandRepository.existsByNameIgnoreCaseAndIdNot("LOTBET", 5L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> brandService.updateBrand(5L, new BrandDto(null, "LOTBET", null)));
    }

    @Test
    void updateOrDeleteMissingBrandIs404() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> brandService.updateBrand(99L, new BrandDto(null, "x", null)));
        assertThrows(ResourceNotFoundException.class, () -> brandService.deleteBrand(99L));
        verify(templateRepository, never()).unlinkBrand(any());
    }

    @Test
    void deleteBrandUnlinksTemplatesBeforeDeleting() {
        Brand brand = Brand.builder().id(3L).name("BET593").user(User.builder().id(2L).build()).build();
        when(brandRepository.findById(3L)).thenReturn(Optional.of(brand));

        brandService.deleteBrand(3L);

        InOrder order = inOrder(templateRepository, brandRepository);
        order.verify(templateRepository).unlinkBrand(3L);
        order.verify(brandRepository).delete(brand);
    }

    // ---------- Categorías ----------

    @Test
    void createCategoryWithDuplicateNameIs409() {
        when(categoryRepository.existsByNameIgnoreCase("Chat")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCategory(new CategoryDto(null, " Chat ")));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryWithDuplicateNameIs409() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("Correo").build()));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("CHAT", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.updateCategory(1L, new CategoryDto(null, "CHAT")));
    }

    @Test
    void deleteCategoryUnlinksTemplatesBeforeDeleting() {
        Category category = Category.builder().id(2L).name("CHAT").user(User.builder().id(2L).build()).build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(2L);

        InOrder order = inOrder(templateRepository, categoryRepository);
        order.verify(templateRepository).unlinkCategory(2L);
        order.verify(categoryRepository).delete(category);
    }

    @Test
    void deleteMissingCategoryIs404() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(99L));
        verify(templateRepository, never()).unlinkCategory(any());
    }
}
