package com.whatsappconnect.database;

import java.net.URI;
import java.net.http.*;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import com.whatsappconnect.WhatsAppConnectApplication;
import static org.assertj.core.api.Assertions.assertThat;

class DatabaseApplicationIT {
    @Test void completeApplicationStartsWithDatabaseProfileAndKeepsApisProtected() throws Exception {
        try(var fixture=new PostgresFixture()) {
            var settings=new HashMap<String,Object>();
            settings.put("wc.database.url",fixture.url);
            settings.put("server.port",0);
            for(String purpose:new String[]{"migration","auth","app","worker"}) {
                settings.put("wc.database."+purpose+".username",purpose.equals("migration")?"wc_owner":"wc_"+purpose);
                settings.put("wc.database."+purpose+".password",fixture.password);
            }
            try(var context=new SpringApplicationBuilder(WhatsAppConnectApplication.class)
                    .profiles("database","test").initializers(ctx -> ctx.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolatedFixture",settings))).run()) {
                int port=((WebServerApplicationContext)context).getWebServer().getPort();
                var client=HttpClient.newHttpClient();
                var health=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/health")).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertThat(health.statusCode()).isEqualTo(200);
                assertThat(health.body()).contains("UP").doesNotContain("jdbc:","password","wc_owner");
                var protectedResponse=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/account")).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertThat(protectedResponse.statusCode()).isEqualTo(401);
            }
        }
    }
}
