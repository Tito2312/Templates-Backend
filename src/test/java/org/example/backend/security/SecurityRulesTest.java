package org.example.backend.security;

import org.example.backend.config.JwtAuthenticationFilter;
import org.example.backend.config.JwtTokenProvider;
import org.example.backend.config.SecurityConfig;
import org.example.backend.config.SecurityErrorHandler;
import org.example.backend.dto.AuthResponseDto;
import org.example.backend.dto.BrandDto;
import org.example.backend.dto.CategoryDto;
import org.example.backend.dto.PageResponseDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.dto.UserDto;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Role;
import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.BrandService;
import org.example.backend.service.CategoryService;
import org.example.backend.service.CloudinaryService;
import org.example.backend.service.TemplateService;
import org.example.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorización de la 1.0.4 (anónimo / ROLE_USER / ROLE_ADMIN) sin base de datos:
 * tokens JWT reales firmados por {@link JwtTokenProvider} y repositorios/servicios simulados.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, SecurityErrorHandler.class})
class SecurityRulesTest {

    private static final String USER_EMAIL = "user@test.com";
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String TEMPLATE_JSON = "{\"title\":\"t\",\"content\":\"c\"}";
    private static final String NAME_JSON = "{\"name\":\"n\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;

    @MockBean private UserRepository userRepository;
    @MockBean private UserService userService;
    @MockBean private TemplateService templateService;
    @MockBean private BrandService brandService;
    @MockBean private CategoryService categoryService;
    @MockBean private CloudinaryService cloudinaryService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        User user = User.builder().id(1L).username("user").email(USER_EMAIL).password("x").role(Role.ROLE_USER).build();
        User admin = User.builder().id(2L).username("admin").email(ADMIN_EMAIL).password("x").role(Role.ROLE_ADMIN).build();
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        userToken = tokenProvider.generateToken(1L, USER_EMAIL);
        adminToken = tokenProvider.generateToken(2L, ADMIN_EMAIL);

        PageResponseDto<TemplateResponseDto> emptyPage =
                PageResponseDto.<TemplateResponseDto>builder().content(List.of()).build();
        when(templateService.searchTemplates(any(), any(), any(), anyInt(), anyInt())).thenReturn(emptyPage);
        when(templateService.createTemplate(any())).thenReturn(new TemplateResponseDto());
        when(templateService.updateTemplate(anyLong(), any())).thenReturn(new TemplateResponseDto());
        when(brandService.getBrands()).thenReturn(List.of());
        when(brandService.createBrand(any())).thenReturn(new BrandDto());
        when(brandService.updateBrand(anyLong(), any())).thenReturn(new BrandDto());
        when(categoryService.getCategories()).thenReturn(List.of());
        when(categoryService.createCategory(any())).thenReturn(new CategoryDto());
        when(categoryService.updateCategory(anyLong(), any())).thenReturn(new CategoryDto());
        when(cloudinaryService.uploadImage(any())).thenReturn("https://img");
        when(userService.getUserByEmail(USER_EMAIL)).thenReturn(new UserDto(1L, "user", USER_EMAIL, "ROLE_USER"));
        when(userService.getUserById(anyLong())).thenReturn(new UserDto(1L, "user", USER_EMAIL, "ROLE_USER"));
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ---------- Anónimo ----------

    @Test
    void anonymousGetsJson401OnProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/templates"));
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/templates/1/increment-usage")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenOrUnknownUserGets401() throws Exception {
        mockMvc.perform(as("token-invalido", get("/api/templates"))).andExpect(status().isUnauthorized());

