package com.campus.secondhand.service;

import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.SystemInitServiceImpl;
import com.campus.secondhand.vo.admin.BootstrapResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemInitServiceTest {

    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private SystemInitServiceImpl systemInitService;

    @Test
    void shouldCreateDefaultsWhenDataMissing() {
        when(itemCategoryMapper.selectByCategoryCode(anyString())).thenReturn(null);
        when(adminMapper.selectByAdminNo("admin1001")).thenReturn(null);
        when(adminMapper.insert(any(Admin.class))).thenAnswer(invocation -> {
            Admin admin = invocation.getArgument(0);
            admin.setAdminId(1001L);
            return 1;
        });

        BootstrapResponse response = systemInitService.bootstrap(principal());

        assertEquals(7, response.createdCount());
        assertEquals(0, response.updatedCount());
        assertEquals(0, response.skippedCount());
        verify(itemCategoryMapper, times(6)).insert(any(ItemCategory.class));
        verify(adminMapper).insert(any(Admin.class));
        verify(adminOperationLogMapper).insert(any(AdminOperationLog.class));
    }

    @Test
    void shouldSkipWhenDefaultsAlreadyAligned() {
        when(itemCategoryMapper.selectByCategoryCode(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return switch (code) {
                case "digital_devices" -> category(code, "数码设备", 10);
                case "books_notes" -> category(code, "教材书籍", 20);
                case "sports_goods" -> category(code, "运动用品", 30);
                case "dorm_supplies" -> category(code, "宿舍用品", 40);
                case "daily_use" -> category(code, "日常生活", 50);
                default -> category(code, "票券卡券", 60);
            };
        });
        when(adminMapper.selectByAdminNo("admin1001")).thenReturn(Admin.builder()
                .adminId(1001L)
                .adminNo("admin1001")
                .passwordHash("$2b$10$TlJI8z2zK0VwcUlz8h2X4e1/ffGwlwzN0ujwhpldA1Ks/0yVcMGpW")
                .adminName("Campus Admin")
                .email("admin@campus.local")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build());

        BootstrapResponse response = systemInitService.bootstrap(principal());

        assertEquals(0, response.createdCount());
        assertEquals(0, response.updatedCount());
        assertEquals(7, response.skippedCount());
        verify(itemCategoryMapper, never()).insert(any(ItemCategory.class));
        verify(itemCategoryMapper, never()).updateById(any(ItemCategory.class));
        verify(adminMapper, never()).insert(any(Admin.class));
        verify(adminMapper, never()).updateById(any(Admin.class));
        verify(adminOperationLogMapper).insert(any(AdminOperationLog.class));
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN);
    }

    private ItemCategory category(String code, String name, Integer order) {
        return ItemCategory.builder()
                .categoryCode(code)
                .categoryName(name)
                .sortOrder(order)
                .isEnabled(1)
                .build();
    }
}
