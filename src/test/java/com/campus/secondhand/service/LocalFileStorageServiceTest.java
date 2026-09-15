package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.config.StorageProperties;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.service.impl.LocalFileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    /** 1x1 像素的最小合法 PNG,用于通过文件头(magic bytes)校验 */
    private static final byte[] VALID_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Mock
    private MediaFileMapper mediaFileMapper;

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreImageFile() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        storageProperties.setPublicBaseUrl("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(mediaFileMapper, storageProperties);
        MockMultipartFile file = new MockMultipartFile("file", "card.png", "image/png", VALID_PNG);
        doAnswer(invocation -> {
            MediaFile mediaFile = invocation.getArgument(0);
            mediaFile.setFileId(88L);
            return 1;
        }).when(mediaFileMapper).insert(any(MediaFile.class));

        var response = service.storeStudentCard(file);

        assertEquals(88L, response.fileId());
        verify(mediaFileMapper).insert(any(MediaFile.class));
    }

    @Test
    void shouldRejectNonImageFile() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        storageProperties.setPublicBaseUrl("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(mediaFileMapper, storageProperties);
        MockMultipartFile file = new MockMultipartFile("file", "card.txt", "text/plain", "content".getBytes());

        assertThrows(BusinessException.class, () -> service.storeStudentCard(file));
    }

    @Test
    void shouldRejectSvgFile() {
        // 内容类型不在白名单内:SVG 曾可被上传构成存储型 XSS,必须拒绝
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        storageProperties.setPublicBaseUrl("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(mediaFileMapper, storageProperties);
        MockMultipartFile file = new MockMultipartFile("file", "x.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes());

        assertThrows(BusinessException.class, () -> service.storeStudentCard(file));
    }

    @Test
    void shouldRejectFileWhoseContentDoesNotMatchDeclaredType() {
        // 声明 image/png 但内容不是图片:文件头校验必须拒绝
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        storageProperties.setPublicBaseUrl("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(mediaFileMapper, storageProperties);
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());

        assertThrows(BusinessException.class, () -> service.storeStudentCard(file));
    }

    @Test
    void shouldRemoveOrphanFileWhenDbInsertFails() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        storageProperties.setPublicBaseUrl("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(mediaFileMapper, storageProperties);
        MockMultipartFile file = new MockMultipartFile("file", "card.png", "image/png", VALID_PNG);
        doAnswer(invocation -> {
            throw new RuntimeException("db down");
        }).when(mediaFileMapper).insert(any(MediaFile.class));

        assertThrows(RuntimeException.class, () -> service.storeStudentCard(file));
        // 磁盘上不应留下孤儿文件
        try (var stream = java.nio.file.Files.walk(tempDir)) {
            long fileCount = stream.filter(java.nio.file.Files::isRegularFile).count();
            assertEquals(0, fileCount);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
