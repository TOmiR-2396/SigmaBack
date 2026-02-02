package com.example.gym.service;

import com.example.gym.dto.TipDTO;
import com.example.gym.model.Tip;
import com.example.gym.model.User;
import com.example.gym.repository.TipRepository;
import com.example.gym.repository.UserRepository;
import com.example.gym.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private final UserRepository userRepository;

    /**
     * Obtener todos los tips activos
     */
    @Transactional(readOnly = true)
    public List<TipDTO> getActiveTips() {
        log.debug("Obteniendo todos los tips activos para tenant: {}", TenantContext.getCurrentTenant());
        return tipRepository.findAllActive()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener tips activos con paginación
     */
    @Transactional(readOnly = true)
    public Page<TipDTO> getActiveTips(Pageable pageable) {
        log.debug("Obteniendo tips activos con paginación para tenant: {}", TenantContext.getCurrentTenant());
        return tipRepository.findAllActive(pageable)
            .map(this::convertToDTO);
    }

    /**
     * Obtener todos los tips (incluyendo inactivos) - solo para owner
     */
    @Transactional(readOnly = true)
    public Page<TipDTO> getAllTips(Pageable pageable) {
        log.debug("Obteniendo todos los tips (incluyendo inactivos) para tenant: {}", TenantContext.getCurrentTenant());
        return tipRepository.findAllTips(pageable)
            .map(this::convertToDTO);
    }

    /**
     * Obtener un tip por ID
     */
    @Transactional(readOnly = true)
    public TipDTO getTipById(Long id) {
        log.debug("Obteniendo tip con ID: {}", id);
        return tipRepository.findById(id)
            .map(this::convertToDTO)
            .orElseThrow(() -> new RuntimeException("Tip no encontrado con ID: " + id));
    }

    /**
     * Crear un nuevo tip (solo para OWNER)
     */
    @Transactional
    public TipDTO createTip(TipDTO tipDTO, Long createdByUserId) {
        log.info("Creando nuevo tip: {} por usuario: {}", tipDTO.getTitle(), createdByUserId);

        User creator = userRepository.findById(createdByUserId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + createdByUserId));

        // Validaciones
        if (tipDTO.getTitle() == null || tipDTO.getTitle().trim().isEmpty()) {
            throw new RuntimeException("El título no puede estar vacío");
        }
        if (tipDTO.getTitle().length() > 50) {
            throw new RuntimeException("El título no puede exceder 50 caracteres");
        }
        if (tipDTO.getContent() == null || tipDTO.getContent().trim().isEmpty()) {
            throw new RuntimeException("El contenido no puede estar vacío");
        }
        if (tipDTO.getContent().length() > 200) {
            throw new RuntimeException("El contenido no puede exceder 200 caracteres");
        }
        if (tipDTO.getEmoji() == null || tipDTO.getEmoji().trim().isEmpty()) {
            throw new RuntimeException("El emoji no puede estar vacío");
        }

        Tip tip = Tip.builder()
            .title(tipDTO.getTitle().trim())
            .content(tipDTO.getContent().trim())
            .emoji(tipDTO.getEmoji().trim())
            .active(tipDTO.isActive())
            .createdBy(creator)
            .build();

        tip.setTenantId(TenantContext.getCurrentTenant());
        Tip saved = tipRepository.save(tip);
        log.info("Tip creado exitosamente con ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    /**
     * Actualizar un tip existente (solo para OWNER)
     */
    @Transactional
    public TipDTO updateTip(Long id, TipDTO tipDTO) {
        log.info("Actualizando tip con ID: {}", id);

        Tip tip = tipRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tip no encontrado con ID: " + id));

        // Validaciones
        if (tipDTO.getTitle() != null && !tipDTO.getTitle().isEmpty()) {
            if (tipDTO.getTitle().length() > 50) {
                throw new RuntimeException("El título no puede exceder 50 caracteres");
            }
            tip.setTitle(tipDTO.getTitle().trim());
        }

        if (tipDTO.getContent() != null && !tipDTO.getContent().isEmpty()) {
            if (tipDTO.getContent().length() > 200) {
                throw new RuntimeException("El contenido no puede exceder 200 caracteres");
            }
            tip.setContent(tipDTO.getContent().trim());
        }

        if (tipDTO.getEmoji() != null && !tipDTO.getEmoji().isEmpty()) {
            tip.setEmoji(tipDTO.getEmoji().trim());
        }

        tip.setActive(tipDTO.isActive());
        Tip updated = tipRepository.save(tip);
        log.info("Tip actualizado exitosamente con ID: {}", id);
        return convertToDTO(updated);
    }

    /**
     * Activar/Desactivar un tip
     */
    @Transactional
    public TipDTO toggleTipStatus(Long id) {
        log.info("Alternando estado del tip con ID: {}", id);

        Tip tip = tipRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tip no encontrado con ID: " + id));

        tip.setActive(!tip.isActive());
        Tip updated = tipRepository.save(tip);
        log.info("Estado del tip alternado a: {}", updated.isActive());
        return convertToDTO(updated);
    }

    /**
     * Eliminar un tip (solo para OWNER)
     */
    @Transactional
    public void deleteTip(Long id) {
        log.info("Eliminando tip con ID: {}", id);

        if (!tipRepository.existsById(id)) {
            throw new RuntimeException("Tip no encontrado con ID: " + id);
        }

        tipRepository.deleteById(id);
        log.info("Tip eliminado exitosamente con ID: {}", id);
    }

    /**
     * Buscar tips por título
     */
    @Transactional(readOnly = true)
    public List<TipDTO> searchByTitle(String title) {
        log.debug("Buscando tips por título: {}", title);
        return tipRepository.searchByTitle(title)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convertir Tip a DTO
     */
    private TipDTO convertToDTO(Tip tip) {
        return TipDTO.builder()
            .id(tip.getId())
            .title(tip.getTitle())
            .content(tip.getContent())
            .emoji(tip.getEmoji())
            .active(tip.isActive())
            .createdAt(tip.getCreatedAt())
            .updatedAt(tip.getUpdatedAt())
            .createdByEmail(tip.getCreatedBy().getEmail())
            .build();
    }
}
