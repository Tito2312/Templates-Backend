package org.example.backend.repository;

import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Role;
import org.example.backend.model.Template;
import org.example.backend.model.TemplateUsage;
import org.example.backend.model.TemplateUsageId;
import org.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas del catálogo global contra H2 (modo PostgreSQL), sin depender de Supabase.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sac;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TemplateRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private TemplateRepository templateRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TemplateUsageRepository usageRepository;

    private User admin;
    private User other;
    private Brand bet;
    private Category chat;
    private Template saludo;
    private Template despedida;
    private Template promo;

    @BeforeEach
    void setUp() {
        admin = em.persist(User.builder().username("admin").email("admin@test.com").password("x").role(Role.ROLE_ADMIN).build());
        other = em.persist(User.builder().username("otro").email("otro@test.com").password("x").role(Role.ROLE_USER).build());

        bet = em.persist(Brand.builder().name("BET593").user(admin).build());
        chat = em.persist(Category.builder().name("CHAT").user(admin).build());

        // Plantillas de dos creadores distintos: el catálogo debe mostrarlas todas
        saludo = em.persist(template("Saludo", "Hola cliente", admin, bet, chat));
        despedida = em.persist(template("Despedida", "Gracias por escribir", other, null, chat));
        promo = em.persist(template("Promo", "Bono de bienvenida", other, bet, null));

        // Uso individual: solo el admin ha copiado plantillas
        em.persist(usage(admin, saludo, 2));
        em.persist(usage(admin, despedida, 5));
        em.flush();
    }

    private Template template(String title, String content, User user, Brand brand, Category category) {
        return Template.builder().title(title).content(content).user(user)
                .brand(brand).category(category).usageCount(0).build();
    }

    private TemplateUsage usage(User user, Template template, int count) {
        return TemplateUsage.builder()
                .id(new TemplateUsageId(user.getId(), template.getId()))
                .usageCount(count)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    private Page<Template> search(String query, String brand, String category) {
        return templateRepository.searchTemplates(admin.getId(), query, brand, category, PageRequest.of(0, 10));
    }

    private List<String> titles(Page<Template> page) {
        return page.getContent().stream().map(Template::getTitle).toList();
    }

    @Test
    void searchReturnsTemplatesFromAllUsersOrderedByPersonalUsage() {
        Page<Template> forAdmin = search(null, null, null);

        assertEquals(3, forAdmin.getTotalElements());
        // Admin: primero lo que más usa él; la que nunca usó va al final
        assertEquals(List.of("Despedida", "Saludo", "Promo"), titles(forAdmin));

        // Otro usuario sin usos: el uso del admin no le afecta, ordena por más reciente
        Page<Template> forOther = templateRepository.searchTemplates(other.getId(), null, null, null, PageRequest.of(0, 10));
        assertEquals(List.of("Promo", "Despedida", "Saludo"), titles(forOther));
    }

    @Test
    void usageIsIncrementedPerUser() {
        LocalDateTime now = LocalDateTime.now();

        assertEquals(1, usageRepository.increment(admin.getId(), saludo.getId(), now));
        assertEquals(0, usageRepository.increment(other.getId(), saludo.getId(), now)); // sin registro aún

        List<TemplateUsage> adminUsage = usageRepository.findByUserAndTemplates(admin.getId(), List.of(saludo.getId(), promo.getId()));
        assertEquals(1, adminUsage.size());
        assertEquals(3, adminUsage.get(0).getUsageCount());
        assertTrue(usageRepository.findByUserAndTemplates(other.getId(), List.of(saludo.getId())).isEmpty());

        assertEquals(1, usageRepository.deleteByTemplateId(saludo.getId()));
        assertTrue(usageRepository.findByUserAndTemplates(admin.getId(), List.of(saludo.getId())).isEmpty());
    }

    @Test
    void searchPaginatesWithTotalPages() {
        for (int i = 0; i < 10; i++) {
            em.persist(template("Extra " + i, "x", admin, null, null));
        }
        em.flush();

        Page<Template> first = templateRepository.searchTemplates(admin.getId(), null, null, null, PageRequest.of(0, 12));
        Page<Template> second = templateRepository.searchTemplates(admin.getId(), null, null, null, PageRequest.of(1, 12));

        assertEquals(13, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(12, first.getContent().size());
        assertEquals(1, second.getContent().size());
    }

    @Test
    void searchFiltersByTextAndByBrandOrCategoryIgnoringCase() {
        assertEquals(1, search("bienvenida", null, null).getTotalElements());
        assertEquals(2, search(null, "bet593", null).getTotalElements());
        assertEquals(2, search(null, String.valueOf(bet.getId()), null).getTotalElements());
        assertEquals(2, search(null, null, "Chat").getTotalElements());
        assertEquals(1, search(null, "BET593", "chat").getTotalElements());
    }

    @Test
    void unlinkBrandKeepsTemplatesWithoutBrand() {
        int updated = templateRepository.unlinkBrand(bet.getId());
        brandRepository.deleteById(bet.getId());
        em.flush();

        assertEquals(2, updated);
        assertEquals(3, templateRepository.count());
        assertTrue(templateRepository.findAll().stream().allMatch(t -> t.getBrand() == null));
    }

    @Test
    void unlinkCategoryKeepsTemplatesWithoutCategory() {
        int updated = templateRepository.unlinkCategory(chat.getId());
        categoryRepository.deleteById(chat.getId());
        em.flush();

        assertEquals(2, updated);
        assertEquals(3, templateRepository.count());
        assertTrue(templateRepository.findAll().stream().allMatch(t -> t.getCategory() == null));
    }

    @Test
    void nameUniquenessChecksIgnoreCase() {
        assertTrue(brandRepository.existsByNameIgnoreCase("bet593"));
        assertFalse(brandRepository.existsByNameIgnoreCaseAndIdNot("bet593", bet.getId()));
        assertTrue(categoryRepository.existsByNameIgnoreCase("chat"));
        assertFalse(categoryRepository.existsByNameIgnoreCaseAndIdNot("Chat", chat.getId()));
    }
}
