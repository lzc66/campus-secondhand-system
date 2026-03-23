package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.config.StorageProperties;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.vo.publicapi.StudentCardUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final MediaFileMapper mediaFileMapper;
    private final StorageProperties storageProperties;

    public LocalFileStorageService(MediaFileMapper mediaFileMapper, StorageProperties storageProperties) {
        this.mediaFileMapper = mediaFileMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    public StudentCardUploadResponse storeStudentCard(MultipartFile file) {
        validateImage(file);
        String extension = resolveExtension(file);
        LocalDate today = LocalDate.now();
        String fileKey = String.format(
                "student-cards/%d/%02d/%s.%s",
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
        Path target = Path.of(storageProperties.getRootDir()).resolve(fileKey.replace('/', java.io.File.separatorChar));
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(50010, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        MediaFile mediaFile = MediaFile.builder()
                .storageProvider("local")
                .bucketName("local")
                .fileKey(fileKey)
                .originalName(file.getOriginalFilename())
                .fileUrl(buildFileUrl(fileKey))
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .fileExt(extension)
                .fileCategory("image")
                .uploaderRole("guest")
                .checksumSha256(calculateSha256(target))
                .build();
        mediaFileMapper.insert(mediaFile);
        return new StudentCardUploadResponse(
                mediaFile.getFileId(),
                mediaFile.getFileUrl(),
                mediaFile.getOriginalName(),
                mediaFile.getFileSize()
        );
    }

    @Override
    public MediaFile getRequiredFile(Long fileId) {
        MediaFile mediaFile = mediaFileMapper.selectById(fileId);
        if (mediaFile == null) {
            throw new BusinessException(40410, HttpStatus.NOT_FOUND, "Uploaded file not found");
        }
        return mediaFile;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40010, HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40011, HttpStatus.BAD_REQUEST, "File size exceeds 10MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(40012, HttpStatus.BAD_REQUEST, "Only image files are supported");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains("/")) {
            return contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        }
        return "bin";
    }

    private String buildFileUrl(String fileKey) {
        String baseUrl = storageProperties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + fileKey;
    }

    private String calculateSha256(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            throw new BusinessException(50011, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate file checksum");
        }
    }
}