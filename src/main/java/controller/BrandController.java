package controller;

import dto.BrandDto;
import service.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ResponseEntity<List<BrandDto>> getBrands(@RequestParam Long userId) {
        return ResponseEntity.ok(brandService.getBrandsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<BrandDto> createBrand(@RequestParam Long userId, @RequestParam String name) {
        return ResponseEntity.ok(brandService.createBrand(userId, name));
    }
}