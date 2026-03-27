package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.ReviewRegistrationApplicationRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.RegistrationStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.service.RegistrationReviewService;
import com.campus.secondhand.vo.admin.RegistrationApplicationDetailResponse;
import com.campus.secondhand.vo.admin.RegistrationApplicationPageResponse;
import com.campus.secondhand.vo.admin.RegistrationApplicationSummaryResponse;
import com.campus.secondhand.vo.admin.ReviewRegistrationResponse;
import com.campus.secondhand.vo.common.MediaFileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class RegistrationReviewServiceImpl implements RegistrationReviewService {

    private final RegistrationApplicationMapper registrationApplicationMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public RegistrationReviewServiceImpl(RegistrationApplicationMapper registrationApplicationMapper,
                                         UserMapper userMapper,
                                         FileStorageService fileStorageService,
                                         NotificationService notificationService,
                                         AdminOperationLogMapper adminOperationLogMapper) {
        this.registrationApplicationMapper = registrationApplicationMapper;
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public RegistrationApplicationPageResponse list(String status, String studentNo, String email, long page, long size) {
        RegistrationStatus statusFilter = parseStatus(status);
        Page<RegistrationApplication> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<RegistrationApplication> wrapper = new LambdaQueryWrapper<RegistrationApplication>()
                .eq(statusFilter != null, RegistrationApplication::getStatus, statusFilter)
                .like(StringUtils.hasText(studentNo), RegistrationApplication::getStudentNo, studentNo)
                .like(StringUtils.hasText(email), RegistrationApplication::getEmail, email)
                .orderByDesc(RegistrationApplication::getSubmittedAt);
        Page<RegistrationApplication> result = registrationApplicationMapper.selectPage(queryPage, wrapper);
        List<RegistrationApplicationSummaryResponse> records = result.getRecords().stream()
                .map(item -> new RegistrationApplicationSummaryResponse(
                        item.getApplicationId(),
                        item.getApplicationNo(),
                        item.getStudentNo(),
                        item.getRealName(),
                        item.getEmail(),
                        item.getStatus().getValue(),
                        item.getSubmittedAt(),
                        item.getReviewedAt()
                ))
                .toList();
        return new RegistrationApplicationPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public RegistrationApplicationDetailResponse detail(Long applicationId) {
        RegistrationApplication application = getRequiredApplication(applicationId);
        MediaFile mediaFile = fileStorageService.getRequiredFile(application.getStudentCardFileId());
        return toDetail(application, mediaFile);
    }

    @Override
    @Transactional
    public ReviewRegistrationResponse approve(Long applicationId, ReviewRegistrationApplicationRequest request, AdminPrincipal principal, String ipAddress) {
        RegistrationApplication application = getRequiredApplication(applicationId);
        ensurePending(application);
        if (userMapper.selectByStudentNo(application.getStudentNo()) != null) {
            throw new BusinessException(40920, HttpStatus.CONFLICT, "Student number already exists in users");
        }
        if (userMapper.selectByEmail(application.getEmail()) != null) {
            throw new BusinessException(40921, HttpStatus.CONFLICT, "Email already exists in users");
        }

        User user = User.builder()
                .applicationId(application.getApplicationId())
                .studentNo(application.getStudentNo())
                .email(application.getEmail())
                .passwordHash(application.getPasswordHash())
                .realName(application.getRealName())
                .gender(application.getGender())
                .phone(application.getPhone())
                .collegeName(application.getCollegeName())
                .majorName(application.getMajorName())
                .className(application.getClassName())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        userMapper.insert(user);

        LocalDateTime now = LocalDateTime.now();
        application.setStatus(RegistrationStatus.APPROVED);
        application.setReviewerAdminId(principal.getAdminId());
        application.setReviewRemark(trimToNull(request.reviewRemark()));
        application.setReviewedAt(now);
        registrationApplicationMapper.updateById(application);

        notificationService.sendRegistrationApproved(application, user, principal.getAdminId());
        saveAuditLog(principal.getAdminId(), application.getApplicationId(), "approve", application.getReviewRemark(), ipAddress);

        return new ReviewRegistrationResponse(application.getApplicationId(), application.getStatus().getValue(), user.getUserId(), now);
    }

    @Override
    @Transactional
    public ReviewRegistrationResponse reject(Long applicationId, ReviewRegistrationApplicationRequest request, AdminPrincipal principal, String ipAddress) {
        RegistrationApplication application = getRequiredApplication(applicationId);
        ensurePending(application);

        LocalDateTime now = LocalDateTime.now();
        application.setStatus(RegistrationStatus.REJECTED);
        application.setReviewerAdminId(principal.getAdminId());
        application.setReviewRemark(trimToNull(request.reviewRemark()));
        application.setReviewedAt(now);
        registrationApplicationMapper.updateById(application);

        notificationService.sendRegistrationRejected(application, principal.getAdminId());
        saveAuditLog(principal.getAdminId(), application.getApplicationId(), "reject", application.getReviewRemark(), ipAddress);

        return new ReviewRegistrationResponse(application.getApplicationId(), application.getStatus().getValue(), null, now);
    }

    private RegistrationApplication getRequiredApplication(Long applicationId) {
        RegistrationApplication application = registrationApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(40430, HttpStatus.NOT_FOUND, "Registration application not found");
        }
        return application;
    }

    private void ensurePending(RegistrationApplication application) {
        if (application.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException(40922, HttpStatus.CONFLICT, "Only pending applications can be reviewed");
        }
    }

    private RegistrationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pending" -> RegistrationStatus.PENDING;
            case "approved" -> RegistrationStatus.APPROVED;
            case "rejected" -> RegistrationStatus.REJECTED;
            case "cancelled" -> RegistrationStatus.CANCELLED;
            default -> throw new BusinessException(40030, HttpStatus.BAD_REQUEST, "status filter is invalid");
        };
    }

    private RegistrationApplicationDetailResponse toDetail(RegistrationApplication application, MediaFile mediaFile) {
        return new RegistrationApplicationDetailResponse(
                application.getApplicationId(),
                application.getApplicationNo(),
                application.getStudentNo(),
                application.getRealName(),
                application.getGender().getValue(),
                application.getEmail(),
                application.getPhone(),
                application.getCollegeName(),
                application.getMajorName(),
                application.getClassName(),
                application.getStatus().getValue(),
                application.getReviewerAdminId(),
                application.getReviewRemark(),
                application.getReviewedAt(),
                application.getSubmittedAt(),
                new MediaFileResponse(
                        mediaFile.getFileId(),
                        mediaFile.getFileUrl(),
                        mediaFile.getOriginalName(),
                        mediaFile.getFileSize(),
                        mediaFile.getMimeType()
                )
        );
    }

    private void saveAuditLog(Long adminId, Long applicationId, String operationType, String reviewRemark, String ipAddress) {
        String escapedRemark = reviewRemark == null ? null : reviewRemark.replace("\\", "\\\\").replace("\"", "\\\"");
        String detail = String.format(
                "{\"status\":\"%s\",\"reviewRemark\":%s}",
                operationType,
                escapedRemark == null ? null : "\"" + escapedRemark + "\""
        );
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("registration")
                .targetId(applicationId)
                .operationType(operationType)
                .operationDetail(detail)
                .ipAddress(ipAddress)
                .build());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}