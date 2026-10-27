package org.example.backend.service;

import org.example.backend.dto.PageResponseDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Role;
import org.example.backend.model.Template;
import org.example.backend.model.TemplateUsage;
import org.example.backend.model.TemplateUsageId;
import org.example.backend.model.User;
import org.example.backend.repository.BrandRepository;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.TemplateRepository;
import org.example.backend.repository.TemplateUsageRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateServiceTest {

    private static final String EMAIL = "user@test.com";

    @Mock private TemplateRepository templateRepository;
    @Mock private UserRepository userRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TemplateUsageRepository usageRepository;

    private TemplateService service;

    @BeforeEach
    void setUp() {
        service = new TemplateService(templateRepository, userRepository, brandRepository, categoryRepository, usageRepository);
        User user = User.builder().id(7L).email(EMAIL).role(Role.ROLE_USER).build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void incrementUsageUpdatesExistingCounterOfCurrentUser() {
        when(templateRepository.existsById(3L)).thenReturn(true);
        when(usageRepository.increment(eq(7L), eq(3L), any())).thenReturn(1);

        service.incrementUsage(3L);

        verify(usageRepository, never()).save(any());
        verify(templateRepository, never()).save(any()); // la plantilla no se toca
    }

    @Test
    void incrementUsageCreatesCounterTheFirstTime() {
        when(templateRepository.existsById(3L)).thenReturn(true);
        when(usageRepository.increment(eq(7L), eq(3L), any())).thenReturn(0);

        service.incrementUsage(3L);

        ArgumentCaptor<TemplateUsage> saved = ArgumentCaptor.forClass(TemplateUsage.class);
        verify(usageRepository).save(saved.capture());
        assertEquals(new TemplateUsageId(7L, 3L), saved.getValue().getId());
        assertEquals(1, saved.getValue().getUsageCount());
    }

    @Test
    void incrementUsageOfMissingTemplateIs404() {
        when(templateRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.incrementUsage(99L));
        verifyNoInteractions(usageRepository);
    }

    @Test
    void searchReturnsPersonalUsageAndClampsPageSize() {
        Template a = Template.builder().id(1L).title("A").content("x").usageCount(40).build();
        Template b = Template.builder().id(2L).title("B").content("y").usageCount(40).build();
        when(templateRepository.searchTemplates(eq(7L), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(a, b), PageRequest.of(0, 50), 2));
        when(usageRepository.findByUserAndTemplates(7L, List.of(1L, 2L))).thenReturn(List.of(
                TemplateUsage.builder().id(new TemplateUsageId(7L, 1L)).usageCount(4).lastUsedAt(LocalDateTime.now()).build()));

        PageResponseDto<TemplateResponseDto> result = service.searchTemplates("", "", "", -3, 500);

        // usageCount es el del usuario (4 y 0), no el contador global antiguo (40)
        assertEquals(List.of(4, 0), result.getContent().stream().map(TemplateResponseDto::getUsageCount).toList());
        verify(templateRepository).searchTemplates(eq(7L), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 50)));
    }

    @Test
    void deleteTemplateRemovesItsUsageCounters() {
        Template t = Template.builder().id(5L).title("T").content("c").build();
        when(templateRepository.findById(5L)).thenReturn(Optional.of(t));

        service.deleteTemplate(5L);

        verify(usageRepository).deleteByTemplateId(5L);
        verify(templateRepository).delete(t);
    }
}
