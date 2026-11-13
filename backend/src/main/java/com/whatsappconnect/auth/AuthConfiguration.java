package com.whatsappconnect.auth;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;

@Configuration(proxyBeanMethods=false)
@Profile("database")
public class AuthConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt",Map.of("bcrypt",new BCryptPasswordEncoder(12)));
    }
    @Bean SignupRepository signupRepository(@Qualifier("authDataSource") DataSource source) {
        return new SignupRepository(source);
    }
    @Bean SignupService signupService(@Qualifier("authDataSource") DataSource source,
                                     SignupRepository repository,PasswordEncoder encoder) {
        return new SignupService(source,repository,encoder);
    }
}
