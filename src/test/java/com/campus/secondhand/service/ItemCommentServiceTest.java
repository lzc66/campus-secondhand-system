package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.CreateItemCommentRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemComment;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemCommentStatus;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.mapper.ItemCommentMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.ItemCommentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemCommentServiceTest {

    @Mock
    private ItemCommentMapper itemCommentMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;

    @InjectMocks
    private ItemCommentServiceImpl itemCommentService;

    @Test
    void shouldCreateComment() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().itemId(1L).status(ItemStatus.ON_SALE).commentCount(0).build());
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).realName("Alice").build());
        doAnswer(invocation -> {
            ItemComment comment = invocation.getArgument(0);
            comment.setCommentId(9L);
            return 1;
        }).when(itemCommentMapper).insert(any(ItemComment.class));

        var response = itemCommentService.createComment(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", com.campus.secondhand.enums.UserAccountStatus.ACTIVE),
                1L,
                new CreateItemCommentRequest("looks good")
        );

        assertEquals(9L, response.commentId());
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        assertEquals(1, itemCaptor.getValue().getCommentCount());
    }

    @Test
    void shouldReplyOnlyWhenCurrentUserIsSeller() {
        when(itemCommentMapper.selectById(3L)).thenReturn(ItemComment.builder()
                .commentId(3L)
                .itemId(1L)
                .commenterUserId(22L)
                .status(ItemCommentStatus.VISIBLE)
                .content("hello")
                .build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().itemId(1L).sellerUserId(99L).status(ItemStatus.ON_SALE).commentCount(1).build());

        assertThrows(BusinessException.class, () -> itemCommentService.replyComment(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", com.campus.secondhand.enums.UserAccountStatus.ACTIVE),
                3L,
                new CreateItemCommentRequest("reply")
        ));
    }

    @Test
    void shouldListPublicComments() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().itemId(1L).status(ItemStatus.ON_SALE).build());
        Page<ItemComment> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(ItemComment.builder()
                .commentId(3L)
                .itemId(1L)
                .commenterUserId(22L)
                .status(ItemCommentStatus.VISIBLE)
                .content("hello")
                .build()));
        when(itemCommentMapper.selectPage(any(), any())).thenReturn(pageResult);
        when(itemCommentMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(List.of(22L))).thenReturn(List.of(User.builder().userId(22L).realName("Bob").build()));

        var response = itemCommentService.listPublicComments(1L, 1, 10);

        assertEquals(1, response.total());
        assertEquals("hello", response.records().get(0).content());
    }
}