package com.example.scaffold.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByStatusOrderByCreatedAtDesc(AccountStatus status);

    @Query("SELECT u FROM User u WHERE "
           + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
           + "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
           + "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> findByNameContainingIgnoreCase(String query);
}
