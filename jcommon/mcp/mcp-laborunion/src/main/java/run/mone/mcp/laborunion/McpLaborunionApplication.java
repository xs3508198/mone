package run.mone.mcp.laborunion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@ComponentScan("run.mone.mcp.laborunion")
@EnableScheduling
public class McpLaborunionApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpLaborunionApplication.class, args);
	}

}
