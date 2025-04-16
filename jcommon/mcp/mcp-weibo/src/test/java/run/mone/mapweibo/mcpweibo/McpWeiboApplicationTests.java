package run.mone.mapweibo.mcpweibo;

import lombok.SneakyThrows;
import org.junit.Test;
import run.mone.mcp.weibo.function.WeiboFunction;
import run.mone.mcp.weibo.model.WeiboContent;
import run.mone.mcp.weibo.model.WeiboContentDisplay;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        List<WeiboContentDisplay> weiboContent = weiboFunction.homeTimeline("1");
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
        List<WeiboContentDisplay> res = weiboFunction.searchWeibo("曾凡博");
        System.out.println(gson.toJson(res));
    }

    @Test
    @SneakyThrows
    public void test7(){
        System.out.println(ExtractUserId("//weibo.com/6616523296?refer_flag=1001030103_"));
    }


    private String ExtractUserId(String userText) {
        // 正则表达式匹配 weibo.com/后面的连续数字
        Pattern pattern = Pattern.compile("weibo\\.com/(\\d+)");
        Matcher matcher = pattern.matcher(userText);
        if (matcher.find()) {
            String userId = matcher.group(1);
            return userId;
        } else {
            return "";
        }
    }


}
