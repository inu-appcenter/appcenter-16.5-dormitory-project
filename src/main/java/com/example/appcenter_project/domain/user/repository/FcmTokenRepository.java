package com.example.appcenter_project.domain.user.repository;

import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken,Long> {
    @Transactional
    @Modifying
    @Query("DELETE FROM FcmToken ft WHERE ft.token = :token")
    void deleteByToken(@Param("token") String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM FcmToken ft WHERE ft.token IN :tokens")
    void deleteAllByTokenIn(@Param("tokens") List<String> tokens);

    List<FcmToken> findAllByUserIn(List<User> users);
    boolean existsByToken(String token);
    Optional<FcmToken> findByUser(User user);
    List<FcmToken> findAllByUser(User user);

    FcmToken findByToken(String token);

    @Query("SELECT ft FROM FcmToken ft WHERE ft.user.id IN :userIds")
    List<FcmToken> findAllByUserIdIn(@Param("userIds") List<Long> userIds);

    List<FcmToken> findAllByTokenIn(List<String> tokens);
}
