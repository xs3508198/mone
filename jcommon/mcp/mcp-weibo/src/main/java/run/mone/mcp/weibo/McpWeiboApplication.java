package run.mone.mcp.weibo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("run.mone.mcp.weibo")
public class McpWeiboApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpWeiboApplication.class, args);
    }

}
