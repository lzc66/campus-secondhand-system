package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.vo.publicapi.StudentCardUploadResponse;
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
    public ApiResponse<StudentCardUploadResponse> uploadStudentCard(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileStorageService.storeStudentCard(file));
    }
}