package com.campus.secondhand.service.impl;

import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.SystemInitService;
import com.campus.secondhand.vo.admin.BootstrapResponse;
import com.campus.secondhand.vo.admin.InitStatusResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SystemInitServiceImpl implements SystemInitService {

    private static final String DEFAULT_ADMIN_NO = "admin1001";
    private static final String DEFAULT_ADMIN_NAME = "Campus Admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@campus.local";
    private static final String DEFAULT_ADMIN_HASH = "$2b$10$TlJI8z2zK0VwcUlz8h2X4e1/ffGwlwzN0ujwhpldA1Ks/0yVcMGpW";
    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("digital_devices", "数码设备", 10),
            new DefaultCategory("books_notes", "教材书籍", 20),
            new DefaultCategory("sports_goods", "运动用品", 30),
            new DefaultCategory("dorm_supplies", "宿舍用品", 40),
            new DefaultCategory("daily_use", "日常生活", 50),
            new DefaultCategory("tickets_cards", "票券卡券", 60)
    );

    private final ItemCategoryMapper itemCategoryMapper;
    private final AdminMapper adminMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public SystemInitServiceImpl(ItemCategoryMapper itemCategoryMapper,
                                 AdminMapper adminMapper,
                                 AdminOperationLogMapper adminOperationLogMapper) {
        this.itemCategoryMapper = itemCategoryMapper;
        this.adminMapper = adminMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public BootstrapResponse bootstrap(AdminPrincipal principal) {
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (DefaultCategory defaultCategory : DEFAULT_CATEGORIES) {
            ItemCategory existing = itemCategoryMapper.selectByCategoryCode(defaultCategory.categoryCode());
            if (existing == null) {
                itemCategoryMapper.insert(ItemCategory.builder()
                        .parentId(null)
                        .categoryCode(defaultCategory.categoryCode())
                        .categoryName(defaultCategory.categoryName())
                        .sortOrder(defaultCategory.sortOrder())
                        .isEnabled(1)
                        .build());
                created++;
                continue;
            }

            boolean changed = !Objects.equals(existing.getCategoryName(), defaultCategory.categoryName())
                    || !Objects.equals(existing.getSortOrder(), defaultCategory.sortOrder())
                    || !Objects.equals(existing.getIsEnabled(), 1)
                    || existing.getParentId() != null;
            if (changed) {
                existing.setParentId(null);
                existing.setCategoryName(defaultCategory.categoryName());
                existing.setSortOrder(defaultCategory.sortOrder());
                existing.setIsEnabled(1);
                itemCategoryMapper.updateById(existing);
                updated++;
            } else {
                skipped++;
            }
        }

        Admin defaultAdmin = adminMapper.selectByAdminNo(DEFAULT_ADMIN_NO);
        if (defaultAdmin == null) {
            defaultAdmin = Admin.builder()
                    .adminNo(DEFAULT_ADMIN_NO)
                    .passwordHash(DEFAULT_ADMIN_HASH)
                    .adminName(DEFAULT_ADMIN_NAME)
                    .email(DEFAULT_ADMIN_EMAIL)
                    .roleCode(AdminRoleCode.SUPER_ADMIN)
                    .accountStatus(AdminAccountStatus.ACTIVE)
                    .build();
            adminMapper.insert(defaultAdmin);
            created++;
        } else {
            boolean changed = !Objects.equals(defaultAdmin.getPasswordHash(), DEFAULT_ADMIN_HASH)
                    || !Objects.equals(defaultAdmin.getAdminName(), DEFAULT_ADMIN_NAME)
                    || !Objects.equals(defaultAdmin.getEmail(), DEFAULT_ADMIN_EMAIL)
                    || defaultAdmin.getRoleCode() != AdminRoleCode.SUPER_ADMIN
                    || defaultAdmin.getAccountStatus() != AdminAccountStatus.ACTIVE;
            if (changed) {
                defaultAdmin.setPasswordHash(DEFAULT_ADMIN_HASH);
                defaultAdmin.setAdminName(DEFAULT_ADMIN_NAME);
                defaultAdmin.setEmail(DEFAULT_ADMIN_EMAIL);
                defaultAdmin.setRoleCode(AdminRoleCode.SUPER_ADMIN);
                defaultAdmin.setAccountStatus(AdminAccountStatus.ACTIVE);
                adminMapper.updateById(defaultAdmin);
                updated++;
            } else {
                skipped++;
            }
        }

        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(principal.getAdminId())
                .targetType("user")
                .targetId(defaultAdmin.getAdminId())
                .operationType("other")
                .operationDetail(String.format("{\"action\":\"bootstrap\",\"createdCount\":%d,\"updatedCount\":%d,\"skippedCount\":%d}", created, updated, skipped))
                .ipAddress(null)
                .build();
        adminOperationLogMapper.insert(log);

        return new BootstrapResponse(created, updated, skipped);
    }

    @Override
    public InitStatusResponse getStatus(AdminPrincipal principal) {
        Long categoryCount = itemCategoryMapper.selectCount(null);
        Admin defaultAdmin = adminMapper.selectByAdminNo(DEFAULT_ADMIN_NO);
        LocalDateTime lastBootstrapAt = adminOperationLogMapper.selectLatestBootstrapAt();
        return new InitStatusResponse(
                categoryCount == null ? 0 : categoryCount,
                defaultAdmin != null,
                principal.getRoleCode() == AdminRoleCode.SUPER_ADMIN,
                lastBootstrapAt
        );
    }

    private record DefaultCategory(String categoryCode, String categoryName, Integer sortOrder) {
    }
}