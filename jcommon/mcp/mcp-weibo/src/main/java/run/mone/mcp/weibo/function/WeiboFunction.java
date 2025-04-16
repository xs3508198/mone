package run.mone.mcp.weibo.function;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.weibo.http.HttpClientUtil;
import run.mone.mcp.weibo.model.WeiboContent;
import run.mone.mcp.weibo.model.WeiboContentDisplay;
import run.mone.mcp.weibo.model.WeiboUserDisplay;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static run.mone.hive.common.JsonUtils.gson;

@Slf4j
public class WeiboFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> stringObjectMap) {
        return null;
    }


    private static String ACCESS_KEY = "" ;
    private static final String ALAPI_TOKEN  = "";



    private static final String CLIENT_ID = "1500473794";
    private static final String CLIENT_SECRET = "";
    private static final String AUTHORIZATION_URL = "https://api.weibo.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.weibo.com/oauth2/access_token";
    public static final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";

    private static final String HOME_TIMELINE = "https://api.weibo.com/2/statuses/home_timeline.json";
    private static final String USER_TIMELINE = "https://api.weibo.com/2/statuses/user_timeline.json";

    private static final String WEIBO_HOT = "https://v3.alapi.cn/api/new/wbtop";

    private static final String SEARCH = "https://s.weibo.com/weibo?q=";

    private static final String CHROME_DRIVER_PATH = "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver";

    static {
        // 在静态块中设置，确保只设置一次
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
    }


    public String loginAuthorization() {
        String finalUrl = AUTHORIZATION_URL + "?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + REDIRECT_URI + "&scope=all";
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(finalUrl));
                    return "授权成功后请输入code：";
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
        return "授权出现问题，请重试！";
    }


    public String loginGetAccessToken(String code) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);
        String res = HttpClientUtil.postForm(TOKEN_URL, params);
        if (res == null || res.isEmpty()) {
            return "登录失败，请核验参数重新登录！";
        }
        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);
        String accessKey = jsonObject.get("access_token").getAsString();
        if (accessKey != null && accessKey.isEmpty()) {
            ACCESS_KEY = accessKey;
            return "登录成功！";
        }
        return "登录失败，请核验参数重新登录！";
    }

    public List<WeiboContentDisplay> homeTimeline(String page) throws IOException {
        if (page == null || page.isEmpty()) {
            page = "1";
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        params.put("page", page);
        String res = HttpClientUtil.get(HOME_TIMELINE, params);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        List<WeiboContentDisplay> contentDisplay = weiboContent.toContentDisplay();
        return contentDisplay;
    }


    public WeiboContent userTimeline() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        String res = HttpClientUtil.get(USER_TIMELINE, params);
        System.out.println(res);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        return weiboContent;
    }

    public String weiboHot() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("token", ALAPI_TOKEN);
        String res = HttpClientUtil.postForm(WEIBO_HOT, params);
        return res;
    }


    public static WebDriver createWebDriver() {
        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);

        try {
            // 1. 访问微博主页以初始化会话
            driver.get("https://s.weibo.com/");
            Thread.sleep(1000); // 等待页面加载

            // 2. 清除现有的 Cookie
            driver.manage().deleteAllCookies();

            // 3. 添加登录后的 Cookie
            driver.manage().addCookie(new Cookie("SUB", ""));
            driver.manage().addCookie(new Cookie("SUBP", ""));
            driver.manage().addCookie(new Cookie("SSOLoginState", ""));
            driver.manage().addCookie(new Cookie("ALF", ""));
            driver.manage().addCookie(new Cookie("SCF", ""));

            // 4. 重新访问微博主页以应用 Cookie
            driver.get("https://s.weibo.com/");
            Thread.sleep(1000); // 等待页面加载

            return driver;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            driver.quit();
            throw new RuntimeException("初始化 WebDriver 时发生中断", e);
        } catch (Exception e) {
            driver.quit();
            throw new RuntimeException("初始化 WebDriver 失败", e);
        }
    }

    public List<WeiboContentDisplay> searchWeibo(String keyword){

        WebDriver driver = createWebDriver();

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://s.weibo.com/weibo?q=" + encodedKeyword;
            driver.get(searchUrl);
            Document doc = Jsoup.parse(Objects.requireNonNull(driver.getPageSource()));
            Elements elements = doc.select("div.card-wrap");

            List<WeiboContentDisplay> res = new ArrayList<>();
            for (Element post : elements) {
                try {
                    WeiboContentDisplay display = new WeiboContentDisplay();
                    String mid = post.attr("mid");
                    display.setWeiboId(mid);
                    Element userElement = post.select("a.name").first();
                    if (userElement != null) {
                        String userUrl = userElement.attr("href");
                        String userId = extractUserId(userUrl);
                        String username = userElement.text();
                        display.setUserId(userId);
                        display.setUsername(username);
                    }

                    Element timeElement = post.select("div.from a").first();
                    String time = timeElement != null ? timeElement.text().trim() : "未知时间";
                    display.setTime(time);

                    Elements contentElements = post.select("p.txt");
                    String content = "";
                    if (contentElements.size() > 1) {
                        for (Element contentElement : contentElements) {
                            if (contentElement.attr("node-type").equals("feed_list_content_full")){
                                //地点包含在内容里，单独提取
                                content = extractCleanContent(contentElement, display);
                            }
                        }
                    } else if (contentElements.size() == 1) {
                        Element firstContentElement = contentElements.first();
                        content = extractCleanContent(firstContentElement, display);
                    }
                    display.setContent(content);

                    Elements actionItems = post.select("div.card-act li");
                    for (Element li : actionItems) {
                        String actionType = li.select("a").attr("action-type");
                        if (actionType.contains("feed_list_forward")){
                            //转发
                            String repostCnt = li.text().trim();
                            if (repostCnt.equals("转发")) {
                                repostCnt = "0";
                            }
                            display.setRepost(repostCnt);
                        }
                        if (actionType.contains("feed_list_comment")){
                            //评论
                            String commentCnt = li.text().trim();
                            if (commentCnt.equals("评论")) {
                                commentCnt = "0";
                            }
                            display.setComment(commentCnt);
                        }
                        if (actionType.contains("feed_list_like")){
                            //点赞
                            Element likeElement = li.select("span.woo-like-count").first();
                            String likeCnt = (likeElement != null) ? likeElement.text().trim() : "";
                            if (likeCnt.equals("赞")) {
                                likeCnt = "0";
                            }
                            display.setLike(likeCnt);
                        }
                    }
                    res.add(display);
                } catch (Exception e) {
                    log.error("解析单条微博失败: {}", e.getMessage());
                }
            }

            if (res.size() > 0) {
                log.info("搜索结果（JSON 格式）：");
                return res;
            } else {
                return new ArrayList<>();
            }
        } finally {
            driver.quit();
        }
    }


    public List<WeiboUserDisplay> searchUsers(String userKeyword){
        WebDriver driver = createWebDriver();
        try {
            List<WeiboUserDisplay> res = new ArrayList<>();
            String encodedKeyword = URLEncoder.encode(userKeyword, StandardCharsets.UTF_8);
            String searchUrl = "https://s.weibo.com/weibo?q=" + encodedKeyword + "&Refer=weibo_user";
            driver.get(searchUrl);
            Document doc = Jsoup.parse(Objects.requireNonNull(driver.getPageSource()));
            Element element = doc.select("div.card-wrap").first();
            if (element == null) {
                return new ArrayList<>();
            }
            Elements userElements = element.select("div.card card-user-b s-brt1 card-user-b-padding");
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
                Element intro = info.select("p").first();
                if (intro != null) {
                    String introduction = intro.text().trim();
                    display.setIntroduction(introduction);
                }

                //TODO 

            res.add(display);

            }

            return res;
        } finally {
            driver.quit();
        }
    }


    private String extractUserId(String userText) {
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

    private String extractCleanContent(Element element, WeiboContentDisplay display) {
        String html = element.html().replaceAll("(?i)<br[^>]*>", "\n");
        Document doc = Jsoup.parse(html);
        for (Element a : doc.select("a")) {
            if (!"_blank".equalsIgnoreCase(a.attr("target"))){
                a.remove();
            }
            String wbicon = a.select("i.wbicon").text().trim();
            if ("_blank".equalsIgnoreCase(a.attr("target")) && wbicon.equals("2")){
                display.setPlace(a.text().replaceFirst("2", ""));
                a.remove();
            }
        }
        doc.select("i.wbicon, svg, use").remove();
        return doc.body().wholeText().trim();
    }




}
