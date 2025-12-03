package com.example.chatgpt.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",     // React 개발서버
            "http://localhost:8000",     // 프론트엔드 서버
            "http://localhost:8080",     // 추가 개발서버
            "*"                          // 개발 테스트용 (운영에서는 제거)
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false); // "*" origin 사용시 false 필요
        configuration.setMaxAge(3600L); // preflight 캐시 시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // ✅ 우리 프로젝트 API 경로들
                                "/api/v1/events",                    // 행사 관리 API
                                "/api/v1/events/**",                 // 행사 상세/상태변경 API
                                "/api/capability/**",                // 기업역량 API
                                "/api/upload",                       // 파일 업로드
                                "/api/analyze/**",                   // 사업계획서 분석
                                "/api/save-answers",                 // 답변 저장
                                "/api/classify-cost",                // 비용 분류
                                "/api/classify-stage-costs",         // Stage별 비용 분류
                                "/api/generate-income-statement",    // 손익계산서 생성
                                "/stage/**",                         // Stage 관리
                                "/api/check-config",                 // OpenAI 설정 확인
                                "/api/test-rate-limit",              // Rate Limit 테스트
                                "/api/test-short",                   // 테스트
                                "/api/excel/test",                   // 엑셀 테스트
                                
                                // ✅ 관리자 API
                                "/api/admin/**",                     // 관리자 전체
                                "/execute-revenue-distribution",     // 매출분배
                                "/execute-summary-and-distribution", // 요약+분배
                                "/rankings",                         // 순위 조회
                                "/dashboard/**",                     // 대시보드
                                
                                // ✅ 정적 리소스 및 기본 페이지
                                "/",                                 // 메인 페이지
                                "/stage",                            // 스테이지 페이지
                                "/fileTest",                         // 파일 업로드 테스트 페이지
                                "/irFileTest",                       // 파일 업로드 테스트 페이지
                                "/stage6FileTest",                   // 파일 업로드 테스트 페이지
                                "/css/**", "/js/**", "/images/**",  // 정적 리소스
                                "/static/**",                        // 정적 리소스
                                "/favicon.ico",                      // 파비콘
                                
                                // ✅ 개발/테스트용 (운영에서는 제거 고려)
                                "/api/**",                           // 모든 API (개발용)
                                "/h2-console/**",                    // H2 DB 콘솔 (개발용)
                                "/actuator/**"                       // Spring Boot Actuator
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        // H2 Console 사용시 (개발환경)
        http.headers(headers -> headers.frameOptions().sameOrigin());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}