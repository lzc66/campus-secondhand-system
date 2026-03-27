package com.campus.secondhand.service.impl;

import com.campus.secondhand.service.SmtpMailSenderFactory;
import com.campus.secondhand.service.SmtpRuntimeSettings;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class SmtpMailSenderFactoryImpl implements SmtpMailSenderFactory {

    @Override
    public JavaMailSender createSender(SmtpRuntimeSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", Boolean.toString(settings.authEnabled()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(settings.starttlsEnabled()));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(settings.sslEnabled()));
        properties.put("mail.mime.charset", StandardCharsets.UTF_8.name());
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}