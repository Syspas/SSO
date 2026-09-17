package com.example.sso.user.repository;

import com.example.sso.user.entity.SsoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** CRUD по пользователям SSO. Без бизнес-логики. */
public interface SsoUserRepository extends JpaRepository<SsoUser, Long> {

    Optional<SsoUser> findByEmailIgnoreCase(String email);
}
