package run.mone.mapweibo.mcpweibo;

import org.junit.Test;
import run.mone.mcp.weibo.function.WeiboFunction;


public class McpWeiboApplicationTests {

    @Test
    public void contextLoads() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.login();
    }

}
