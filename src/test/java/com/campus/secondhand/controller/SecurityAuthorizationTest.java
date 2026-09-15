package com.campus.secondhand.controller;

import com.campus.secondhand.common.exception.GlobalExceptionHandler;
import com.campus.secondhand.config.SecurityConfig;
import com.campus.secondhand.controller.admin.AdminUserManagementController;
import com.campus.secondhand.controller.publicapi.PublicItemController;
import com.campus.secondhand.controller.user.UserOrderController;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.JwtAuthenticationFilter;
import com.campus.secondhand.security.JwtProperties;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.service.AdminUserManagementService;
import com.campus.secondhand.service.PublicItemService;
import com.campus.secondhand.service.UserOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限矩阵回归测试:锁定 SecurityConfig 的 URL 放行/角色规则,
 * 防止"新增控制器漏配 matcher、或误把受保护路径加入 permitAll"这类回归。
 */
@WebMvcTest(controllers = {PublicItemController.class, UserOrderController.class, AdminUserManagementController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityAuthorizationTest.TestConfig.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PublicItemService publicItemService;
    @MockBean
    private UserOrderService userOrderService;
    @MockBean
    private AdminUserManagementService adminUserManagementService;
    @MockBean
    private AdminMapper adminMapper;
    @MockBean
    private UserMapper userMapper;

    @Test
    void shouldAllowAnonymousAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/public/items"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousAccessToUserEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/user/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowUserTokenOnUserEndpoints() throws Exception {
        when(userMapper.selectById(11L)).thenReturn(activeUser());
        String token = jwtTokenProvider.createUserToken(11L, "20240001", "active");
        mockMvc.perform(get("/api/v1/user/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAdminTokenOnUserEndpoints() throws Exception {
        when(adminMapper.selectById(1L)).thenReturn(activeAdmin());
        String token = jwtTokenProvider.createAdminToken(1L, "admin1001", "super_admin", "active");
        mockMvc.perform(get("/api/v1/user/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAnonymousAccessToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserTokenOnAdminEndpoints() throws Exception {
        when(userMapper.selectById(11L)).thenReturn(activeUser());
        String token = jwtTokenProvider.createUserToken(11L, "20240001", "active");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminTokenOnAdminEndpoints() throws Exception {
        when(adminMapper.selectById(1L)).thenReturn(activeAdmin());
        String token = jwtTokenProvider.createAdminToken(1L, "admin1001", "super_admin", "active");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private User activeUser() {
        return User.builder()
                .userId(11L)
                .studentNo("20240001")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
    }

    private Admin activeAdmin() {
        return Admin.builder()
                .adminId(1L)
                .adminNo("admin1001")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("UnitTestJwtSecretKey-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            properties.setExpirationSeconds(3600);
            return properties;
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
            return new JwtTokenProvider(jwtProperties);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AdminMapper adminMapper, UserMapper userMapper) {
            return new JwtAuthenticationFilter(jwtTokenProvider, adminMapper, userMapper);
        }
    }
}
