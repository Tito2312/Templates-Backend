package org.example.backend.controller;

import org.example.backend.dto.PageResponseDto;
import org.example.backend.dto.TemplateRequestDto;
import org.example.backend.dto.TemplateResponseDto;
import org.example.backend.service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<TemplateResponseDto>> getTemplates(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "") String brand,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {

        PageResponseDto<TemplateResponseDto> response = templateService.searchTemplates(query, brand, category, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TemplateResponseDto> createTemplate(@RequestBody TemplateRequestDto requestDto) {
        return ResponseEntity.ok(templateService.createTemplate(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponseDto> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateRequestDto requestDto) {
        return ResponseEntity.ok(templateService.updateTemplate(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/increment-usage")
    public ResponseEntity<Void> incrementUsage(@PathVariable Long id) {
        templateService.incrementUsage(id);
        return ResponseEntity.ok().build();
    }
}