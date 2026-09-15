package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.common.util.JsonUtil;
import com.campus.secondhand.config.StorageProperties;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminFileService;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.vo.admin.MediaFileDownloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminFileServiceImpl implements AdminFileService {

    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "image/jpeg", "image/jpeg",
            "image/png", "image/png",
            "image/gif", "image/gif",
            "image/webp", "image/webp"
    );

    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final RegistrationApplicationMapper registrationApplicationMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminFileServiceImpl(FileStorageService fileStorageService,
                                StorageProperties storageProperties,
                                RegistrationApplicationMapper registrationApplicationMapper,
                                AdminOperationLogMapper adminOperationLogMapper) {
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
        this.registrationApplicationMapper = registrationApplicationMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public MediaFileDownloadResponse downloadStudentCard(Long fileId, AdminPrincipal principal) {
        MediaFile mediaFile = fileStorageService.getRequiredFile(fileId);
        if (mediaFile.getFileKey() == null || !mediaFile.getFileKey().startsWith("student-cards/")) {
            throw new BusinessException(40310, HttpStatus.FORBIDDEN, "Only student card files can be downloaded through this endpoint");
        }
        Path root = Path.of(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path target = root.resolve(mediaFile.getFileKey().replace('/', File.separatorChar)).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(40311, HttpStatus.FORBIDDEN, "Invalid file path");
        }
        byte[] content;
        try {
            content = Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new BusinessException(40410, HttpStatus.NOT_FOUND, "Uploaded file not found");
        }

        // 学生证属于敏感证件影像,每次查看写入操作日志(查看审计)
        RegistrationApplication application = registrationApplicationMapper.selectByStudentCardFileId(fileId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "view_student_card");
        payload.put("fileId", fileId);
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(principal.getAdminId())
                .targetType("registration")
                .targetId(application != null ? application.getApplicationId() : fileId)
                .operationType("other")
                .operationDetail(JsonUtil.toJson(payload))
                .ipAddress(null)
                .build());

        return new MediaFileDownloadResponse(
                mediaFile.getFileId(),
                mediaFile.getOriginalName(),
                sanitizeMimeType(mediaFile.getMimeType()),
                content
        );
    }

    /**
     * mimeType 由上传时客户端自报,下载响应必须以白名单为准,避免伪造 Content-Type 造成浏览器内解析风险。
     */
    private String sanitizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "application/octet-stream";
        }
        return ALLOWED_MIME_TYPES.getOrDefault(mimeType.toLowerCase(Locale.ROOT), "application/octet-stream");
    }
}
