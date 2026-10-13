package controller;

import dto.CategoryDto;
import service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(@RequestParam Long userId) {
        return ResponseEntity.ok(categoryService.getCategoriesByUser(userId));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestParam Long userId, @RequestParam String name) {
        return ResponseEntity.ok(categoryService.createCategory(userId, name));
    }
}
