package run.mone.mcp.weibo.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
public class HttpClientUtil {
    private static OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private HttpClientUtil() {}


    /**
     * GET 请求
     * @param url 请求地址
     * @param params 查询参数（可为null）
     */
    public static String get(String url, Map<String, String> params) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        return executeRequest(request);
    }


    public static String get(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build())
                .get();
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        return executeRequest(request);
    }

    /**
     * POST 表单请求
     * @param url 请求地址
     * @param formParams 表单参数（可为null）
     */
    public static String postForm(String url, Map<String, String> formParams) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        if (formParams != null) {
            formParams.forEach(builder::add);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        return executeRequest(request);
    }

    public static String postForm(String url, Map<String, String> formParams, Map<String, String> headers) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        if (formParams != null) {
            formParams.forEach(builder::add);
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(builder.build());

        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        Request request = requestBuilder.build();
        return executeRequest(request);
    }

    /**
     * POST JSON 请求
     * @param url 请求地址
     * @param json JSON字符串
     */
    public static String postJson(String url, String json) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return executeRequest(request);
    }

    public static String postJson(String url, String json,Map<String, String> headers) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();

        return executeRequest(request);
    }


    /**
     * 通用请求执行方法
     */
    private static String executeRequest(Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.info("请求失败，状态码: {}，消息: {}", response.code(), response.message());
                return "";
            }
            ResponseBody body = response.body();

            return body != null ? body.string() : null ;
        }
    }

    /**
     * 设置自定义 OkHttpClient（可选）
     */
    public static void setCustomClient(OkHttpClient client) {
        CLIENT.dispatcher().executorService().shutdown();
        CLIENT.connectionPool().evictAll();
        CLIENT = client;
    }



    public static String getHtml(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build())
                .get();
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.info("请求失败，状态码: {}，消息: {}", response.code(), response.message());
                return "";
            }
            ResponseBody body = response.body();
            if (body != null) {
                byte[] bytes = body.bytes();
                String res = new String(bytes, "GB2312");
                return res;
            }
            return null ;
        }
    }
}
