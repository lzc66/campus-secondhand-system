package com.campus.secondhand.service;

import com.campus.secondhand.dto.publicapi.RegistrationApplicationSubmitRequest;
import com.campus.secondhand.vo.publicapi.RegistrationApplicationSubmitResponse;

public interface PublicRegistrationService {

    RegistrationApplicationSubmitResponse submit(RegistrationApplicationSubmitRequest request);
}