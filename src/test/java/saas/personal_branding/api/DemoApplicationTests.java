package saas.personal_branding.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.redis.repositories.enabled=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "app.mail.provider=RESEND",
        "app.mail.resend.api-key=dummy",
        "app.mail.resend.domain=example.com",
        "app.mail.resend.from=no-reply@example.com"
})
class DemoApplicationTests {
    @Test
    void contextLoads() {
    }
}
