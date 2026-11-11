package com.whatsappconnect.database;
import java.net.URI;
import javax.sql.DataSource;
import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@Profile("database")
public class DatabaseConfiguration {
    public static String validateUrl(String url, boolean allowTest) {
        try {
            if(!url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI uri=URI.create(url.substring(5));
            String name=uri.getPath().substring(1);
            if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
                throw new IllegalArgumentException();
            if(!name.equals("whatsapp_connect_ai") && !(allowTest && name.matches("whatsapp_connect_ai_test_[a-f0-9]{32}")))
                throw new IllegalArgumentException();
            return url;
        } catch(RuntimeException error) {
            throw new IllegalArgumentException("Use an explicit PostgreSQL URL for the separate whatsapp_connect_ai database without embedded options or credentials");
        }
    }
    private HikariDataSource pool(Environment env,String purpose,String role) {
        String url=validateUrl(env.getRequiredProperty("wc.database.url"),env.matchesProfiles("test"));
        if(!role.equals(env.getRequiredProperty("wc.database."+purpose+".username")))
            throw new IllegalArgumentException("Unexpected database role for "+purpose);
        HikariConfig config=new HikariConfig();
        config.setJdbcUrl(url);config.setUsername(role);
        config.setPassword(env.getRequiredProperty("wc.database."+purpose+".password"));
        config.setPoolName("wc-"+purpose);config.setMinimumIdle(0);
        config.setMaximumPoolSize(env.getProperty("wc.database."+purpose+".pool-size",Integer.class,2));
        config.setConnectionTimeout(5000);
        config.setConnectionInitSql("DO $$ BEGIN IF current_user <> '"+role+"' THEN RAISE EXCEPTION 'Unexpected database role'; END IF; END $$");
        return new HikariDataSource(config);
    }
    @Bean(destroyMethod="close")
    DataSource migrationDataSource(Environment env){return pool(env,"migration","wc_owner");}
    @Bean(initMethod="migrate")
    Flyway databaseMigrations(@Qualifier("migrationDataSource") DataSource source) {
        return Flyway.configure().dataSource(source).defaultSchema("public").locations("classpath:db/migration")
            .cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true).load();
    }
    @Bean(destroyMethod="close") @DependsOn("databaseMigrations")
    DataSource appDataSource(Environment env){return pool(env,"app","wc_app");}
    @Bean(destroyMethod="close") @DependsOn("databaseMigrations")
    DataSource authDataSource(Environment env){return pool(env,"auth","wc_auth");}
    @Bean(destroyMethod="close") @DependsOn("databaseMigrations")
    DataSource workerDataSource(Environment env){return pool(env,"worker","wc_worker");}
    @Bean
    AccountTransactions accountTransactions(@Qualifier("appDataSource") DataSource source){return new AccountTransactions(source);}
}
