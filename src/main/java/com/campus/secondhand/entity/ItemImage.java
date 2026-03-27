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
@TableName("item_images")
public class ItemImage {

    @TableId(value = "item_image_id", type = IdType.AUTO)
    private Long itemImageId;
    private Long itemId;
    private Long fileId;
    private Integer sortOrder;
    private Integer isCover;
    private LocalDateTime createdAt;
}