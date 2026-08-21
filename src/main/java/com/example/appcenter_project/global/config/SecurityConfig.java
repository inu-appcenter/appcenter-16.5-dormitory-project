package com.example.appcenter_project.global.config;

import com.example.appcenter_project.global.security.jwt.JwtFilter;
import com.example.appcenter_project.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        /** 스웨거 **/
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()

                        /** 이미지 **/
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/static/**", "/chat-test.html").permitAll()

                        /** 파일 **/
                        .requestMatchers("/files/**").permitAll()

                        /** 유저 **/
                        // 로그인
                        .requestMatchers(POST, "/users", "/users/freshman", "/users/freshman/register", "/users/refreshToken").permitAll()
                        // 관리자 모든 유저 조회
                        .requestMatchers("/users/all/admin").hasRole("ADMIN")
                        // 특정 유저 푸시 알림 전송
                        .requestMatchers( "/users/push-notification").hasRole("ADMIN")
                        // 사용자 권한 변경 (ADMIN 전용)
                        .requestMatchers(PATCH, "/users/role").hasRole("ADMIN")
                        // 사용자 권한 조회
                        .requestMatchers(GET, "/users/role")
                            .hasAnyRole("DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER","ADMIN")
                        // 사용자 정보 조회 및 수정
                        .requestMatchers("/users", "/users/image", "/users/time-table-image").authenticated()
                        // 사용자 게시글 조회(좋아요, 자신의 게시글)
                        .requestMatchers("/users/board", "/users/like").authenticated()

                        /** 공지사항 **/
                        // 공지사항 조회
                        .requestMatchers(GET, "/announcements/**").permitAll()
                        // 공지사항 등록, 수정, 삭제
                        .requestMatchers("/announcements/**")
                            .hasAnyRole("DORM_SUPPORTERS", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER","ADMIN")

                        /** 공동구매 **/
                        // 검색 기록 조회
                        .requestMatchers(GET, "/group-orders/searchLog").authenticated()
                        // 공동구매 조회(목록, 단건)
                        .requestMatchers(GET, "/group-orders/**").permitAll()
                        // 공동구매 등록, 수정, 삭제, 좋아요, 완료 처리
                        .requestMatchers("/group-orders/**").authenticated()
                        // 댓글 등록, 삭제
                        .requestMatchers("/group-order-comments/**").authenticated()

                        /** 룸메이트 **/
                        // 나와 유사한 룸메이트 조회
                        .requestMatchers(GET, "/roommates/similar").authenticated()
                        // 나의 체크리스트 조회
                        .requestMatchers(GET, "/roommates/my-checklist").authenticated()
                        // 이전 학기 체크리스트 불러오기
                        .requestMatchers(GET, "/roommates/my-checklist/previous").authenticated()
                        // 게시글 좋아요 여부 확인
                        .requestMatchers(GET, "/roommates/{boardId}/liked").authenticated()
                        // 룸메이트 게시글 조회
                        .requestMatchers(GET, "/roommates/**").permitAll()
                        // 룸메이트 게시글 등록, 수정, 삭제
                        .requestMatchers("/roommates/**").authenticated()
                        // 나의 룸메이트
                        .requestMatchers("/my-roommate/**").authenticated()
                        // 룸메이트 매칭
                        .requestMatchers("/roommate-matching/**").authenticated()
                        // 룸메이트 채팅방
                        .requestMatchers("/roommate-chatting-room/**").authenticated()
                        // 룸메이트 채팅
                        .requestMatchers("/roommate/chat/**").authenticated()

                        /** 채팅 **/
                        .requestMatchers("/ws-stomp", "/health").permitAll()

                        /** 팁 **/
                        // 팁 조회
                        .requestMatchers(GET, "/tips/**").permitAll()
                        // 팁 좋아요
                        .requestMatchers(PATCH, "/tips/**").authenticated()
                        // 팁 등록, 수정, 삭제
                        .requestMatchers("/tips/**")
                            .hasAnyRole("DORM_SUPPORTERS", "ADMIN")
                        // 팁 댓글 등록, 삭제
                        .requestMatchers("/tip-comments/**").authenticated()

                        /** 민원 **/
                        // 일반 사용자
                        .requestMatchers("/complaints/**").authenticated()
                        // 관리자, 기숙사 담당자
                        .requestMatchers("/admin/complaints/**")
                            .hasAnyRole("DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER","ADMIN")

                        /** 캘린더 **/
                        // 캘린더 조회
                        .requestMatchers(GET, "/calenders/**").permitAll()
                        // 캘린더 등록, 수정, 삭제
                        .requestMatchers("/calenders/**")
                            .hasAnyRole("DORM_SUPPORTERS", "ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER")

                        /** AI캘린더 **/
                        .requestMatchers("/admin/ai-schedule/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_DORM_MANAGER")

                        /** 알림 **/
                        // 특정 유저 1:1 직접 알림 전송(ADMIN 전용)
                        .requestMatchers(POST, "/notifications/admin/direct").hasRole("ADMIN")
                        // 알림 읽음 처리(로그인한 사용자)
                        .requestMatchers(PATCH, "/notifications/read").authenticated()
                        // 알림 조회(로그인한 사용자)
                        .requestMatchers(GET, "/notifications/**").permitAll()
                        // 알림 등록, 수정, 삭제(관리자)
                        .requestMatchers("/notifications/**").hasAnyRole("ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER","DORM_MANAGER")

                        /** 팝업 알림 **/
                        // 팝업 알림 조회
                        .requestMatchers(GET, "/popup-notifications/admin").hasAnyRole("ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER","DORM_SUPPORTERS") // 전체 조회는 ADMIN만 가능
                        .requestMatchers(GET, "/popup-notifications/**").permitAll()
                        // 팝업 알림 등록, 수정, 삭제(관리자)
                        .requestMatchers("/popup-notifications/**").hasAnyRole("ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER", "DORM_SUPPORTERS")

                        /** 사용자 알림 **/
                        .requestMatchers("/user-notifications/**").authenticated()

                        /** FCM **/
                        .requestMatchers(GET, "/fcm/stats").hasRole("ADMIN")
                        .requestMatchers(GET, "/fcm/dlq").hasRole("ADMIN")
                        .requestMatchers(POST, "/fcm/dlq/*/retry").hasRole("ADMIN")
                        .requestMatchers("/fcm/token/**").permitAll()

                        /** 쿠폰 **/
                        .requestMatchers(POST, "/coupons/admin/stock").hasRole("ADMIN")
                        .requestMatchers("/coupons/**").authenticated()

                        /** 설문조사 **/
                        .requestMatchers(POST, "/surveys/responses").authenticated() // 응답 로그인한 이용자 가능
                        .requestMatchers(GET, "/surveys/*/results", "/surveys/*/export/csv").hasAnyRole("ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER", "DORM_SUPPORTERS")
                        .requestMatchers(GET, "/surveys/**").permitAll() // 설문 단순 조회 모두 가능
                        .requestMatchers("/surveys/**").hasAnyRole("ADMIN", "DORM_LIFE_MANAGER", "DORM_ROOMMATE_MANAGER", "DORM_MANAGER", "DORM_EXPEDITED_COMPLAINT_MANAGER", "DORM_SUPPORTERS")

                        /** API 통계 **/
                        .requestMatchers("/statistics/**").hasRole("ADMIN")

                        /** 피처 플래그 **/
                        .requestMatchers("/features/**").permitAll()

                        /** 오픈 채팅 **/
                        .requestMatchers("/admin/open-chat-reports/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/admin/open-chat-rooms").hasRole("ADMIN")
                        .requestMatchers(POST, "/admin/open-chat-rooms/*/bot").hasRole("ADMIN")
                        .requestMatchers("/open-chat-rooms/**").authenticated()

                        /** 학번 공개 **/
                        .requestMatchers("/student-id-disclosures/**").authenticated()

                        /** 장소 검색 **/
                        .requestMatchers(GET, "/places/search").authenticated()

                        /** 어드민 장소 관리 **/
                        .requestMatchers("/admin/places/**").hasRole("ADMIN")

                        /** 차단 **/
                        .requestMatchers("/block/**").authenticated()

                        /** 날씨 **/
                        .requestMatchers(GET, "/weather").permitAll()

                        /** 나머지 **/
                        .anyRequest().authenticated()
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가

        return http.build();
    }
}
