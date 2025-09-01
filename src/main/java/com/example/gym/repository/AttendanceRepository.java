package com.example.gym.repository;

import com.example.gym.model.Attendance;
import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUser(User user);
}
