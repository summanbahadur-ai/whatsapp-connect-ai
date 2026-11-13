package com.whatsappconnect.auth;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.whatsappconnect.database.*;

/** Credentials use wc_auth; membership and profile always use transaction-local wc_app/RLS. */
public final class SessionService {
    private final JdbcTemplate auth;
    private final AccountTransactions accounts;
    private final PasswordEncoder encoder;
    private final String dummyHash;
    public record View(UserResponse user,Map<String,Object> profile,Map<String,Object> account) {}
    public static final class InvalidCredentials extends RuntimeException {}
    public SessionService(DataSource source,AccountTransactions accounts,PasswordEncoder encoder) {
        this.auth=new JdbcTemplate(source);this.accounts=accounts;this.encoder=encoder;
        this.dummyHash=encoder.encode(UUID.randomUUID().toString());
    }
    public View authenticate(SigninRequest input) {
        var rows=auth.query("SELECT u.id,c.password_hash FROM identity.users u JOIN identity.password_credentials c ON c.user_id=u.id WHERE lower(u.email)=?",
            (r,n)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),input.email());
        String hash=rows.isEmpty()?dummyHash:rows.getFirst().getValue();
        boolean valid=input.password().getBytes(StandardCharsets.UTF_8).length<=72
            && encoder.matches(input.password(),hash);
        if(!valid||rows.size()!=1)throw new InvalidCredentials();
        try{return view(rows.getFirst().getKey());}catch(AccountAccessDeniedException e){throw new InvalidCredentials();}
    }
    public View view(UUID id) {
        var users=auth.query("SELECT id,name,email,email_verified,image,created_at,updated_at FROM identity.users WHERE id=?",
            (r,n)->new UserResponse(r.getObject("id",UUID.class),r.getString("name"),r.getString("email"),
                r.getBoolean("email_verified"),r.getString("image"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),id);
        if(users.size()!=1)throw new InvalidCredentials();
        return accounts.withAccount(id,AccountRole.VIEWER,scope->{
            var profile=scope.jdbc().queryForMap("SELECT id,full_name,email,avatar_url,role,account_id,account_role FROM public.profiles WHERE user_id=?",id);
            profile.put("beta_features",List.of());
            var account=scope.jdbc().queryForMap("SELECT id,name,default_currency FROM public.accounts WHERE id=?",scope.context().accountId());
            return new View(users.getFirst(),profile,account);
        });
    }
}
