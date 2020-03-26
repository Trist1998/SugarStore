package com.uct.carbbuilder.config.security.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleAccess extends JpaRepository<Role, Long>
{
	Optional<Role> findByName(ERole name);
}