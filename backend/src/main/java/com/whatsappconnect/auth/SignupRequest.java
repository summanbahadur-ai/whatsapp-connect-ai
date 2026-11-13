package com.whatsappconnect.auth;

import java.util.Locale;
import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Size(max=100) String name,
        @NotBlank @Email @Size(max=254) String email,
        @NotNull @Size(min=8,max=72) String password) {
    public SignupRequest {
        if(name!=null)name=name.strip();
        if(email!=null)email=email.strip().toLowerCase(Locale.ROOT);
    }
    @Override public String toString(){return "SignupRequest[redacted]";}
}
