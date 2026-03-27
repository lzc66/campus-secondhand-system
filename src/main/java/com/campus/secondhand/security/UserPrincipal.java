package com.campus.secondhand.security;

import com.campus.secondhand.enums.UserAccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String studentNo;
    private final String realName;
    private final String email;
    private final UserAccountStatus accountStatus;

    public UserPrincipal(Long userId, String studentNo, String realName, String email, UserAccountStatus accountStatus) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.realName = realName;
        this.email = email;
        this.accountStatus = accountStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public UserAccountStatus getAccountStatus() {
        return accountStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return studentNo;
    }
}