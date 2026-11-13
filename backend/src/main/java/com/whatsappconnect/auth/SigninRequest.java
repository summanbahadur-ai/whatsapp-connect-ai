package com.whatsappconnect.auth;
import jakarta.validation.constraints.*;
import java.util.Locale;
public record SigninRequest(@NotBlank @Email @Size(max=254) String email,
                            @NotNull @Size(min=1,max=72) String password) {
    public SigninRequest { if(email!=null)email=email.strip().toLowerCase(Locale.ROOT); }
    @Override public String toString(){return "SigninRequest[REDACTED]";}
}
