package com.campus.secondhand.service;

import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.MediaFileDownloadResponse;

public interface AdminFileService {

    /**
     * 鉴权下载学生证影像。学生证目录不挂载到公开静态资源,只能经此接口(管理员角色)下载,
     * 且每次下载都会写入操作日志(查看审计)。
     */
    MediaFileDownloadResponse downloadStudentCard(Long fileId, AdminPrincipal principal);
}
