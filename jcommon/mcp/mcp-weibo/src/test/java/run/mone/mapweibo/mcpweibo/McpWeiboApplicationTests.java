package run.mone.mapweibo.mcpweibo;

import lombok.SneakyThrows;
import org.junit.Test;
import run.mone.mcp.weibo.function.WeiboFunction;
import run.mone.mcp.weibo.model.WeiboContent;

import static run.mone.hive.common.JsonUtils.gson;


public class McpWeiboApplicationTests {

    @Test
    public void test1() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.loginAuthorization();
    }

    @Test
    @SneakyThrows
    public void test2() {
        String token = "f5f0f8d3819a60dadbc94bfd909a7f2c";
        WeiboFunction weiboFunction = new WeiboFunction();
        String res = weiboFunction.loginGetAccessToken(token);
        System.out.println(res);
    }

    @Test
    @SneakyThrows
    public void test3(){
        WeiboFunction weiboFunction = new WeiboFunction();
        WeiboContent weiboContent = weiboFunction.homeTimeline("1");
        System.out.println(gson.toJson(weiboContent));
    }

    @Test
    @SneakyThrows
    public void test4(){
        WeiboFunction weiboFunction = new WeiboFunction();
        String id = "5154278696290069";
        WeiboContent res = weiboFunction.userTimeline();
        System.out.println(gson.toJson(res));
    }

    @Test
    @SneakyThrows
    public void test5(){
        WeiboFunction weiboFunction = new WeiboFunction();
        String res = weiboFunction.weiboHot();
        System.out.println(res);
    }
    @Test
    @SneakyThrows
    public void test6(){
        WeiboFunction weiboFunction = new WeiboFunction();
        String res = weiboFunction.searchWeibo("关税");
        System.out.println(res);
    }

}
