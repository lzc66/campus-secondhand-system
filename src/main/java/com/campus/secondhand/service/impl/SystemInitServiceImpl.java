package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.util.JsonUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    @Transactional
    public BootstrapResponse bootstrap(AdminPrincipal principal) {
        int created = 0;
        int skipped = 0;

        // 已存在的分类只跳过、绝不覆盖:管理员在后台对分类的下架/改名/排序调整不应被 bootstrap 静默回滚。
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
            } else {
                skipped++;
            }
        }

        // 管理员已存在时只跳过、绝不覆盖:密码、角色、启用状态一律保持现状,避免把改过的密码重置回默认值、
        // 把被停用的账号重新激活。
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
            skipped++;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "bootstrap");
        payload.put("createdCount", created);
        payload.put("skippedCount", skipped);
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(principal.getAdminId())
                .targetType("user")
                .targetId(defaultAdmin.getAdminId())
                .operationType("other")
                .operationDetail(JsonUtil.toJson(payload))
                .ipAddress(null)
                .build();
        adminOperationLogMapper.insert(log);

        return new BootstrapResponse(created, 0, skipped);
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