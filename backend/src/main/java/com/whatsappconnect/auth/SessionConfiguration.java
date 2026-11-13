package com.whatsappconnect.auth;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.whatsappconnect.database.AccountTransactions;

@Configuration(proxyBeanMethods=false)
@Profile("database")
public class SessionConfiguration {
    @Bean SessionService sessionService(@Qualifier("authDataSource") DataSource source,
            AccountTransactions accounts,PasswordEncoder encoder){return new SessionService(source,accounts,encoder);}
    @Bean JdbcIndexedSessionRepository sessionRepository(@Qualifier("authDataSource") DataSource source) {
        var transactions=new TransactionTemplate(new JdbcTransactionManager(source));
        transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var repository=new JdbcIndexedSessionRepository(new JdbcTemplate(source),transactions);
        repository.setTableName("identity.spring_session");
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));
        repository.setCreateSessionAttributeQuery("INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) VALUES (?, ?, ?) ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME) DO NOTHING");
        return repository;
    }
    @Bean FilterRegistrationBean<?> sessionFilter(
            JdbcIndexedSessionRepository repository,Environment environment) {
        var cookie=new DefaultCookieSerializer();cookie.setCookieName("WC_SESSION");cookie.setCookiePath("/");
        cookie.setCookieMaxAge(604800);cookie.setUseHttpOnlyCookie(true);cookie.setSameSite("Lax");
        if(environment.getProperty("wc.auth.secure-cookies",Boolean.class,false))cookie.setUseSecureCookie(true);
        var resolver=new CookieHttpSessionIdResolver();resolver.setCookieSerializer(cookie);
        SessionRepositoryFilter<?> filter=new SessionRepositoryFilter<>(repository);filter.setHttpSessionIdResolver(resolver);
        var registration=new FilterRegistrationBean<>(filter);registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);
        return registration;
    }
}
