package com.campus.secondhand.security;

import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminMapper adminMapper;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AdminMapper adminMapper, UserMapper userMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
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
                    String accountType = String.valueOf(claims.get("accountType"));
                    Long accountId = Long.valueOf(String.valueOf(claims.get("accountId")));
                    if ("admin".equals(accountType)) {
                        authenticateAdmin(accountId);
                    } else if ("user".equals(accountType)) {
                        authenticateUser(accountId);
                    }
                } catch (Exception ex) {
                    // 记录无效/被篡改 token 的来源与原因(不打印完整 token),便于发现爆破与篡改行为
                    log.warn("Rejected JWT from {} ({}): {}", request.getRemoteAddr(), request.getRequestURI(), ex.getClass().getSimpleName());
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateAdmin(Long adminId) {
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
    }

    private void authenticateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getAccountStatus() == UserAccountStatus.ACTIVE) {
            UserPrincipal principal = new UserPrincipal(
                    user.getUserId(),
                    user.getStudentNo(),
                    user.getRealName(),
                    user.getEmail(),
                    user.getAccountStatus()
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}