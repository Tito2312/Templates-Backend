package controller;

import dto.TemplateRequestDto;
import dto.TemplateResponseDto;
import service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    // Búsqueda dinámica con autocomplete y orden por uso
    @GetMapping("/search")
    public ResponseEntity<List<TemplateResponseDto>> searchTemplates(
            @RequestParam Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId) {

        List<TemplateResponseDto> result = templateService.searchTemplates(userId, q, brandId, categoryId);
        return ResponseEntity.ok(result);
    }

    // Crear nueva plantilla
    @PostMapping
    public ResponseEntity<TemplateResponseDto> createTemplate(
            @RequestParam Long userId,
            @RequestBody TemplateRequestDto dto) {

        return ResponseEntity.ok(templateService.createTemplate(userId, dto));
    }

    // Incrementar contador de uso (+1) al copiar al portapapeles
    @PostMapping("/{id}/use")
    public ResponseEntity<Void> incrementUsage(@PathVariable Long id, @RequestParam Long userId) {
        templateService.incrementUsage(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Eliminar plantilla
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id, @RequestParam Long userId) {
        templateService.deleteTemplate(id, userId);
        return ResponseEntity.noContent().build();
    }
}
