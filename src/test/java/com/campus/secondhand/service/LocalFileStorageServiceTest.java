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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

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
        MockMultipartFile file = new MockMultipartFile("file", "card.png", "image/png", "content".getBytes());
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
}