package com.checkers.repository;

import com.checkers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// implements user repository for the application
// defines the methods for accessing the user entity in the database

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}