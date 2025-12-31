package com.joaograca.chirp.infra.database.repositories

import com.joaograca.chirp.domain.model.UserId
import com.joaograca.chirp.infra.database.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, UserId> {
    fun findByEmail(email: String): UserEntity?
    fun findByEmailOrUsername(email: String, username: String): List<UserEntity>
}