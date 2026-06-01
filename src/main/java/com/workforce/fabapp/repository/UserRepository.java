package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.User;
import com.workforce.fabapp.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByActiveTrue();
    long countByActiveTrue();

    @Query("""
            select user
            from User user
            left join fetch user.employee
            left join fetch user.supervisor
            where user.active = true
            """)
    List<User> findByActiveTrueWithLinks();

    List<User> findByRole(Role role);
}
