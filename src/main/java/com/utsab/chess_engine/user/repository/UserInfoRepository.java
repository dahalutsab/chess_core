package com.utsab.chess_engine.user.repository;

import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserEntity,Long> {
    /** With the roles in the same query, as every authenticated request looks the user up */
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByEmailId(String emailId);

    /** Registered, verified users other than the given one whose name contains the query or whose email is exactly it */
    @Query("""
            select u from UserEntity u
            where u.id <> :excludeId
              and u.isVerified = true
              and (u.guest is null or u.guest = false)
              and (lower(u.fullName) like lower(concat('%', :query, '%')) or lower(u.emailId) = lower(:query))
            order by u.fullName
            """)
    List<UserEntity> search(@Param("query") String query, @Param("excludeId") Long excludeId, Pageable pageable);
}
