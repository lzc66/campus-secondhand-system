package com.campus.secondhand.controller.admin;

import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminFileService;
import com.campus.secondhand.vo.admin.MediaFileDownloadResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/admin/files")
public class AdminFileController {

    private final AdminFileService adminFileService;

    public AdminFileController(AdminFileService adminFileService) {
        this.adminFileService = adminFileService;
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadStudentCard(@PathVariable Long fileId,
                                                      @AuthenticationPrincipal AdminPrincipal principal) {
        MediaFileDownloadResponse file = adminFileService.downloadStudentCard(fileId, principal);
        String filename = file.originalName() == null ? "student-card" : file.originalName();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }
}
