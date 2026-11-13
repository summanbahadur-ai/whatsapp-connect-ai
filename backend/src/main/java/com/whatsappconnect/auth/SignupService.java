package com.whatsappconnect.auth;

import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

public final class SignupService {
    public static final class DuplicateEmail extends RuntimeException {}
    public static final class InvalidPassword extends RuntimeException {}
    private final SignupRepository repository;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transactions;
    public SignupService(DataSource source,SignupRepository repository,PasswordEncoder encoder) {
        this.repository=repository;this.encoder=encoder;
        transactions=new TransactionTemplate(new JdbcTransactionManager(source));
    }
    public UserResponse signup(SignupRequest input) {
        // BCrypt's byte limit is stricter than the character limit for Unicode.
        if(input.password().length()<8||input.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw new InvalidPassword();
        // Expensive hashing happens before opening a database transaction.
        String hash=encoder.encode(input.password());
        return transactions.execute(status->{
            final UserResponse user;
            try { user=repository.insertIdentity(input); }
            catch(DuplicateKeyException duplicate){throw new DuplicateEmail();}
            // Existing identity INSERT trigger provisions exactly one account/profile.
            repository.insertCredential(user.id(),hash);
            return user;
        });
    }
}
