package org.example.repository;

import org.example.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByInn(String inn);

    @Query("SELECT u FROM User u WHERE u.companyName LIKE %:name%")
    List<User> findByCompanyNameContaining(@Param("name") String name);

    boolean existsByEmail(String email);

    boolean existsByInn(String inn);
}