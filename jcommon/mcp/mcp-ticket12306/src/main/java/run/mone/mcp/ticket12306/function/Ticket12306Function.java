package run.mone.mcp.ticket12306.function;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import run.mone.hive.mcp.spec.McpSchema;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static run.mone.hive.common.JsonUtils.gson;


@Slf4j
public class Ticket12306Function implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> stringObjectMap) {
        return null;
    }

    private WebDriver driver;

    private Map<String, String> cityCodeMap; // key是城市名， value是编码

    private static final String CHROME_DRIVER_PATH = System.getenv().getOrDefault("CHROME_DRIVER_PATH", "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver");
    private static final int LOADING_TIME = Integer.parseInt(System.getenv().getOrDefault("LOADING_TIME", "300"));

    private static final String STATION_CODE_JS = "https://kyfw.12306.cn/otn/resources/js/framework/station_name.js";
    private static final String LOGIN_URL = "https://kyfw.12306.cn/otn/resources/login.html";
    private static final String MY_CENTER = "https://kyfw.12306.cn/otn/view/index.html";
    private static final String HOME_PAGE = "https://www.12306.cn/index/index.html";

    static {
        // 在静态块中设置，确保只设置一次
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
    }

    @PostConstruct
    public Map<String, String> initStationCode() throws Exception{
        TrustManager[] trustAllCerts = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STATION_CODE_JS))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            String message = response.body();
            Map<String, String> map = extractDataToMap(message);
            cityCodeMap = map;
        }else {
            throw new RuntimeException("Failed to fetch station data: HTTP " + response.statusCode());
        }

        return null;
    }

    public String login(){
        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        }
        ChromeOptions options = new ChromeOptions();
        WebDriver webDriver = new ChromeDriver(options);

        webDriver.get(LOGIN_URL);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(90));
        try {
            wait.until(ExpectedConditions.urlContains(MY_CENTER));
        } catch (TimeoutException e) {
            webDriver.quit();
            return "登录失败，请重试！";
        }
        options.addArguments("--headless=new");
        WebDriver newWebDriver = new ChromeDriver(options);
        webDriver.get(HOME_PAGE);
        newWebDriver.get(HOME_PAGE);
        for (Cookie cookie : webDriver.manage().getCookies()) {
            newWebDriver.manage().addCookie(cookie);
        }
        waitLoading();
        driver = newWebDriver;
        driver.get(HOME_PAGE);
        return "登录成功！";
    }


    private Map<String, String> extractDataToMap(String originData){
        String msg = originData.substring(originData.indexOf("'") + 1, originData.lastIndexOf("'"));
        String[] cityMsg = msg.split("@");
        List<String> validBlocks = Arrays.stream(cityMsg)
                .filter(block -> !block.trim().isEmpty())
                .toList();
        Map<String, String> res = new HashMap<>();
        for (String block : validBlocks) {
            String[] split = block.split("\\|");
            res.put(split[1], split[2]);
        }
        return  res;
    }


    public void waitLoading(){
        try {
            Thread.sleep(LOADING_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException("Loading interrupted", e);
        }
    }

}
