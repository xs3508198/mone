package run.mone.mapweibo.mcpweibo;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import run.mone.mcp.weibo.function.WeiboFunction;
import run.mone.mcp.weibo.model.WeiboUserDisplay;

import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static run.mone.hive.common.JsonUtils.gson;


public class McpWeiboApplicationTests {


    @Test
    @SneakyThrows
    public void test5() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String res = weiboFunction.weiboHot();
        System.out.println(gson.toJson(res));
    }

    @Test
    @SneakyThrows
    public void test6() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String users = weiboFunction.searchWeibo("特朗普 关税", 1);
        System.out.println(gson.toJson(users));
    }

    @Test
    @SneakyThrows
    public void test7() {
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
    public void test8() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String users = weiboFunction.searchUsers("曾凡博", 2);
        System.out.println(gson.toJson(users));
    }

    @Test
    @SneakyThrows
    public void testGetUsers() {
        String k = "曾凡博";
        Map<String, String> cookie = new HashMap();
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

    @Test
    @SneakyThrows
    public void testGetCookie() {
        String path = "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver";
        if (System.getProperty("webdriver.chrome.driver") == null) {
            System.setProperty("webdriver.chrome.driver", path);
        }
        ChromeOptions opts = new ChromeOptions();

        ChromeDriver driver = new ChromeDriver(opts);

        driver.get("https://weibo.com/login");

        waitUntilLogin(driver, 180);
        driver.get("https://weibo.com");
        Set<Cookie> cookies1 = driver.manage().getCookies();
        System.out.println(gson.toJson(cookies1));
        driver.get("https://login.sina.com.cn/");
        Set<Cookie> cookies2 = driver.manage().getCookies();
        waitUntilLogin(driver, 180);
        System.out.println(gson.toJson(cookies2));

    }

    public static void waitUntilLogin(WebDriver driver, int maxWaitSeconds) {
        System.out.println("等待人工登录中（自动检测 Cookie 是否登录成功）...");
        int waited = 0;

        while (waited < maxWaitSeconds) {
            Set<Cookie> cookies = driver.manage().getCookies();
            boolean hasSub = cookies.stream().anyMatch(c -> c.getName().equals("SUB"));

            if (hasSub) {
                System.out.println("检测到已登录，继续执行程序...");
                return;
            }

            try {
                Thread.sleep(1000); // 每秒检查一次
                waited++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new RuntimeException("等待超时，未检测到登录成功");
    }


    @Test
    @SneakyThrows
    public void test9() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String weiboContentDisplays = weiboFunction.myFollowWeibo(10);
        System.out.println(gson.toJson(weiboContentDisplays));
    }

    @Test
    @SneakyThrows
    public void test10() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String weiboContentDisplays = weiboFunction.recommendWeibo(2);
        System.out.println(gson.toJson(weiboContentDisplays));
    }


    @Test
    @SneakyThrows
    public void test11() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String list = weiboFunction.weiboDetail("https://weibo.com/2803301701/PnGqfmKc7", "", 3);
        System.out.println(gson.toJson(list));
    }

    @Test
    @SneakyThrows
    public void testLogin() {

        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver");
        }
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        WebDriver webDriver = new ChromeDriver(options);

        webDriver.get("https://passport.weibo.com/sso/signin?entry=miniblog&source=miniblog&url=");
        Thread.sleep(10000);
        Set<Cookie> cookies = webDriver.manage().getCookies();
        for (Cookie cookie : cookies) {
            System.out.println(gson.toJson(cookie));
        }
    }

    @Test
    @SneakyThrows
    public void test12() {
        WeiboFunction weiboFunction = new WeiboFunction();
        weiboFunction.createWebDriver();
        String weiboContentDisplays = weiboFunction.localWeibo(0);
        System.out.println(gson.toJson(weiboContentDisplays));
    }

    @Test
    @SneakyThrows
    public void test13() {
        WeiboFunction weiboFunction = new WeiboFunction();
        String login = weiboFunction.createWebDriver();
        System.out.println(login);
        String weiboContentDisplays = weiboFunction.hereAndNowWeibo(0);
        System.out.println(gson.toJson(weiboContentDisplays));
    }

    @Test
    @SneakyThrows
    public void test14() {
        WeiboFunction weiboFunction = new WeiboFunction();
        String login = weiboFunction.createWebDriver();
        System.out.println(login);
        String weiboContentDisplay = weiboFunction.newWeibo("hello world！");
        System.out.println(gson.toJson(weiboContentDisplay));
    }

    @Test
    @SneakyThrows
    public void test15() {
        WeiboFunction weiboFunction = new WeiboFunction();
        String login = weiboFunction.createWebDriver();
        System.out.println(login);
        String weiboContentDisplays = weiboFunction.userHomepage("7992710487", 2);
        System.out.println(gson.toJson(weiboContentDisplays));
    }

}
