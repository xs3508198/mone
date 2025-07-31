package run.mone.mcp.laborunion;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import run.mone.mcp.laborunion.function.LaborUnionFunction;

@SpringBootTest
class McpLaborunionApplicationTests {

	@Resource
	private LaborUnionFunction function;

	@Test
	void test() {
		function.setAuth("eyJhbGciOiJIUzI1NiJ9.eyJpc0dvb2RzQWRtaW4iOmZhbHNlLCJsb2dpbk5hbWUiOiJ4dWVzaGFuIiwiaXNNZW1iZXIiOnRydWUsImlzQWRtaW4iOmZhbHNlLCJ1c2VyTmFtZSI6IuiWm-WxsSIsImV4cCI6MTc1NDU0OTM1MywiZW1haWwiOiJ4dWVzaGFuQHhpYW9taS5jb20iLCJtZW1iZXJJZCI6IjI0MTU4In0.lJ4HChFYFdm3VqYkEajnUGwdKCO5VswREp0BuRatEFQ");
		String res = function.checkTodaySign();
		System.out.println(res);
	}

}
