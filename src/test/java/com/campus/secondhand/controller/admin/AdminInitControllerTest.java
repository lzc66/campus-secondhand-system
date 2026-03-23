package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.exception.GlobalExceptionHandler;
import com.campus.secondhand.config.SecurityConfig;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.security.JwtAuthenticationFilter;
import com.campus.secondhand.security.JwtProperties;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.service.SystemInitService;
import com.campus.secondhand.vo.admin.BootstrapResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminInitController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, AdminInitControllerTest.TestConfig.class})
class AdminInitControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SystemInitService systemInitService;
    @MockBean
    private AdminMapper adminMapper;

    @Test
    void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(post("/api/v1/admin/init/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenRoleNotSuperAdmin() throws Exception {
        when(adminMapper.selectById(2L)).thenReturn(Admin.builder()
                .adminId(2L)
                .adminNo("admin1002")
                .adminName("Operator")
                .email("operator@campus.local")
                .roleCode(AdminRoleCode.OPERATOR)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build());

        String token = jwtTokenProvider.createToken(2L, "admin1002", "operator");

        mockMvc.perform(post("/api/v1/admin/init/bootstrap")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBootstrapWhenRoleIsSuperAdmin() throws Exception {
        when(adminMapper.selectById(1L)).thenReturn(Admin.builder()
                .adminId(1L)
                .adminNo("admin1001")
                .adminName("Campus Admin")
                .email("admin@campus.local")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build());
        when(systemInitService.bootstrap(any())).thenReturn(new BootstrapResponse(1, 2, 4));

        String token = jwtTokenProvider.createToken(1L, "admin1001", "super_admin");

        mockMvc.perform(post("/api/v1/admin/init/bootstrap")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.updatedCount").value(2))
                .andExpect(jsonPath("$.data.skippedCount").value(4));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("CampusSecondhandJwtSecretKeyForAdminInit1234567890");
            properties.setExpirationSeconds(3600);
            return properties;
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
            return new JwtTokenProvider(jwtProperties);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AdminMapper adminMapper) {
            return new JwtAuthenticationFilter(jwtTokenProvider, adminMapper);
        }
    }
}
