package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.publicapi.RegistrationApplicationSubmitRequest;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.enums.Gender;
import com.campus.secondhand.enums.RegistrationStatus;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.service.PublicRegistrationService;
import com.campus.secondhand.vo.publicapi.RegistrationApplicationSubmitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class PublicRegistrationServiceImpl implements PublicRegistrationService {

    private static final DateTimeFormatter APPLICATION_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RegistrationApplicationMapper registrationApplicationMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public PublicRegistrationServiceImpl(RegistrationApplicationMapper registrationApplicationMapper,
                                         UserMapper userMapper,
                                         FileStorageService fileStorageService,
                                         PasswordEncoder passwordEncoder) {
        this.registrationApplicationMapper = registrationApplicationMapper;
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RegistrationApplicationSubmitResponse submit(RegistrationApplicationSubmitRequest request) {
        if (registrationApplicationMapper.selectPendingByStudentNo(request.studentNo()) != null) {
            throw new BusinessException(40910, HttpStatus.CONFLICT, "There is already a pending application for this student number");
        }
        if (userMapper.selectByStudentNo(request.studentNo()) != null) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "Student number has already been registered");
        }
        if (userMapper.selectByEmail(request.email()) != null) {
            throw new BusinessException(40912, HttpStatus.CONFLICT, "Email has already been registered");
        }

        MediaFile studentCard = fileStorageService.getRequiredFile(request.studentCardFileId());
        if (!"image".equalsIgnoreCase(studentCard.getFileCategory())) {
            throw new BusinessException(40013, HttpStatus.BAD_REQUEST, "studentCardFileId must reference an image file");
        }

        RegistrationApplication application = RegistrationApplication.builder()
                .applicationNo(generateApplicationNo())
                .studentNo(request.studentNo().trim())
                .realName(request.realName().trim())
                .gender(parseGender(request.gender()))
                .email(request.email().trim().toLowerCase(Locale.ROOT))
                .phone(trimToNull(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .collegeName(request.collegeName().trim())
                .majorName(trimToNull(request.majorName()))
                .className(trimToNull(request.className()))
                .studentCardFileId(request.studentCardFileId())
                .status(RegistrationStatus.PENDING)
                .build();
        registrationApplicationMapper.insert(application);

        return new RegistrationApplicationSubmitResponse(
                application.getApplicationId(),
                application.getApplicationNo(),
                application.getStatus().getValue()
        );
    }

    private String generateApplicationNo() {
        return "RA" + LocalDateTime.now().format(APPLICATION_NO_FORMATTER) + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private Gender parseGender(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            case "unknown" -> Gender.UNKNOWN;
            default -> throw new BusinessException(40014, HttpStatus.BAD_REQUEST, "gender must be male, female, or unknown");
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}