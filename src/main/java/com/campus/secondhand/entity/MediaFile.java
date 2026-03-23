package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("media_files")
public class MediaFile {

    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;
    private String storageProvider;
    private String bucketName;
    private String fileKey;
    private String originalName;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private String fileExt;
    private String fileCategory;
    private String uploaderRole;
    private Long uploaderRefId;
    private String checksumSha256;
    private LocalDateTime createdAt;
}