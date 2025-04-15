package run.mone.mcp.weibo.function;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    public WeiboContent homeTimeline(String page) throws IOException {
        if (page == null || page.isEmpty()) {
            page = "1";
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        params.put("page", page);
        String res = HttpClientUtil.get(HOME_TIMELINE, params);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        return weiboContent;
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

    public String searchWeibo(String keyword) throws IOException {

        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://s.weibo.com/weibo?q=" + encodedKeyword;
            driver.get(searchUrl);

            System.out.println(driver.getPageSource());

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.card-wrap")));

            List<WebElement> elements = driver.findElements(By.cssSelector("div.card-wrap"));
            JSONArray results = new JSONArray();

            for (WebElement post : elements) {
                try {
                    String username = post.findElement(By.cssSelector("a.name")).getText();
                    String content = post.findElement(By.cssSelector("p.txt")).getText();
                    String time = "未知";
                    try {
                        WebElement timeElement = post.findElement(By.cssSelector("p.from a"));
                        if (timeElement != null) {
                            time = timeElement.getText();
                        }
                    } catch (Exception ex) {
                        log.warn("未找到发布时间，message：{}", ex.getMessage());
                        // 如果没有找到发布时间则保持 "未知"
                    }
                    if (!username.isEmpty() && !content.isEmpty()) {
                        JSONObject postJson = new JSONObject();
                        postJson.put("username", username);
                        postJson.put("content", content);
                        postJson.put("time", time);
                        results.put(postJson);
                    }
                } catch (JSONException e) {
                    log.error("解析单条微博失败: {}", e.getMessage());
                }
            }

            if (results.length() > 0) {
                log.info("搜索结果（JSON 格式）：");
                return results.toString(2);
            } else {
                return "未找到有效搜索结果";
            }
        } finally {
            driver.quit();
        }
    }

    public static WebDriver createWebDriver(){
        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        }
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            // 1. 访问微博主页以初始化会话
            driver.get("https://weibo.com/");
            Thread.sleep(3000); // 等待页面加载

            // 2. 清除现有的 Cookie
            driver.manage().deleteAllCookies();

            // 3. 添加登录后的 Cookie
            driver.manage().addCookie(new Cookie("SUB", ""));
            driver.manage().addCookie(new Cookie("SUBP", ""));
            driver.manage().addCookie(new Cookie("SSOLoginState", ""));
            driver.manage().addCookie(new Cookie("ALF", ""));
            driver.manage().addCookie(new Cookie("SCF", ""));

            // 4. 重新访问微博主页以应用 Cookie
            driver.get("https://weibo.com/");
            Thread.sleep(3000); // 等待页面加载

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

}
