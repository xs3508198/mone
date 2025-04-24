package run.mone.mcp.weibo.function;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.weibo.model.WeiboContentDisplay;
import run.mone.mcp.weibo.model.WeiboUserDisplay;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static run.mone.hive.common.JsonUtils.gson;

@Data
@Slf4j
public class WeiboFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    private String name = "weiboOperation";

    private String desc = "Weibo operations mainly include logging in, Posting on Weibo, viewing the user's homepage, my followed Weibo, recommended Weibo, Weibo hot search, searching for Weibo, viewing the details of retweets, comments and likes of a certain Weibo, searching for users, local Weibo, nearby Weibo and logging out of Weibo";

    private String toolScheme = """
            {
               "type": "object",
               "properties": {
                 "operation": {
                   "type": "string",
                   "enum": ["login", "newWeibo", "userHome", "myFollowedWeibo", "recommendWeibo", "weiboHot", "searchWeibo", "weiboDetail", "searchUsers", "localWeibo", "nearbyWeibo", "logout"],
                   "description": "Weibo operation type and requirements: login/logout/weiboHot require no parameters; newWeibo requires myContent; userHome requires userId & scrollTimes; myFollowedWeibo/recommendWeibo/localWeibo/nearbyWeibo require scrollTimes; searchWeibo requires keyword & page; weiboDetail requires weiboUrl (myContent & detailType optional); searchUsers requires keyword & scrollTimes. Finally, you need to format the returned data, perform automatic line breaks, etc., to make it display more aesthetically pleasing"
                 },
                 "myContent": {
                   "type": "string",
                   "description": "Weibo text content (only used for newWeibo posting/weiboDetail commenting, ignored in other operations)."
                 },
                 "userId": {
                   "type": "string",
                   "description": "Target user's unique ID (required for userHome operation)."
                 },
                 "scrollTimes": {
                   "type": "integer",
                   "description": "Page scroll loading count (default: 5, used in userHome/myFollowedWeibo/recommendWeibo/searchUsers/localWeibo/nearbyWeibo)."
                 },
                 "keyword": {
                   "type": "string",
                   "description": "Search term (required for searchWeibo/searchUsers operations)."
                 },
                 "page": {
                   "type": "integer",
                   "description": "Search results page number (default: 1, required for searchWeibo operation)."
                 },
                 "weiboUrl": {
                   "type": "string",
                   "description": "Weibo detail page URL (required for weiboDetail operation)."
                 },
                 "detailType": {
                   "type": "integer",
                   "description": "Interaction type for weiboDetail: 1=reposts, 2=comments (default), 3=likes."
                 }
               },
               "required": ["operation", "myContent", "userId", "scrollTimes", "keyword", "page", "weiboUrl", "detailType"]
             }
            """;

    @SneakyThrows
    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> args) {
        String operation = (String) args.get("operation");
        String myContent = (String) args.get("myContent");
        String userId = (String) args.get("userId");
        Integer scrollTimes = (Integer) args.get("scrollTimes");
        String keyword = (String) args.get("keyword");
        Integer page = (Integer) args.get("page");
        String weiboUrl = (String) args.get("weiboUrl");
        Integer detailType = (Integer) args.get("detailType");

        try{
            String res = switch (operation){
                case "login" -> createWebDriver();
                case "newWeibo" -> newWeibo(myContent);
                case "userHome" -> userHomepage(userId, scrollTimes);
                case "myFollowedWeibo" -> myFollowWeibo(scrollTimes);
                case "recommendWeibo" -> recommendWeibo(scrollTimes);
                case "weiboHot" -> weiboHot();
                case "searchWeibo" -> searchWeibo(keyword, page);
                case "weiboDetail" -> weiboDetail(weiboUrl, myContent, detailType);
                case "searchUsers" -> searchUsers(keyword, page);
                case "localWeibo" -> localWeibo(scrollTimes);
                case "nearbyWeibo" -> hereAndNowWeibo(scrollTimes);
                case "logout" -> logout();
                default -> "no this operation";
            };
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(res)), false);
        } catch (IOException e) {
            log.error("Error performing Elasticsearch operation: ", e);
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("Error: " + e.getMessage())), true);
        }
    }

    private WebDriver driver;

    private static final String LOGIN_URL = "https://passport.weibo.com/sso/signin?entry=wapsso&source=wapsso&url=";
    private static final String WEIBO_HOT = "https://weibo.com/newlogin?tabtype=search&gid=&openLoginLayer=0&url=https%3A%2F%2Fweibo.com%2Fhot%2Fsearch";
    private static final String SEARCH_WEIBO = "https://s.weibo.com/weibo?q=";
    private static final String SEARCH_USER = "https://s.weibo.com/user?q=";
    private static final String MY_FOLLOW_WEIBO = "https://weibo.com/";
    private static final String RECOMMEND_WEIBO = "https://weibo.com/hot/weibo/102803";
    private static final String MOBILE_WEIBO = "https://m.weibo.cn/";
    private static final String HERE_NOW_WEIBO = "https://m.weibo.cn/p/index?containerid=2310360020_fujin_%s_%s_0_m_&needlocation=1&luicode=20000174";
    private static final String USER_HOMEPAGE = "https://weibo.com/u/%s";

    private static final String CHROME_DRIVER_PATH = System.getenv().getOrDefault("CHROME_DRIVER_PATH", "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver");
    private static final int LOADING_TIME = Integer.parseInt(System.getenv().getOrDefault("LOADING_TIME", "300"));


    static {
        // 在静态块中设置，确保只设置一次
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
    }


    public String createWebDriver() throws InterruptedException {
        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        }
        ChromeOptions options = new ChromeOptions();
        WebDriver webDriver = new ChromeDriver(options);

        webDriver.get(LOGIN_URL);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(90));
        try {
            wait.until(ExpectedConditions.urlContains(MY_FOLLOW_WEIBO));
        } catch (TimeoutException e) {
            webDriver.quit();
            return "登录失败，请重试！";
        }

        Map<String, Object> prefs = new HashMap<>();
        // 0 = 默认弹窗，1 = 允许，2 = 阻止
        prefs.put("profile.default_content_setting_values.geolocation", 1);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--headless=new");
        WebDriver newWebDriver = new ChromeDriver(options);
        // 访问 m.weibo.cn，确保生成相关 Cookies
        webDriver.get(MOBILE_WEIBO);
        newWebDriver.get(MOBILE_WEIBO);
        waitLoading();// 等待页面加载

        Set<Cookie> cookies = webDriver.manage().getCookies();
        System.out.println(gson.toJson(cookies));
        if (cookies.isEmpty()) {
            return "登录失败！请重新登录";
        }
        for (Cookie cookie : cookies) {
            waitLoading();
            newWebDriver.manage().addCookie(cookie);
        }

        // 访问 weibo.com，确保生成相关 Cookies
        webDriver.get(MY_FOLLOW_WEIBO);
        newWebDriver.get(MY_FOLLOW_WEIBO);
        String localStorage = (String) ((JavascriptExecutor) webDriver).executeScript("return JSON.stringify(localStorage);");
        String sessionStorage = (String) ((JavascriptExecutor) webDriver).executeScript("return JSON.stringify(sessionStorage);");

        Set<Cookie> cookies2 = webDriver.manage().getCookies();
        System.out.println(gson.toJson(cookies2));
        for (Cookie cookie : cookies2) {
            waitLoading();
            newWebDriver.manage().addCookie(cookie);
        }
        newWebDriver.get(MY_FOLLOW_WEIBO);
        // 恢复本地存储和会话存储（如果需要）
        ((JavascriptExecutor) newWebDriver).executeScript("localStorage.clear();");
        ((JavascriptExecutor) newWebDriver).executeScript("var data = " + localStorage + "; for(var key in data){localStorage.setItem(key, data[key]);}");
        ((JavascriptExecutor) newWebDriver).executeScript("sessionStorage.clear();");
        ((JavascriptExecutor) newWebDriver).executeScript("var data = " + sessionStorage + "; for(var key in data){sessionStorage.setItem(key, data[key]);}");
        webDriver.quit();
        driver = newWebDriver;
        waitLoading();
        return "登录成功！";
    }

    public String newWeibo(String myContent) throws Exception {
        WeiboContentDisplay display = new WeiboContentDisplay();
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(MY_FOLLOW_WEIBO);
        waitLoading();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement textarea2 = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("textarea.Form_input_2gtXx")));
        textarea2.click();
        textarea2.sendKeys(myContent);

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.Tool_btn_2Eane")));
        submitButton.click();

        waitLoading();
        String html = driver.getPageSource();
        Document doc = Jsoup.parse(html);
        Element element = doc.select("div.wbpro-scroller-item").first();
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('span.expand').forEach(function(el){el.click();});"
        );
        Element contentElement = element.select("div.detail_wbtext_4CRf9").first();
        String content = extractCleanContent(contentElement, display);
        display.setContent(content);
        Element userElement = element.select("div.head_content_wrap_27749").first();
        Element usernameElement = userElement.select("a.head_name_24eEB").first();
        String username = usernameElement.text().trim();
        String userId = usernameElement.attr("usercard");
        display.setUsername(username);
        display.setUserId(userId);
        Element timeElement = element.select("a.head-info_time_6sFQg").first();
        String time = timeElement.attr("title");
        String weiboUrl = timeElement.attr("href");
        display.setTime(time);
        display.setWeiboUrl(weiboUrl);
        Element footer = element.select("footer[aria-label]").first();
        if (footer != null) {
            String ariaLabel = footer.attr("aria-label").trim();
            String[] counts = ariaLabel.split(",");
            String repostCnt = counts.length > 0 ? counts[0] : "0";
            String commentCnt = counts.length > 1 ? counts[1] : "0";
            String likeCnt = counts.length > 2 ? counts[2] : "0";
            display.setRepost(repostCnt);
            display.setComment(commentCnt);
            display.setLike(likeCnt);
        }
        return gson.toJson(display);
    }

    public String userHomepage(String userId, int scrollTimes) throws Exception{
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        String fullUrl = String.format(USER_HOMEPAGE,userId);
        List<WeiboContentDisplay> res = processMsg(driver, fullUrl, scrollTimes);
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return gson.toJson(res);
        } else {
            log.warn("搜索结果为空！");
            return "";
        }
    }

    public String myFollowWeibo(int scrollTimes) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        List<WeiboContentDisplay> res = processMsg(driver, MY_FOLLOW_WEIBO, scrollTimes);
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return gson.toJson(res);
        } else {
            log.warn("搜索结果为空！");
            return "";
        }
    }

    private List<WeiboContentDisplay> processMsg(WebDriver webDriver, String url, int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        webDriver.get(url);
        waitLoading();

        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i < scrollTimes; i++) {
            ((JavascriptExecutor) webDriver).executeScript(
                    "document.querySelectorAll('span.expand').forEach(function(el){el.click();});"
            );
            waitLoading();
            String scrollHtml = webDriver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) webDriver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            waitLoading();
        }
        Set<String> seenWeiboUrlSet = new HashSet<>();
        for (String html : htmlList) {
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.vue-recycle-scroller__item-view");
            for (Element element : elements) {
                WeiboContentDisplay display = new WeiboContentDisplay();
                Element userElement = element.select("a.head_name_24eEB").first();
                String userId = userElement.attr("usercard").trim();
                String username = userElement.text().trim();
                display.setUserId(userId);
                display.setUsername(username);
                Element timeElement = element.select("a.head-info_time_6sFQg").first();
                String weiboUrl = timeElement.attr("href").trim();
                if (seenWeiboUrlSet.contains(weiboUrl)) {
                    continue;
                }
                seenWeiboUrlSet.add(weiboUrl);
                String time = timeElement.attr("title");
                display.setTime(time);
                display.setWeiboUrl(weiboUrl);
                Element contentElement = element.select("div.detail_wbtext_4CRf9").first();
                String content = extractCleanContent(contentElement, display);
                display.setContent(content);
                Element footer = element.selectFirst("footer[aria-label]");
                if (footer != null) {
                    String ariaLabel = footer.attr("aria-label").trim();
                    String[] counts = ariaLabel.split(",");
                    String repostCnt = counts.length > 0 ? counts[0] : "0";
                    String commentCnt = counts.length > 1 ? counts[1] : "0";
                    String likeCnt = counts.length > 2 ? counts[2] : "0";
                    display.setRepost(repostCnt);
                    display.setComment(commentCnt);
                    display.setLike(likeCnt);
                }
                res.add(display);
            }
        }
        return res;
    }

    public String recommendWeibo(int scrollTimes) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        List<WeiboContentDisplay> res = processMsg(driver, RECOMMEND_WEIBO, scrollTimes);
        ;
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return gson.toJson(res);
        } else {
            log.warn("搜索结果为空！");
            return "";
        }
    }


    public String weiboHot() throws Exception {
        Map<String, String> res = new TreeMap<>(new Comparator<String>() {

            @Override
            public int compare(String key1, String key2) {
                return Integer.compare(Integer.parseInt(key1), Integer.parseInt(key2));
            }
        });
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(WEIBO_HOT);
        waitLoading();
        int i = 0; //最多就刷10次
        while (i++ < 10) {
            String html = driver.getPageSource();
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.vue-recycle-scroller__item-view");
            for (Element element : elements) {
                Element numberElement = element.select("div.HotTopic_rankimg_2Y9y8").first();
                if (numberElement == null) {
                    continue;
                }
                String number = numberElement.text();
                String content = element.select("a.HotTopic_tit_eS4fv").first().text().trim();
                res.put(number, content);
            }
            if (res.size() >= 50) {
                break;
            }
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, window.innerHeight);");
            waitLoading();
        }
        return gson.toJson(res);
    }

    public String searchWeibo(String keyword, int page) {
        List<WeiboContentDisplay> res = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String searchUrl = SEARCH_WEIBO + encodedKeyword;
        if (page > 1) {
            searchUrl = searchUrl + "&page=" + page;
        }
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(searchUrl);
        String html = driver.getPageSource();
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("div.card-wrap");


        for (Element post : elements) {
            try {
                WeiboContentDisplay display = new WeiboContentDisplay();
                String mid = post.attr("mid");
                Element userElement = post.select("a.name").first();
                if (userElement != null) {
                    String userUrl = userElement.attr("href");
                    String userId = extractUserId(userUrl);
                    String username = userElement.text();
                    display.setUserId(userId);
                    display.setUsername(username);
                    String weiboUrl = MY_FOLLOW_WEIBO + userId + "/" + mid;
                    display.setWeiboUrl(weiboUrl);
                }

                Element timeElement = post.select("div.from a").first();
                String time = timeElement != null ? timeElement.text().trim() : "未知时间";
                display.setTime(time);

                Elements contentElements = post.select("p.txt");
                String content = "";
                if (contentElements.size() > 1) {
                    for (Element contentElement : contentElements) {
                        if (contentElement.attr("node-type").equals("feed_list_content_full")) {
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
                    if (actionType.contains("feed_list_forward")) {
                        //转发
                        String repostCnt = li.text().trim();
                        if (repostCnt.equals("转发")) {
                            repostCnt = "0";
                        }
                        display.setRepost(repostCnt);
                    }
                    if (actionType.contains("feed_list_comment")) {
                        //评论
                        String commentCnt = li.text().trim();
                        if (commentCnt.equals("评论")) {
                            commentCnt = "0";
                        }
                        display.setComment(commentCnt);
                    }
                    if (actionType.contains("feed_list_like")) {
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

        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return gson.toJson(res);
        } else {
            log.warn("搜索结果为空！");
            return "";
        }

    }

    //1：转发， 2：评论，3：点赞
    public String weiboDetail(String weiboUrl, String myContent, int detailType) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        List<String> res = new ArrayList<>();
        driver.get(weiboUrl);
        waitLoading();
        switch (detailType) {
            case 1:
                res = processRepostDetail(driver, myContent);
                break;
            case 2:
                res = processCommentDetail(driver, myContent);
                break;
            case 3:
                res = processLikeDetail(driver, myContent);
                break;
            default:
                res = processCommentDetail(driver, myContent);
                break;
        }
        return gson.toJson(res);
    }

    private List<String> processCommentDetail(WebDriver webDriver, String myContent) throws Exception {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        if (!myContent.isEmpty()) {
            WebElement textarea = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("textarea.Form_input_3JT2Q")));
            textarea.click();
            textarea.sendKeys(myContent);

            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.Composer_btn_2XFOD")));
            submitButton.click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.wbpro-scroller-item")));
            waitLoading();
        }

        List<String> res = new ArrayList<>();
        int i = 0;
        while (i++ < 30) {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.vue-recycle-scroller__item-view")));
            List<WebElement> elements = webDriver.findElements(By.cssSelector("div.vue-recycle-scroller__item-view"));
            log.info("第" + i + "次：" + elements.size());
            for (WebElement element : elements) {
                WebElement textElement = element.findElement(By.cssSelector("div.text"));
                String username = textElement.findElement(
                                By.cssSelector("a.ALink_default_2ibt1"))
                        .getText().trim();
                String content = element.findElement(
                                By.cssSelector("span"))
                        .getText().trim();
                String disPlay = "【" + username + "】: " + content;
                if (!res.contains(disPlay)) {
                    res.add(disPlay);
                }
            }
            ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, window.innerHeight * 0.5);");
            waitLoading();
            if (res.size() >= 100) {
                break;
            }
        }
        return res;
    }


    private List<String> processRepostDetail(WebDriver webDriver, String myContent) throws Exception {
        List<String> res = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        WebElement textarea = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.toolbar_wrap_np6Ug")));
        textarea.click();
        waitLoading();
        String html = webDriver.getPageSource();
        Document doc = Jsoup.parse(html);
        if (doc.select("div.toolbar_cur_JoD5A").isEmpty()) {
            return res;
        }
        if (!myContent.isEmpty()) {
            WebDriverWait wait2 = new WebDriverWait(webDriver, Duration.ofSeconds(5));
            WebElement textarea2 = wait2.until(ExpectedConditions.elementToBeClickable(By.cssSelector("textarea.Form_input_3JT2Q")));
            textarea2.click();
            textarea2.sendKeys(myContent);

            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.Composer_btn_2XFOD")));
            submitButton.click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.wbpro-scroller-item")));
            waitLoading();
        }
        int i = 0;
        while (i++ < 30) {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.vue-recycle-scroller__item-view")));

            List<WebElement> elements = webDriver.findElements(By.cssSelector("div.vue-recycle-scroller__item-view"));
            log.info("第" + i + "次：" + elements.size());
            for (WebElement element : elements) {
                WebElement textElement = element.findElement(By.cssSelector("div.text"));
                String username = textElement.findElement(
                                By.cssSelector("a.ALink_default_2ibt1"))
                        .getText().trim();
                String content = element.findElement(
                                By.cssSelector("span"))
                        .getText().trim();
                String time = element.findElement(
                        By.cssSelector("a.ALink_none_1w6rm")).getText().trim();
                String disPlay = time + "【" + username + "】: " + content;
                if (!res.contains(disPlay)) {
                    res.add(disPlay);
                }
            }
            ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, window.innerHeight * 0.5);");
            waitLoading();
            if (res.size() >= 100) {
                break;
            }
        }
        return res;

    }


    private List<String> processLikeDetail(WebDriver webDriver, String myContent) throws Exception {
        List<String> res = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        WebElement likeElement = webDriver.findElements(By.cssSelector("div.toolbar_wrap_np6Ug")).getLast();
        likeElement.click();
        waitLoading();

        if (!myContent.isEmpty()) {
            likeElement.click();
        }

        String html = webDriver.getPageSource();
        Document doc = Jsoup.parse(html);
        if (doc.select("div.toolbar_cur_JoD5A").isEmpty()) {
            return res;
        }

        int i = 0;
        while (i++ < 30) {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.vue-recycle-scroller__item-view")));

            List<WebElement> elements = webDriver.findElements(By.cssSelector("div.vue-recycle-scroller__item-view"));
            log.info("第" + i + "次：" + elements.size());
            for (WebElement element : elements) {
                WebElement textElement = element.findElement(By.cssSelector("div.text"));
                String username = textElement.findElement(
                                By.cssSelector("a.ALink_default_2ibt1"))
                        .getText().trim();
                if (!res.contains(username)) {
                    res.add(username);
                }
            }
            ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, window.innerHeight * 0.5);");
            waitLoading();
            if (res.size() >= 100) {
                break;
            }
        }
        return res;
    }


    public String searchUsers(String userKeyword, int page) {

        List<WeiboUserDisplay> res = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(userKeyword, StandardCharsets.UTF_8);
        String searchUrl = SEARCH_USER + encodedKeyword + "&Refer=weibo_user";
        if (page > 1) {
            searchUrl = searchUrl + "&page=" + page;
        }
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(searchUrl);
        String html = driver.getPageSource();
        Document doc = Jsoup.parse(html);
        Element element = doc.select("div.card-wrap").first();
        if (element == null) {
            return "";
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
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
        } else {
            log.warn("搜索结果为空！");
            return "";
        }
        return gson.toJson(res);

    }

    public String localWeibo(int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(MOBILE_WEIBO);
        List<WebElement> items = driver.findElements(By.cssSelector("li.item_li"));
        List<WebElement> cur_items = driver.findElements(By.cssSelector("li.cur"));
        items.addAll(cur_items);
        for (WebElement item : items) {
            if (item.getText().equals("同城")) {
                item.click();
                waitLoading();
                break;
            }
        }
        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i <= scrollTimes; i++) {
            String scrollHtml = driver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            waitLoading();
        }
        for (String html : htmlList) {
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.wb-item-wrap");
            for (Element element : elements) {
                WeiboContentDisplay display = new WeiboContentDisplay();
                Element userElement = element.select("header.weibo-top").first();
                if (userElement == null) {
                    continue;
                }
                String attr = userElement.select("a").first().attr("href");
                if (attr.contains("/profile/")) {
                    String userId = attr.replace("/profile/", "");
                    display.setUserId(userId);
                }
                String username = userElement.select("h3.m-text-cut").first().text().trim();
                display.setUsername(username);
                String time = userElement.select("span.time").first().text().trim();
                display.setTime(time);

                Element textelement = element.select("div.weibo-text").first();
                Elements iconElements = textelement.select("a");
                for (Element iconElement : iconElements) {
                    Element img = iconElement.select("img").first();
                    if (img != null && img.attr("src").equals("https://h5.sinaimg.cn/upload/2015/09/25/3/timeline_card_small_location_default.png")) {
                        String place = iconElement.text().trim();
                        display.setPlace(place);
                    }
                    iconElement.remove();
                }
                String content = textelement.text().trim();
                display.setContent(content);

                Elements box = element.select("div.m-box-center-a");
                Element repostElement = box.getFirst();
                String repost = repostElement.text().trim();
                repost = repost.equals("转发") ? "0" : repost;
                display.setRepost(repost);
                Element commentElement = box.get(1);
                String comment = commentElement.text().trim();
                comment = comment.equals("评论") ? "0" : comment;
                display.setComment(comment);
                Element likeElement = box.getLast();
                String like = likeElement.text().trim();
                like = like.equals("赞") ? "0" : like;
                display.setLike(like);
                if (!res.contains(display)) {
                    res.add(display);
                }
            }
        }
        return gson.toJson(res);

    }

    public String hereAndNowWeibo(int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        if (driver == null) {
            log.error("未登录，请您登录！");
            return "未登录，请您登录！";
        }
        driver.get(MOBILE_WEIBO);
        waitLoading();
        // 执行 JS，获取真实经纬度
        Map<String, Object> position = (Map<String, Object>) ((JavascriptExecutor) driver)
                .executeScript(
                        "return new Promise((resolve, reject) => {" +
                                "  navigator.geolocation.getCurrentPosition(" +
                                "    p => resolve({" +
                                "      latitude: p.coords.latitude," +
                                "      longitude: p.coords.longitude," +
                                "      accuracy: p.coords.accuracy" +
                                "    })," +
                                "    e => reject(e)," +
                                "    {" +
                                "      enableHighAccuracy: true," +
                                "      timeout: 10000," +
                                "      maximumAge: 0" +
                                "    }" +
                                "  );" +
                                "});"
                );
        double lon = (Double) position.get("longitude");
        double lat = (Double) position.get("latitude");
        double accuracy = (Double) position.get("accuracy");
        log.info("实时定位 → 经度：{}，纬度：{}，精度：{} 米", lon, lat, accuracy);

        String fullUrl = String.format(HERE_NOW_WEIBO, lon, lat);
        driver.get(fullUrl);
        waitLoading();
        WebElement localElement = driver.findElement(By.cssSelector("ul.center"));
        localElement.findElements(By.cssSelector("li")).get(0).click();
        waitLoading();
        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i <= scrollTimes; i++) {
            String scrollHtml = driver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            waitLoading();
        }
        for (String html : htmlList) {
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.card-main");
            for (Element element : elements) {
                WeiboContentDisplay display = new WeiboContentDisplay();
                Element userElement = element.select("header.m-avatar-box").first();
                if (userElement == null) {
                    continue;
                }
                String username = userElement.select("h3.m-text-cut").first().text().trim();
                display.setUsername(username);
                String time = userElement.select("span.time").first().text().trim();
                display.setTime(time);

                Element textelement = element.select("div.weibo-text").first();
                String textHtml = textelement.html().replaceAll("(?i)<br[^>]*>", "\n");
                Document textDoc = Jsoup.parse(textHtml);
                Elements iconElements = textDoc.select("a");
                for (Element iconElement : iconElements) {
                    Element img = iconElement.select("img").first();
                    if (img != null && img.attr("src").equals("https://h5.sinaimg.cn/upload/2015/09/25/3/timeline_card_small_location_default.png")) {
                        String place = iconElement.text().trim();
                        display.setPlace(place);
                    }
                    iconElement.remove();
                }
                String content = textDoc.text().trim();
                display.setContent(content);

                Elements box = element.select("div.m-box-center-a");
                Element repostElement = box.getFirst();
                String repost = repostElement.text().trim();
                repost = repost.equals("转发") ? "0" : repost;
                display.setRepost(repost);
                Element commentElement = box.get(1);
                String comment = commentElement.text().trim();
                comment = comment.equals("评论") ? "0" : comment;
                display.setComment(comment);
                Element likeElement = box.getLast();
                String like = likeElement.text().trim();
                like = like.equals("赞") ? "0" : like;
                display.setLike(like);
                if (!res.contains(display)) {
                    res.add(display);
                }
            }
        }
        return gson.toJson(res);

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
        //换行
        String html = element.html().replaceAll("(?i)<br[^>]*>", "\n");
        //
        Document doc = Jsoup.parse(html);
        for (Element a : doc.select("a")) {
            if (!"_blank".equalsIgnoreCase(a.attr("target"))) {
                a.remove();
            }
            String wbicon = a.select("i.wbicon").text().trim();
            if ("_blank".equalsIgnoreCase(a.attr("target")) && wbicon.equals("2")) {
                display.setPlace(a.text().replaceFirst("2", ""));
                a.remove();
            }
        }
        doc.select("i.wbicon, svg, use").remove();
        String content = doc.body().wholeText().trim();
        // —— 新增：去除零宽空格（U+200B） ——
        content = content.replaceAll("\\u200B", "");  // 针对 ZWSP 的专用清理

        // —— 新增：去除所有控制字符（含 C0/C1 及格式字符等） ——
        content = content.replaceAll("\\p{C}", "");
        content = content.trim();

        if (content.endsWith("收起")) {
            content = content.substring(0, content.length() - 2);
        }

        return content;
    }

    public void waitLoading() throws InterruptedException {
        Thread.sleep(LOADING_TIME);
    }

    public String logout() {
        if (driver != null) {
            driver.quit();
        }
        return "退出成功！";
    }

    @PreDestroy
    public void destroy() {
        logout();
    }


}
