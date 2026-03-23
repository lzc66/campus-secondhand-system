package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.CreateItemCommentRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemComment;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemCommentStatus;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.mapper.ItemCommentMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.ItemCommentService;
import com.campus.secondhand.vo.publicapi.CommentAuthorResponse;
import com.campus.secondhand.vo.publicapi.PublicItemCommentPageResponse;
import com.campus.secondhand.vo.publicapi.PublicItemCommentReplyResponse;
import com.campus.secondhand.vo.publicapi.PublicItemCommentResponse;
import com.campus.secondhand.vo.user.UserReceivedCommentPageResponse;
import com.campus.secondhand.vo.user.UserReceivedCommentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemCommentServiceImpl implements ItemCommentService {

    private final ItemCommentMapper itemCommentMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final MediaFileMapper mediaFileMapper;

    public ItemCommentServiceImpl(ItemCommentMapper itemCommentMapper,
                                  ItemMapper itemMapper,
                                  UserMapper userMapper,
                                  MediaFileMapper mediaFileMapper) {
        this.itemCommentMapper = itemCommentMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public PublicItemCommentPageResponse listPublicComments(Long itemId, long page, long size) {
        ensurePublicItem(itemId);
        Page<ItemComment> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<ItemComment> wrapper = new LambdaQueryWrapper<ItemComment>()
                .eq(ItemComment::getItemId, itemId)
                .isNull(ItemComment::getParentCommentId)
                .eq(ItemComment::getStatus, ItemCommentStatus.VISIBLE)
                .orderByAsc(ItemComment::getCreatedAt);
        Page<ItemComment> result = itemCommentMapper.selectPage(queryPage, wrapper);
        List<PublicItemCommentResponse> records = buildThreadResponses(result.getRecords());
        return new PublicItemCommentPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public PublicItemCommentResponse createComment(UserPrincipal principal, Long itemId, CreateItemCommentRequest request) {
        ensurePublicItem(itemId);
        User user = getRequiredUser(principal.getUserId());
        ItemComment comment = ItemComment.builder()
                .itemId(itemId)
                .commenterUserId(user.getUserId())
                .content(request.content().trim())
                .status(ItemCommentStatus.VISIBLE)
                .build();
        itemCommentMapper.insert(comment);
        increaseItemCommentCount(itemId);
        return buildThreadResponse(comment, List.of());
    }

    @Override
    @Transactional
    public PublicItemCommentResponse replyComment(UserPrincipal principal, Long commentId, CreateItemCommentRequest request) {
        ItemComment parent = getVisibleComment(commentId);
        Item item = getRequiredItem(parent.getItemId());
        if (!Objects.equals(item.getSellerUserId(), principal.getUserId())) {
            throw new BusinessException(40350, HttpStatus.FORBIDDEN, "Only the item seller can reply to comments");
        }
        User seller = getRequiredUser(principal.getUserId());
        User replyToUser = getRequiredUser(parent.getCommenterUserId());
        ItemComment reply = ItemComment.builder()
                .itemId(parent.getItemId())
                .commenterUserId(seller.getUserId())
                .parentCommentId(parent.getCommentId())
                .replyToUserId(replyToUser.getUserId())
                .content(request.content().trim())
                .status(ItemCommentStatus.VISIBLE)
                .build();
        itemCommentMapper.insert(reply);
        increaseItemCommentCount(parent.getItemId());
        return buildThreadResponse(parent, List.of(reply));
    }

    @Override
    public UserReceivedCommentPageResponse listReceivedComments(UserPrincipal principal, Long itemId, long page, long size) {
        Page<ItemComment> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        List<Item> ownedItems = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getSellerUserId, principal.getUserId())
                .ne(Item::getStatus, ItemStatus.DELETED));
        if (ownedItems.isEmpty()) {
            return new UserReceivedCommentPageResponse(queryPage.getCurrent(), queryPage.getSize(), 0, List.of());
        }
        List<Long> itemIds = ownedItems.stream().map(Item::getItemId).toList();
        Map<Long, Item> itemMap = ownedItems.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));
        LambdaQueryWrapper<ItemComment> wrapper = new LambdaQueryWrapper<ItemComment>()
                .in(ItemComment::getItemId, itemIds)
                .isNull(ItemComment::getParentCommentId)
                .eq(ItemComment::getStatus, ItemCommentStatus.VISIBLE)
                .eq(itemId != null, ItemComment::getItemId, itemId)
                .orderByDesc(ItemComment::getCreatedAt);
        Page<ItemComment> result = itemCommentMapper.selectPage(queryPage, wrapper);
        List<Long> rootCommentIds = result.getRecords().stream().map(ItemComment::getCommentId).toList();
        Set<Long> repliedParentIds = rootCommentIds.isEmpty() ? Set.of() : itemCommentMapper.selectList(new LambdaQueryWrapper<ItemComment>()
                .in(ItemComment::getParentCommentId, rootCommentIds)
                .eq(ItemComment::getCommenterUserId, principal.getUserId())
                .eq(ItemComment::getStatus, ItemCommentStatus.VISIBLE))
                .stream().map(ItemComment::getParentCommentId).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUserMap(result.getRecords().stream().map(ItemComment::getCommenterUserId).toList());
        List<UserReceivedCommentResponse> records = result.getRecords().stream()
                .map(comment -> new UserReceivedCommentResponse(
                        comment.getCommentId(),
                        comment.getItemId(),
                        itemMap.get(comment.getItemId()) == null ? null : itemMap.get(comment.getItemId()).getTitle(),
                        comment.getContent(),
                        comment.getCreatedAt(),
                        toAuthor(userMap.get(comment.getCommenterUserId())),
                        repliedParentIds.contains(comment.getCommentId())
                ))
                .toList();
        return new UserReceivedCommentPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    private List<PublicItemCommentResponse> buildThreadResponses(List<ItemComment> rootComments) {
        if (rootComments.isEmpty()) {
            return List.of();
        }
        List<Long> rootIds = rootComments.stream().map(ItemComment::getCommentId).toList();
        List<ItemComment> replies = itemCommentMapper.selectList(new LambdaQueryWrapper<ItemComment>()
                .in(ItemComment::getParentCommentId, rootIds)
                .eq(ItemComment::getStatus, ItemCommentStatus.VISIBLE)
                .orderByAsc(ItemComment::getCreatedAt));
        Map<Long, List<ItemComment>> replyMap = replies.stream().collect(Collectors.groupingBy(ItemComment::getParentCommentId, LinkedHashMap::new, Collectors.toList()));
        return rootComments.stream()
                .map(root -> buildThreadResponse(root, replyMap.getOrDefault(root.getCommentId(), List.of())))
                .toList();
    }

    private PublicItemCommentResponse buildThreadResponse(ItemComment root, List<ItemComment> replies) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(root.getCommenterUserId());
        replies.forEach(reply -> {
            userIds.add(reply.getCommenterUserId());
            if (reply.getReplyToUserId() != null) {
                userIds.add(reply.getReplyToUserId());
            }
        });
        Map<Long, User> userMap = loadUserMap(userIds);
        List<PublicItemCommentReplyResponse> replyResponses = replies.stream()
                .map(reply -> new PublicItemCommentReplyResponse(
                        reply.getCommentId(),
                        reply.getItemId(),
                        reply.getContent(),
                        reply.getCreatedAt(),
                        reply.getReplyToUserId(),
                        userMap.get(reply.getReplyToUserId()) == null ? null : userMap.get(reply.getReplyToUserId()).getRealName(),
                        toAuthor(userMap.get(reply.getCommenterUserId()))
                ))
                .toList();
        return new PublicItemCommentResponse(
                root.getCommentId(),
                root.getItemId(),
                root.getContent(),
                root.getCreatedAt(),
                toAuthor(userMap.get(root.getCommenterUserId())),
                replyResponses
        );
    }

    private void increaseItemCommentCount(Long itemId) {
        Item item = getRequiredItem(itemId);
        item.setCommentCount(item.getCommentCount() == null ? 1 : item.getCommentCount() + 1);
        itemMapper.updateById(item);
    }

    private void ensurePublicItem(Long itemId) {
        Item item = getRequiredItem(itemId);
        if (item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
    }

    private Item getRequiredItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private ItemComment getVisibleComment(Long commentId) {
        ItemComment comment = itemCommentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != ItemCommentStatus.VISIBLE) {
            throw new BusinessException(40451, HttpStatus.NOT_FOUND, "Comment not found");
        }
        return comment;
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds.stream().filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private CommentAuthorResponse toAuthor(User user) {
        if (user == null) {
            return null;
        }
        MediaFile avatar = user.getAvatarFileId() == null ? null : mediaFileMapper.selectById(user.getAvatarFileId());
        return new CommentAuthorResponse(user.getUserId(), user.getRealName(), avatar == null ? null : avatar.getFileUrl());
    }
}