package service;

import dto.BrandDto;
import model.Brand;
import model.User;
import repository.BrandRepository;
import repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandDto> getBrandsByUser(Long userId) {
        return brandRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(b -> new BrandDto(b.getId(), b.getName()))
                .toList();
    }

    @Transactional
    public BrandDto createBrand(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Brand brand = Brand.builder()
                .user(user)
                .name(name)
                .build();

        Brand saved = brandRepository.save(brand);
        return new BrandDto(saved.getId(), saved.getName());
    }
}
