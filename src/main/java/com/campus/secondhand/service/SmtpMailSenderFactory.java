package com.campus.secondhand.service;

import org.springframework.mail.javamail.JavaMailSender;

public interface SmtpMailSenderFactory {

    JavaMailSender createSender(SmtpRuntimeSettings settings);
}