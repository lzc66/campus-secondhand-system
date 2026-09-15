package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.config.StorageProperties;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.publicapi.StudentCardUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * 允许上传的图片类型白名单(Content-Type → 落盘扩展名)。
     * 扩展名一律由服务端按此映射生成,不再取原始文件名后缀,杜绝上传 html/svg/jsp 等文件构成存储型 XSS。
     */
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;

    private final MediaFileMapper mediaFileMapper;
    private final StorageProperties storageProperties;

    public LocalFileStorageService(MediaFileMapper mediaFileMapper, StorageProperties storageProperties) {
        this.mediaFileMapper = mediaFileMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    public StudentCardUploadResponse storeStudentCard(MultipartFile file) {
        MediaFile mediaFile = storeImage(file, "student-cards", "guest", null);
        return new StudentCardUploadResponse(
                mediaFile.getFileId(),
                mediaFile.getFileUrl(),
                mediaFile.getOriginalName(),
                mediaFile.getFileSize()
        );
    }

    @Override
    public MediaFileResponse storeUserAvatar(Long userId, MultipartFile file) {
        return toResponse(storeImage(file, "avatars", "user", userId));
    }

    @Override
    public MediaFileResponse storeItemImage(Long userId, MultipartFile file) {
        return toResponse(storeImage(file, "item-images", "user", userId));
    }

    @Override
    public MediaFile getRequiredFile(Long fileId) {
        MediaFile mediaFile = mediaFileMapper.selectById(fileId);
        if (mediaFile == null) {
            throw new BusinessException(40410, HttpStatus.NOT_FOUND, "Uploaded file not found");
        }
        return mediaFile;
    }

    private MediaFile storeImage(MultipartFile file, String folder, String uploaderRole, Long uploaderRefId) {
        String extension = validateImage(file);
        LocalDate today = LocalDate.now();
        String fileKey = String.format(
                "%s/%d/%02d/%s.%s",
                folder,
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

        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.length() > MAX_ORIGINAL_NAME_LENGTH) {
            originalName = originalName.substring(0, MAX_ORIGINAL_NAME_LENGTH);
        }
        MediaFile mediaFile = MediaFile.builder()
                .storageProvider("local")
                .bucketName("local")
                .fileKey(fileKey)
                .originalName(originalName)
                .fileUrl(buildFileUrl(fileKey))
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .fileExt(extension)
                .fileCategory("image")
                .uploaderRole(uploaderRole)
                .uploaderRefId(uploaderRefId)
                .checksumSha256(calculateSha256(target))
                .build();
        try {
            mediaFileMapper.insert(mediaFile);
        } catch (Exception ex) {
            // 文件系统与数据库双写:插库失败必须删除已落盘文件,避免留下永久孤儿文件
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupEx) {
                // 清理失败时保留原始异常继续抛出
            }
            throw ex;
        }
        return mediaFile;
    }

    private MediaFileResponse toResponse(MediaFile mediaFile) {
        return new MediaFileResponse(
                mediaFile.getFileId(),
                mediaFile.getFileUrl(),
                mediaFile.getOriginalName(),
                mediaFile.getFileSize(),
                mediaFile.getMimeType()
        );
    }

    /**
     * 校验上传文件并返回落盘扩展名:Content-Type 必须在白名单内,且文件头(magic bytes)必须与声明类型一致。
     * 客户端自报的 Content-Type 与原始文件名后缀均可伪造,因此两者都不能作为唯一依据。
     */
    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40010, HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40011, HttpStatus.BAD_REQUEST, "File size exceeds 10MB");
        }
        String contentType = file.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String extension = ALLOWED_IMAGE_TYPES.get(normalizedContentType);
        if (extension == null) {
            throw new BusinessException(40012, HttpStatus.BAD_REQUEST, "Only jpg, png, gif and webp images are supported");
        }
        try (InputStream inputStream = file.getInputStream()) {
            if ("webp".equals(extension)) {
                if (!isWebpHeader(inputStream.readNBytes(12))) {
                    throw new BusinessException(40012, HttpStatus.BAD_REQUEST, "File content does not match its declared image type");
                }
            } else if (ImageIO.read(inputStream) == null) {
                throw new BusinessException(40012, HttpStatus.BAD_REQUEST, "File content does not match its declared image type");
            }
        } catch (IOException ex) {
            throw new BusinessException(40012, HttpStatus.BAD_REQUEST, "File content does not match its declared image type");
        }
        return extension;
    }

    private boolean isWebpHeader(byte[] header) {
        return header != null && header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
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