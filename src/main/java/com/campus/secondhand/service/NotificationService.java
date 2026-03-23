package com.campus.secondhand.service;

import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;

public interface NotificationService {

    void sendRegistrationApproved(RegistrationApplication application, User user, Long adminId);

    void sendRegistrationRejected(RegistrationApplication application, Long adminId);

    void sendAnnouncementPublished(Announcement announcement, Long adminId);
}
