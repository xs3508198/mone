package run.mone.mcp.weibo.function;

import jakarta.annotation.PreDestroy;
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

@Slf4j
public class WeiboFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> stringObjectMap) {
        return null;
    }

    private WebDriver driver;

    private static final String LOGIN_URL = "https://passport.weibo.com/sso/signin?entry=wapsso&source=wapsso&url=";
    private static final String WEIBO_HOT = "https://weibo.com/newlogin?tabtype=search&gid=&openLoginLayer=0&url=https%3A%2F%2Fweibo.com%2Fhot%2Fsearch";
    private static final String SEARCH_WEIBO = "https://s.weibo.com/weibo?q=";
    private static final String SEARCH_USER = "https://s.weibo.com/user?q=";
    private static final String MY_FOLLOW_WEIBO = "https://weibo.com/";
    private static final String RECOMMEND_WEIBO = "https://weibo.com/hot/weibo/102803";
    private static final String MOBILE_WEIBO  = "https://m.weibo.cn/";


    private static final String CHROME_DRIVER_PATH = "/Users/a1/chromedriver/chromedriver-mac-arm64/chromedriver";

    static {
        // 在静态块中设置，确保只设置一次
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
    }


    public String createWebDriver() throws InterruptedException {
        if (System.getProperty("webdriver.chrome.driver") == null || System.getProperty("webdriver.chrome.driver").isEmpty()) {
            System.setProperty("webdriver.chrome.driver", "CHROME_DRIVER_PATH");
        }
        ChromeOptions options = new ChromeOptions();
        // 自动允许地理位置访问
        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.geolocation", 1); // 1=允许，2=阻止
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        WebDriver webDriver = new ChromeDriver(options);
        ((JavascriptExecutor) webDriver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        webDriver.get(LOGIN_URL);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.urlContains("https://weibo.com/"));


        //options.addArguments("--headless");

        WebDriver newWebDriver = new ChromeDriver(options);
        ((JavascriptExecutor) newWebDriver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        // 访问 m.weibo.cn，确保生成相关 Cookies
        webDriver.get(MOBILE_WEIBO);
        newWebDriver.get(MOBILE_WEIBO);
        Thread.sleep(800); // 等待页面加载

        Set<Cookie> cookies = webDriver.manage().getCookies();
        System.out.println(gson.toJson(cookies));
        if (cookies.isEmpty()) {
            return "登录失败！请重新登录";
        }
        for (Cookie cookie : cookies) {
            Thread.sleep(500);
            newWebDriver.manage().addCookie(cookie);
        }

        // 访问 weibo.com，确保生成相关 Cookies
        webDriver.get(MY_FOLLOW_WEIBO);
        newWebDriver.get(MY_FOLLOW_WEIBO);
        String localStorage = (String) ((JavascriptExecutor) webDriver).executeScript("return JSON.stringify(localStorage);");
        String sessionStorage = (String) ((JavascriptExecutor) webDriver).executeScript("return JSON.stringify(sessionStorage);");

        Set<Cookie> cookies2 = webDriver.manage().getCookies();
        System.out.println(gson.toJson(cookies2));
        for (Cookie cookie : cookies2){
            Thread.sleep(500);
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
        Thread.sleep(300);
        return "登录成功！";
    }

    public List<WeiboContentDisplay> myFollowWeibo(int scrollTimes) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return new ArrayList<>();
        }
        List<WeiboContentDisplay> res = processMsg(driver, MY_FOLLOW_WEIBO, scrollTimes);
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return res;
        } else {
            log.warn("搜索结果为空！");
            return new ArrayList<>();
        }
    }

    private List<WeiboContentDisplay> processMsg(WebDriver webDriver, String url, int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        webDriver.get(url);
        Thread.sleep(500);

        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i < scrollTimes; i++) {
            ((JavascriptExecutor) webDriver).executeScript(
                    "document.querySelectorAll('span.expand').forEach(function(el){el.click();});"
            );
            Thread.sleep(1000);
            String scrollHtml = webDriver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) webDriver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(1000);
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
                String time = timeElement.text().trim();
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

    public List<WeiboContentDisplay> recommendWeibo(int scrollTimes) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return new ArrayList<>();
        }
        List<WeiboContentDisplay> res = processMsg(driver, RECOMMEND_WEIBO, scrollTimes);
        ;
        if (!res.isEmpty()) {
            log.info("搜索结果：" + res.size() + "条");
            return res;
        } else {
            log.warn("搜索结果为空！");
            return new ArrayList<>();
        }
    }


    public Map<String, String> weiboHot() throws Exception {
        Map<String, String> res = new TreeMap<>(new Comparator<String>() {

            @Override
            public int compare(String key1, String key2) {
                return Integer.compare(Integer.parseInt(key1), Integer.parseInt(key2));
            }
        });
        if (driver == null) {
            log.error("未登录，请您登录！");
            return res;
        }
        driver.get(WEIBO_HOT);
        Thread.sleep(1500);
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
            Thread.sleep(100);
        }
        return res;
    }

    public List<WeiboContentDisplay> searchWeibo(String keyword, int page) throws IOException {
        List<WeiboContentDisplay> res = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String searchUrl = SEARCH_WEIBO + encodedKeyword;
        if (page > 1) {
            searchUrl = searchUrl + "&page=" + page;
        }
        if (driver == null) {
            log.error("未登录，请您登录！");
            return res;
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
            return res;
        } else {
            log.warn("搜索结果为空！");
            return new ArrayList<>();
        }

    }

    //1：转发， 2：评论，3：点赞
    public List<String> weiboDetail(String weiboUrl, String content, int type) throws Exception {
        if (driver == null) {
            log.error("未登录，请您登录！");
            return new ArrayList<>();
        }
        List<String> res = new ArrayList<>();
        driver.get(weiboUrl);
        Thread.sleep(1000);
        switch (type) {
            case 1:
                res = processRepostDetail(driver, content);
                break;
            case 2:
                res = processCommentDetail(driver, content);
                break;
            case 3:
                res = processLikeDetail(driver, content);
                break;
            default:
                res = processCommentDetail(driver, content);
                break;
        }
        return res;
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
            Thread.sleep(1000);
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
            Thread.sleep(300);
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
        Thread.sleep(200);
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
            Thread.sleep(1000);
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
                String disPlay = time +  "【" + username + "】: " + content ;
                if (!res.contains(disPlay)) {
                    res.add(disPlay);
                }
            }
            ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, window.innerHeight * 0.5);");
            Thread.sleep(300);
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
        Thread.sleep(300);

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
            Thread.sleep(300);
            if (res.size() >= 100) {
                break;
            }
        }
        return res;
    }


    public List<WeiboUserDisplay> searchUsers(String userKeyword, int page) {

        List<WeiboUserDisplay> res = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(userKeyword, StandardCharsets.UTF_8);
        String searchUrl = SEARCH_USER + encodedKeyword + "&Refer=weibo_user";
        if (page > 1) {
            searchUrl = searchUrl + "&page=" + page;
        }
        if (driver == null) {
            log.error("未登录，请您登录！");
            return res;
        }
        driver.get(searchUrl);
        String html = driver.getPageSource();
        Document doc = Jsoup.parse(html);
        Element element = doc.select("div.card-wrap").first();
        if (element == null) {
            return new ArrayList<>();
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
            return new ArrayList<>();
        }
        return res;

    }

    public List<WeiboContentDisplay> localWeibo(int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        driver.get(MOBILE_WEIBO);
        List<WebElement> items = driver.findElements(By.cssSelector("li.item_li"));
        List<WebElement> cur_items = driver.findElements(By.cssSelector("li.cur"));
        items.addAll(cur_items);
        for (WebElement item : items) {
            if (item.getText().equals("同城")){
                item.click();
                Thread.sleep(800);
                break;
            }
        }
        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i <= scrollTimes; i++) {
            String scrollHtml = driver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(300);
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
                    if (img != null && img.attr("src") .equals("https://h5.sinaimg.cn/upload/2015/09/25/3/timeline_card_small_location_default.png")){
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
        return res;


    }

    public List<WeiboContentDisplay> hereAndNowWeibo(int scrollTimes) throws Exception {
        List<WeiboContentDisplay> res = new ArrayList<>();
        driver.get(MOBILE_WEIBO);
        Thread.sleep(500);
        System.out.println(gson.toJson(driver.manage().getCookies()));
        List<WebElement> items = driver.findElements(By.cssSelector("li.item_li"));
        List<WebElement> cur_items = driver.findElements(By.cssSelector("li.cur"));
        items.addAll(cur_items);
        for (WebElement item : items) {
            if (item.getText().equals("同城")){
                item.click();
                Thread.sleep(800);
                break;
            }
        }
        List<WebElement> items2 = driver.findElements(By.cssSelector("div.card19-mode"));
        for (WebElement item : items2) {
            if (item.findElement(By.cssSelector("h4.m-text-cut")).getText().equals("附近")){
                item.findElement(By.cssSelector("div.m-box-center-a")).click();
                Thread.sleep(500);
            }
        }
        WebElement localElement = driver.findElement(By.cssSelector("ul.center"));
        localElement.findElements(By.cssSelector("li")).get(0).click();
        Thread.sleep(500);
        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i <= scrollTimes; i++) {
            String scrollHtml = driver.getPageSource();
            htmlList.add(scrollHtml);
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(300);
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
                    if (img != null && img.attr("src") .equals("https://h5.sinaimg.cn/upload/2015/09/25/3/timeline_card_small_location_default.png")){
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
        return res;

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
        if (content.endsWith("收起")) {
            content = content.substring(0, content.length() - 2);
        }

        return content;
    }

    public void logout(){
        if (driver != null) {
            driver.quit();
        }
    }

    @PreDestroy
    public void destroy(){
        logout();
    }


}
