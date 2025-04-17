package run.mone.mapweibo.mcpweibo;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import run.mone.mcp.weibo.function.WeiboFunction;
import run.mone.mcp.weibo.model.WeiboContent;
import run.mone.mcp.weibo.model.WeiboContentDisplay;
import run.mone.mcp.weibo.model.WeiboUserDisplay;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    @Test
    @SneakyThrows
    public void test8(){
        WeiboFunction weiboFunction = new WeiboFunction();
        List<WeiboUserDisplay> users = weiboFunction.searchUsers("曾凡博", 2);
        System.out.println(gson.toJson(users));
    }

    @Test
    @SneakyThrows
    public void testGetUsers(){
        String k = "曾凡博";
        Map<String, String > cookie = new HashMap();
        cookie.put("SUB", "");
        cookie.put("SUBP", "");
        cookie.put("ALF", "");
        cookie.put("SCF", "");
        cookie.put("WBPSESS", "");
        Document doc = Jsoup.connect("https://s.weibo.com/user?q=" + URLEncoder.encode(k, "UTF-8"))
                .cookies(cookie)
                .get();
        List<WeiboUserDisplay> res = new ArrayList<>();
        Element element = doc.select("div.card-wrap").first();
        if (element == null) {
            System.out.println("空的！");
            return;
        }
        Elements userElements = element.select("div.card.card-user-b.s-brt1.card-user-b-padding");
        for (Element userElement : userElements) {
            WeiboUserDisplay display = new WeiboUserDisplay();
            Element info = userElement.select("div.info").first();
            Element nameElement = info.select("a.name").first();
            String nameHref = nameElement.attr("href").trim();
            String regex = "//weibo\\.com/u/(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(nameHref);
            if (matcher.find()) {
                String userId = matcher.group(1);
                display.setUserId(userId);
            }
            String username = nameElement.text().trim();
            display.setUsername(username);
            Elements pElements = info.select("p");
            if (pElements.size() > 1) {
                String introduction = pElements.first().text().trim();
                display.setIntroduction(introduction);
            }
            String fansCount = pElements.select("span.s-nobr").first().text().trim().replace("粉丝：", "");
            display.setFansCount(fansCount);
            res.add(display);
        }
        System.out.println(gson.toJson(res));

    }



}
