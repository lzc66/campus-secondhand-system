package com.campus.secondhand.security;

import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.mapper.AdminMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminMapper adminMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AdminMapper adminMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminMapper = adminMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtTokenProvider.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Claims claims = jwtTokenProvider.parseClaims(token);
                    Long adminId = Long.valueOf(claims.get("adminId").toString());
                    Admin admin = adminMapper.selectById(adminId);
                    if (admin != null && admin.getAccountStatus() == AdminAccountStatus.ACTIVE) {
                        AdminPrincipal principal = new AdminPrincipal(
                                admin.getAdminId(),
                                admin.getAdminNo(),
                                admin.getAdminName(),
                                admin.getEmail(),
                                admin.getRoleCode()
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
