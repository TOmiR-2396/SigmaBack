package com.example.gym.controller;

import com.example.gym.dto.TipDTO;
import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import com.example.gym.security.JwtUtil;
import com.example.gym.service.TipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Obtener todos los tips activos
     * GET /api/tips
     */
    @GetMapping
    public ResponseEntity<List<TipDTO>> getActiveTips() {
        log.info("GET /api/tips - Obteniendo tips activos");
        List<TipDTO> tips = tipService.getActiveTips();
        return ResponseEntity.ok(tips);
    }

    /**
     * Obtener todos los tips activos con paginación
     * GET /api/tips/paginated
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<TipDTO>> getActiveTipsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/tips/paginated - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TipDTO> tips = tipService.getActiveTips(pageable);
        return ResponseEntity.ok(tips);
    }

    /**
     * Obtener todos los tips (incluyendo inactivos) - solo OWNER
     * GET /api/tips/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Page<TipDTO>> getAllTips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/tips/all - Obteniendo todos los tips para OWNER");
        Pageable pageable = PageRequest.of(page, size);
        Page<TipDTO> tips = tipService.getAllTips(pageable);
        return ResponseEntity.ok(tips);
    }

    /**
     * Obtener un tip por ID
     * GET /api/tips/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TipDTO> getTipById(@PathVariable Long id) {
        log.info("GET /api/tips/{} - Obteniendo tip por ID", id);
        TipDTO tip = tipService.getTipById(id);
        return ResponseEntity.ok(tip);
    }

    /**
     * Crear un nuevo tip - solo OWNER
     * POST /api/tips
     */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TipDTO> createTip(
            @RequestBody TipDTO tipDTO,
            @RequestHeader("Authorization") String token) {
        log.info("POST /api/tips - Creando nuevo tip: {}", tipDTO.getTitle());
        
        // Extraer email del token
        String tokenWithoutBearer = token.replace("Bearer ", "");
        String email = jwtUtil.getEmailFromToken(tokenWithoutBearer);
        
        // Obtener usuario por email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        TipDTO created = tipService.createTip(tipDTO, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualizar un tip - solo OWNER
     * PUT /api/tips/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TipDTO> updateTip(
            @PathVariable Long id,
            @RequestBody TipDTO tipDTO) {
        log.info("PUT /api/tips/{} - Actualizando tip", id);
        TipDTO updated = tipService.updateTip(id, tipDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cambiar estado activo/inactivo de un tip - solo OWNER
     * PATCH /api/tips/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TipDTO> toggleTipStatus(@PathVariable Long id) {
        log.info("PATCH /api/tips/{}/toggle - Alternando estado del tip", id);
        TipDTO toggled = tipService.toggleTipStatus(id);
        return ResponseEntity.ok(toggled);
    }

    /**
     * Eliminar un tip - solo OWNER
     * DELETE /api/tips/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteTip(@PathVariable Long id) {
        log.info("DELETE /api/tips/{} - Eliminando tip", id);
        tipService.deleteTip(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Buscar tips por título
     * GET /api/tips/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<TipDTO>> searchTips(@RequestParam String title) {
        log.info("GET /api/tips/search?title={} - Buscando tips", title);
        List<TipDTO> tips = tipService.searchByTitle(title);
        return ResponseEntity.ok(tips);
    }
}
