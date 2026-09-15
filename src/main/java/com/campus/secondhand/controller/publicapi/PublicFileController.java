package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.common.util.SimpleRateLimiter;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.vo.publicapi.StudentCardUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/files")
public class PublicFileController {

    private final FileStorageService fileStorageService;

    public PublicFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/student-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentCardUploadResponse> uploadStudentCard(@RequestParam("file") MultipartFile file,
                                                                    HttpServletRequest httpServletRequest) {
        // 匿名上传接口按 IP 限频,防止被脚本批量灌文件
        if (!SimpleRateLimiter.tryAcquire("student-card-upload:" + httpServletRequest.getRemoteAddr(), 10, 60)) {
            throw new BusinessException(42900, HttpStatus.TOO_MANY_REQUESTS, "Too many uploads, please try again later");
        }
        return ApiResponse.success(fileStorageService.storeStudentCard(file));
    }
}