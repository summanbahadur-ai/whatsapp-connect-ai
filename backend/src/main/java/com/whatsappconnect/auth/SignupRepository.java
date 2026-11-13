package com.whatsappconnect.auth;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Always used within the auth-role transaction owned by SignupService. */
public final class SignupRepository {
    private final JdbcTemplate jdbc;
    public SignupRepository(DataSource source){jdbc=new JdbcTemplate(source);}
    public UserResponse insertIdentity(SignupRequest input) {
        return jdbc.queryForObject("""
            INSERT INTO identity.users(id,name,email) VALUES(?,?,?)
            RETURNING id,name,email,email_verified,image,created_at,updated_at
            """,(row,n)->new UserResponse(row.getObject("id",UUID.class),
                row.getString("name"),row.getString("email"),row.getBoolean("email_verified"),
                row.getString("image"),row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant()),
            UUID.randomUUID(),input.name(),input.email());
    }
    public void insertCredential(UUID userId,String hash) {
        jdbc.update("INSERT INTO identity.password_credentials(user_id,password_hash) VALUES(?,?)",userId,hash);
    }
}
