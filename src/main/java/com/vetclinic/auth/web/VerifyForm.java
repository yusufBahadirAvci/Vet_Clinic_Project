package com.vetclinic.auth.web;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyForm {

    @Pattern(regexp = "\\d{6}", message = LoginController.INVALID_CODE_MESSAGE)
    private String code;
}
