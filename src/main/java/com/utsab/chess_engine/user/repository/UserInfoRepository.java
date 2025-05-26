package com.utsab.chess_engine.user.repository;

import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByEmailId(String emailId);

}
