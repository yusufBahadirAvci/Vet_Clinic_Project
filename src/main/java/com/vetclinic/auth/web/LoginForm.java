package com.vetclinic.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = LoginController.INVALID_CONTACT_MESSAGE)
    @Size(max = 254, message = LoginController.INVALID_CONTACT_MESSAGE)
    private String contact;
}
