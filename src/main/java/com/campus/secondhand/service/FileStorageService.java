package com.campus.secondhand.service;

import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.publicapi.StudentCardUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StudentCardUploadResponse storeStudentCard(MultipartFile file);

    MediaFileResponse storeUserAvatar(Long userId, MultipartFile file);

    MediaFileResponse storeItemImage(Long userId, MultipartFile file);

    MediaFile getRequiredFile(Long fileId);
}