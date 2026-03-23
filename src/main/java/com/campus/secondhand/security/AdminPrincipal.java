package com.campus.secondhand.security;

import com.campus.secondhand.enums.AdminRoleCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class AdminPrincipal implements UserDetails {

    private final Long adminId;
    private final String adminNo;
    private final String adminName;
    private final String email;
    private final AdminRoleCode roleCode;

    public AdminPrincipal(Long adminId, String adminNo, String adminName, String email, AdminRoleCode roleCode) {
        this.adminId = adminId;
        this.adminNo = adminNo;
        this.adminName = adminName;
        this.email = email;
        this.roleCode = roleCode;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getAdminNo() {
        return adminNo;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getEmail() {
        return email;
    }

    public AdminRoleCode getRoleCode() {
        return roleCode;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode.getValue().toUpperCase(Locale.ROOT)));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return adminNo;
    }
}
