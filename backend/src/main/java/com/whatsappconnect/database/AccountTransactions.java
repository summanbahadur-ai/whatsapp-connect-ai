package com.whatsappconnect.database;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.*;
public final class AccountTransactions {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public record AccountScope(AccountContext context, JdbcTemplate jdbc) {}
    public AccountTransactions(DataSource source) {
        jdbc=new JdbcTemplate(source);
        transactions=new TransactionTemplate(new JdbcTransactionManager(source));
    }
    public <T> T withAccount(UUID user, AccountRole minimum, Function<AccountScope,T> action) {
        Objects.requireNonNull(user); Objects.requireNonNull(minimum); Objects.requireNonNull(action);
        if(TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Nested account scopes are not permitted");
        return transactions.execute(status -> {
            jdbc.queryForObject("SELECT set_config('app.user_id', ?, true)",String.class,user.toString());
            var memberships=jdbc.query("SELECT account_id, account_role FROM public.profiles WHERE user_id=?",
                (rs,n)->new AccountContext(user,rs.getObject("account_id",UUID.class),
                    AccountRole.valueOf(rs.getString("account_role").toUpperCase(Locale.ROOT))),user);
            if(memberships.size()!=1 || memberships.getFirst().role().ordinal()<minimum.ordinal())
                throw new AccountAccessDeniedException();
            return action.apply(new AccountScope(memberships.getFirst(),jdbc));
        });
    }
}
