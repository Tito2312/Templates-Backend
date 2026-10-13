package Services;

import dto.CategoryDto;
import model.Category;
import model.User;
import repository.CategoryRepository;
import repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesByUser(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Category category = Category.builder()
                .user(user)
                .name(name)
                .build();

        Category saved = categoryRepository.save(category);
        return new CategoryDto(saved.getId(), saved.getName());
    }
}
