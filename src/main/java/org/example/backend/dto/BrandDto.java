package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDto {
    private Long id;
    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 50, message = "El nombre de la marca no puede superar 50 caracteres")
    private String name;

    // Opcional; si se envía debe ser un color #RRGGBB
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB")
    private String colorHex;
}
