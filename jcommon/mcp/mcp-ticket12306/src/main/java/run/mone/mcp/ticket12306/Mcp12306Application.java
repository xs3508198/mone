package run.mone.mcp.ticket12306;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("run.mone.mcp.ticket12306")
public class Mcp12306Application {
    public static void main(String[] args) {
        SpringApplication.run(Mcp12306Application.class, args);
    }

}
