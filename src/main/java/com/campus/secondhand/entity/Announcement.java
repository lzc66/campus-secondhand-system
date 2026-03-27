package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("announcements")
public class Announcement {

    @TableId(value = "announcement_id", type = IdType.AUTO)
    private Long announcementId;
    private Long publisherAdminId;
    private String title;
    private String content;
    private Integer isPinned;
    private AnnouncementPublishStatus publishStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
