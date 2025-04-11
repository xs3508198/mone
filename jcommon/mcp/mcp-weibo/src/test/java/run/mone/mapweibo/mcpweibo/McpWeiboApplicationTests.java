package run.mone.mapweibo.mcpweibo;

import lombok.SneakyThrows;
import org.junit.Test;
import run.mone.mcp.weibo.function.WeiboFunction;


public class McpWeiboApplicationTests {

    @Test
    public void test1() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.loginAuthorization();
    }

    @Test
    @SneakyThrows
    public void test2() {
        String token = "123456";
        WeiboFunction weiboFunction = new WeiboFunction();
        String res = weiboFunction.loginGetAccessToken(token);
        System.out.println(res);
    }

    @Test
    @SneakyThrows
    public void test3(){
        WeiboFunction weiboFunction = new WeiboFunction();
        String s = weiboFunction.homeTimeline("1");
        System.out.println(s);
    }

}
