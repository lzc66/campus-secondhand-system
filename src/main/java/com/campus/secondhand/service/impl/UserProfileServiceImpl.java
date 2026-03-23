package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.ChangePasswordRequest;
import com.campus.secondhand.dto.user.UpdateUserProfileRequest;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.service.UserProfileService;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final MediaFileMapper mediaFileMapper;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(UserMapper userMapper,
                                  MediaFileMapper mediaFileMapper,
                                  FileStorageService fileStorageService,
                                  PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResponse getProfile(UserPrincipal principal) {
        return toProfile(getRequiredUser(principal.getUserId()));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UserPrincipal principal, UpdateUserProfileRequest request) {
        User user = getRequiredUser(principal.getUserId());
        User existingEmailOwner = userMapper.selectByEmail(request.email().trim().toLowerCase());
        if (existingEmailOwner != null && !existingEmailOwner.getUserId().equals(user.getUserId())) {
            throw new BusinessException(40930, HttpStatus.CONFLICT, "Email has already been registered");
        }
        user.setRealName(request.realName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(trimToNull(request.phone()));
        user.setQqNo(trimToNull(request.qqNo()));
        user.setWechatNo(trimToNull(request.wechatNo()));
        user.setCollegeName(trimToNull(request.collegeName()));
        user.setMajorName(trimToNull(request.majorName()));
        user.setClassName(trimToNull(request.className()));
        user.setDormitoryAddress(trimToNull(request.dormitoryAddress()));
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Override
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = getRequiredUser(principal.getUserId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(40031, HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public MediaFileResponse uploadAvatar(UserPrincipal principal, MultipartFile file) {
        MediaFileResponse response = fileStorageService.storeUserAvatar(principal.getUserId(), file);
        User user = getRequiredUser(principal.getUserId());
        user.setAvatarFileId(response.fileId());
        userMapper.updateById(user);
        return response;
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private UserProfileResponse toProfile(User user) {
        MediaFile avatar = user.getAvatarFileId() == null ? null : mediaFileMapper.selectById(user.getAvatarFileId());
        return new UserProfileResponse(
                user.getUserId(),
                user.getStudentNo(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getQqNo(),
                user.getWechatNo(),
                user.getAvatarFileId(),
                avatar == null ? null : avatar.getFileUrl(),
                user.getCollegeName(),
                user.getMajorName(),
                user.getClassName(),
                user.getDormitoryAddress(),
                user.getAccountStatus().getValue()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}