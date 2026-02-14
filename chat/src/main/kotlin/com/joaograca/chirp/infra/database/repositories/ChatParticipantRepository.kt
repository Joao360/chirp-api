package com.joaograca.chirp.infra.database.repositories

import com.joaograca.chirp.domain.type.UserId
import com.joaograca.chirp.infra.database.entities.ChatParticipantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChatParticipantRepository : JpaRepository<ChatParticipantEntity, UserId> {
    fun findByUserIdIn(userIds: List<UserId>): Set<ChatParticipantEntity>
    @Query("""
        SELECT p 
        FROM ChatParticipantEntity p
        WHERE LOWER(p.email) = :query OR LOWER(p.username) = :query
    """,
        nativeQuery = false
    )
    fun findByEmailOrUsername(query: String): ChatParticipantEntity?
}