        String orphanToken = tokenProvider.generateToken(99L, "borrado@test.com");
        mockMvc.perform(as(orphanToken, get("/api/templates"))).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndRegisterArePublic() throws Exception {
        when(userService.login(any(), any())).thenReturn(new AuthResponseDto("t", new UserDto()));
        when(userService.register(any(), any(), any())).thenReturn(new AuthResponseDto("t", new UserDto()));

        mockMvc.perform(json(post("/api/users/login"), "{\"email\":\"a@b.com\",\"password\":\"p\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(json(post("/api/users/register"), "{\"username\":\"u\",\"email\":\"a@b.com\",\"password\":\"p\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(json(post("/api/users/register"), "{\"username\":\"\",\"email\":\"no-es-email\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- ROLE_USER ----------

    @Test
    void userCanReadCatalogAndCopyTemplates() throws Exception {
        mockMvc.perform(as(userToken, get("/api/templates"))).andExpect(status().isOk());
        mockMvc.perform(as(userToken, get("/api/brands"))).andExpect(status().isOk());
        mockMvc.perform(as(userToken, get("/api/categories"))).andExpect(status().isOk());
        mockMvc.perform(as(userToken, post("/api/templates/5/increment-usage"))).andExpect(status().isOk());
    }

    @Test
    void userCannotWriteCatalog() throws Exception {
        mockMvc.perform(as(userToken, json(post("/api/templates"), TEMPLATE_JSON)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(as(userToken, json(put("/api/templates/1"), TEMPLATE_JSON))).andExpect(status().isForbidden());
        mockMvc.perform(as(userToken, delete("/api/templates/1"))).andExpect(status().isForbidden());

        mockMvc.perform(as(userToken, json(post("/api/brands"), NAME_JSON))).andExpect(status().isForbidden());
        mockMvc.perform(as(userToken, json(put("/api/brands/1"), NAME_JSON))).andExpect(status().isForbidden());
        mockMvc.perform(as(userToken, delete("/api/brands/1"))).andExpect(status().isForbidden());

        mockMvc.perform(as(userToken, json(post("/api/categories"), NAME_JSON))).andExpect(status().isForbidden());
        mockMvc.perform(as(userToken, json(put("/api/categories/1"), NAME_JSON))).andExpect(status().isForbidden());
        mockMvc.perform(as(userToken, delete("/api/categories/1"))).andExpect(status().isForbidden());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        mockMvc.perform(as(userToken, multipart("/api/images/upload").file(file))).andExpect(status().isForbidden());
    }

    @Test
    void userCanReadOwnProfileButNotOthers() throws Exception {
        mockMvc.perform(as(userToken, get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
        mockMvc.perform(as(userToken, get("/api/users/2"))).andExpect(status().isForbidden());
    }

    // ---------- ROLE_ADMIN ----------

    @Test
    void adminCanWriteCatalog() throws Exception {
        mockMvc.perform(as(adminToken, json(post("/api/templates"), TEMPLATE_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, json(put("/api/templates/1"), TEMPLATE_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, delete("/api/templates/1"))).andExpect(status().isNoContent());

        mockMvc.perform(as(adminToken, json(post("/api/brands"), NAME_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, json(put("/api/brands/1"), NAME_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, delete("/api/brands/1"))).andExpect(status().isNoContent());

        mockMvc.perform(as(adminToken, json(post("/api/categories"), NAME_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, json(put("/api/categories/1"), NAME_JSON))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, delete("/api/categories/1"))).andExpect(status().isNoContent());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        mockMvc.perform(as(adminToken, multipart("/api/images/upload").file(file))).andExpect(status().isOk());

        mockMvc.perform(as(adminToken, get("/api/users/1"))).andExpect(status().isOk());
    }

    @Test
    void catalogPayloadsAreValidated() throws Exception {
        mockMvc.perform(as(adminToken, json(post("/api/brands"), "{\"name\":\"  \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de la marca es obligatorio"));
        mockMvc.perform(as(adminToken, json(post("/api/brands"), "{\"name\":\"X\",\"colorHex\":\"rojo\"}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, json(put("/api/categories/1"), "{\"name\":\"\"}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, json(post("/api/templates"), "{\"title\":\"\",\"content\":\"c\"}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, json(put("/api/templates/1"), "{\"title\":\"t\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notFoundIsMappedTo404() throws Exception {
        when(brandService.updateBrand(eq(404L), any())).thenThrow(new ResourceNotFoundException("Marca no encontrada con ID: 404"));

        mockMvc.perform(as(adminToken, json(put("/api/brands/404"), NAME_JSON)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Marca no encontrada con ID: 404"));
    }
}
