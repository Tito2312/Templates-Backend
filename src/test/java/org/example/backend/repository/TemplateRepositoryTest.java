package org.example.backend.repository;

import org.example.backend.model.Brand;
import org.example.backend.model.Category;
import org.example.backend.model.Role;
import org.example.backend.model.Template;
import org.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    private Brand bet;
    private Category chat;

    @BeforeEach
    void setUp() {
        User admin = em.persist(User.builder().username("admin").email("admin@test.com").password("x").role(Role.ROLE_ADMIN).build());
        User other = em.persist(User.builder().username("otro").email("otro@test.com").password("x").role(Role.ROLE_USER).build());

        bet = em.persist(Brand.builder().name("BET593").user(admin).build());
        chat = em.persist(Category.builder().name("CHAT").user(admin).build());

        // Plantillas de dos creadores distintos: el catálogo debe mostrarlas todas
        em.persist(template("Saludo", "Hola cliente", admin, bet, chat, 5));
        em.persist(template("Despedida", "Gracias por escribir", other, null, chat, 1));
        em.persist(template("Promo", "Bono de bienvenida", other, bet, null, 3));
        em.flush();
    }

    private Template template(String title, String content, User user, Brand brand, Category category, int usage) {
        return Template.builder().title(title).content(content).user(user)
                .brand(brand).category(category).usageCount(usage).build();
    }

    private Page<Template> search(String query, String brand, String category) {
        return templateRepository.searchTemplates(query, brand, category,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "usageCount")));
    }

    @Test
    void searchReturnsTemplatesFromAllUsers() {
        Page<Template> page = search(null, null, null);

        assertEquals(3, page.getTotalElements());
        assertEquals(List.of("Saludo", "Promo", "Despedida"),
                page.getContent().stream().map(Template::getTitle).toList());
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
