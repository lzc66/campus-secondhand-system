package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.UpdateUserStatusRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminUserManagementService;
import com.campus.secondhand.vo.admin.AdminUserDetailResponse;
import com.campus.secondhand.vo.admin.AdminUserPageResponse;
import com.campus.secondhand.vo.admin.AdminUserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final UserMapper userMapper;
    private final MediaFileMapper mediaFileMapper;
    private final ItemMapper itemMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminUserManagementServiceImpl(UserMapper userMapper,
                                          MediaFileMapper mediaFileMapper,
                                          ItemMapper itemMapper,
                                          AdminOperationLogMapper adminOperationLogMapper) {
        this.userMapper = userMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.itemMapper = itemMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public AdminUserPageResponse list(String accountStatus, String studentNo, String realName, long page, long size) {
        UserAccountStatus statusFilter = parseAccountStatus(accountStatus, true);
        Page<User> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<User> result = userMapper.selectPage(queryPage, new LambdaQueryWrapper<User>()
                .eq(statusFilter != null, User::getAccountStatus, statusFilter)
                .like(StringUtils.hasText(studentNo), User::getStudentNo, studentNo == null ? null : studentNo.trim())
                .like(StringUtils.hasText(realName), User::getRealName, realName == null ? null : realName.trim())
                .orderByDesc(User::getCreatedAt));
        List<AdminUserSummaryResponse> records = result.getRecords().stream()
                .map(this::toSummary)
                .toList();
        return new AdminUserPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public AdminUserDetailResponse detail(Long userId) {
        return toDetail(getRequiredUser(userId));
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateStatus(AdminPrincipal principal, Long userId, UpdateUserStatusRequest request) {
        User user = getRequiredUser(userId);
        UserAccountStatus targetStatus = parseAccountStatus(request.accountStatus(), false);
        user.setAccountStatus(targetStatus);
        userMapper.updateById(user);
        logOperation(principal.getAdminId(), user.getUserId(), "update_status", "{\"accountStatus\":\"" + targetStatus.getValue() + "\"}");
        return toDetail(user);
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private AdminUserSummaryResponse toSummary(User user) {
        return new AdminUserSummaryResponse(
                user.getUserId(),
                user.getStudentNo(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getCollegeName(),
                user.getAccountStatus().getValue(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private AdminUserDetailResponse toDetail(User user) {
        MediaFile avatar = user.getAvatarFileId() == null ? null : mediaFileMapper.selectById(user.getAvatarFileId());
        Long publishedItemCount = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getSellerUserId, user.getUserId())
                .ne(Item::getStatus, ItemStatus.DELETED));
        return new AdminUserDetailResponse(
                user.getUserId(),
                user.getStudentNo(),
                user.getRealName(),
                user.getEmail(),
                user.getGender() == null ? null : user.getGender().getValue(),
                user.getPhone(),
                user.getQqNo(),
                user.getWechatNo(),
                avatar == null ? null : avatar.getFileUrl(),
                user.getCollegeName(),
                user.getMajorName(),
                user.getClassName(),
                user.getDormitoryAddress(),
                user.getAccountStatus().getValue(),
                publishedItemCount == null ? 0 : publishedItemCount.intValue(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserAccountStatus parseAccountStatus(String value, boolean nullable) {
        if (!StringUtils.hasText(value)) {
            if (nullable) {
                return null;
            }
            throw new BusinessException(40020, HttpStatus.BAD_REQUEST, "accountStatus is required");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "active" -> UserAccountStatus.ACTIVE;
            case "disabled" -> UserAccountStatus.DISABLED;
            case "locked" -> UserAccountStatus.LOCKED;
            default -> throw new BusinessException(40021, HttpStatus.BAD_REQUEST, "accountStatus is invalid");
        };
    }

    private void logOperation(Long adminId, Long userId, String operationType, String operationDetail) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("user")
                .targetId(userId)
                .operationType(operationType)
                .operationDetail(operationDetail)
                .ipAddress(null)
                .build());
    }
}
