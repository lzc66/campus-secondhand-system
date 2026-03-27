package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.WantedPostStatus;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.service.impl.PublicWantedPostServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicWantedPostServiceTest {

    @Mock
    private WantedPostMapper wantedPostMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;

    @InjectMocks
    private PublicWantedPostServiceImpl publicWantedPostService;

    @Test
    void shouldListOpenWantedPosts() {
        Page<WantedPost> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(WantedPost.builder()
                .wantedPostId(3L)
                .categoryId(2L)
                .title("Need Java Book")
                .brand("Tsinghua")
                .expectedPriceMin(new BigDecimal("20"))
                .expectedPriceMax(new BigDecimal("50"))
                .status(WantedPostStatus.OPEN)
                .build()));
        when(wantedPostMapper.selectPage(any(), any())).thenReturn(pageResult);
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("Books").build());

        var response = publicWantedPostService.listWantedPosts(2L, "Java", null, null, null, "latest", 1, 10);

        assertEquals(1, response.total());
        assertEquals("Need Java Book", response.records().get(0).title());
    }

    @Test
    void shouldIncreaseViewCountWhenGettingDetail() {
        when(wantedPostMapper.selectById(3L)).thenReturn(WantedPost.builder()
                .wantedPostId(3L)
                .requesterUserId(11L)
                .categoryId(2L)
                .title("Need Java Book")
                .description("Looking for one")
                .status(WantedPostStatus.OPEN)
                .viewCount(2)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("Books").build());
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).realName("Alice").build());

        var response = publicWantedPostService.getWantedPostDetail(3L);

        assertEquals(3, response.viewCount());
        ArgumentCaptor<WantedPost> captor = ArgumentCaptor.forClass(WantedPost.class);
        verify(wantedPostMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getViewCount());
    }
}
