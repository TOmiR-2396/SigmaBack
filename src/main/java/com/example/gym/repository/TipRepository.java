package com.example.gym.repository;

import com.example.gym.model.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TipRepository extends JpaRepository<Tip, Long> {
    
    // Obtener todos los tips activos
    @Query("SELECT t FROM Tip t WHERE t.active = true ORDER BY t.createdAt DESC")
    List<Tip> findAllActive();

    // Obtener tips activos con paginación
    @Query("SELECT t FROM Tip t WHERE t.active = true ORDER BY t.createdAt DESC")
    Page<Tip> findAllActive(Pageable pageable);

    // Obtener todos los tips (incluyendo inactivos) - solo para owner
    @Query("SELECT t FROM Tip t ORDER BY t.createdAt DESC")
    Page<Tip> findAllTips(Pageable pageable);

    // Buscar tips por título
    @Query("SELECT t FROM Tip t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%')) AND t.active = true")
    List<Tip> searchByTitle(@Param("title") String title);
}
