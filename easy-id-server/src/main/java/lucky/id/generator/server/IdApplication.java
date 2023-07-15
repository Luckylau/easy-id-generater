package lucky.id.generator.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Author luckylau
 * @Date 2023/5/21
 */
@SpringBootApplication
@EnableTransactionManagement
public class IdApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class);
    }
}
