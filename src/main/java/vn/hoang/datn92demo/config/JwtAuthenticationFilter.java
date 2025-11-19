package vn.hoang.datn92demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Bỏ qua xác thực cho các endpoint public
        if (path.startsWith("/api/auth") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    String phone = jwtTokenProvider.getPhoneFromToken(token);
                    String roleFromToken = jwtTokenProvider.getRoleFromToken(token); // có thể "ADMIN" hoặc "ROLE_ADMIN"

                    if (roleFromToken == null || roleFromToken.isBlank()) {
                        logger.warn("Token không có role claim (token principal: {})", phone);
                    } else {
                        // Nếu role đã có prefix "ROLE_" thì không thêm nữa
                        String authorityRole = roleFromToken.startsWith("ROLE_") ? roleFromToken : ("ROLE_" + roleFromToken);

                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityRole);

                        // principal hiện là phone — ensure consistency: controllers use authentication.getName() expecting phone
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        phone, null, Collections.singletonList(authority)
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        logger.debug("Set authentication for principal={}, authorities={}", phone, Collections.singletonList(authorityRole));
                    }
                } else {
                    logger.debug("JWT token không hợp lệ hoặc hết hạn");
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
