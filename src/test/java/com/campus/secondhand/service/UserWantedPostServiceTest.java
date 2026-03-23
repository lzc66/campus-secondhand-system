package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.SaveWantedPostRequest;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.enums.WantedPostStatus;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserWantedPostServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWantedPostServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private WantedPostMapper wantedPostMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private RecommendationBehaviorService recommendationBehaviorService;

    @InjectMocks
    private UserWantedPostServiceImpl userWantedPostService;

    @Test
    void shouldCreateWantedPostUsingProfileContacts() {
        when(userMapper.selectById(11L)).thenReturn(User.builder()
                .userId(11L)
                .realName("Alice")
                .phone("13800000000")
                .qqNo("123456")
                .wechatNo("alicewx")
                .build());
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("Books").isEnabled(1).build());
        doAnswer(invocation -> {
            WantedPost wantedPost = invocation.getArgument(0);
            wantedPost.setWantedPostId(9L);
            return 1;
        }).when(wantedPostMapper).insert(any(WantedPost.class));

        var response = userWantedPostService.createWantedPost(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new SaveWantedPostRequest(
                        2L,
                        "Need Java Book",
                        "Tsinghua",
                        null,
                        "Looking for a clean copy",
                        new BigDecimal("20"),
                        new BigDecimal("50"),
                        null,
                        null,
                        null,
                        "open",
                        LocalDateTime.now().plusDays(3)
                )
        );

        assertEquals(9L, response.wantedPostId());
        assertEquals("13800000000", response.contactPhone());
        assertEquals("open", response.status());
        verify(recommendationBehaviorService).recordWantedPostPublish(11L, 9L, "Need Java Book");
    }

    @Test
    void shouldRejectInvalidExpectedPriceRange() {
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).build());

        assertThrows(BusinessException.class, () -> userWantedPostService.createWantedPost(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new SaveWantedPostRequest(
                        null,
                        "Need Java Book",
                        null,
                        null,
                        "Looking for a clean copy",
                        new BigDecimal("80"),
                        new BigDecimal("50"),
                        null,
                        null,
                        null,
                        "open",
                        LocalDateTime.now().plusDays(2)
                )
        ));
    }

    @Test
    void shouldSoftDeleteWantedPost() {
        when(wantedPostMapper.selectById(5L)).thenReturn(WantedPost.builder()
                .wantedPostId(5L)
                .requesterUserId(11L)
                .status(WantedPostStatus.OPEN)
                .build());

        userWantedPostService.deleteWantedPost(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                5L
        );

        ArgumentCaptor<WantedPost> captor = ArgumentCaptor.forClass(WantedPost.class);
        verify(wantedPostMapper).updateById(captor.capture());
        assertEquals(WantedPostStatus.CANCELLED, captor.getValue().getStatus());
    }
}
