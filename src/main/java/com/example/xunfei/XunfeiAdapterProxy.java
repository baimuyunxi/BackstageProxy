package com.example.xunfei;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Project ：xiaoyi
 * @File ：XunfeiAdapter.java
 * @IDE ：IntelliJ IDEA
 * @Author ：baimuyunxi
 * @E-Mail ：1365402987@qq.com
 * @Date ：2025/04/28 10:30
 * @Description ：讯飞大模型调用适配器，封装了与讯飞大模型的交互逻辑
 */
public class XunfeiAdapterProxy {
    private static final Logger log = LoggerFactory.getLogger(XunfeiAdapterProxy.class);

    // 设置默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT = 300;

    /**
     * 调用讯飞大模型并返回结果
     *
     * @param contentData 请求内容
     * @return 模型返回的结果
     * @throws Exception 调用过程中的异常
     */
    public static List<String> callXunfeiModel(String contentData) throws Exception {
        return callXunfeiModel(contentData, DEFAULT_TIMEOUT);
    }

    /**
     * 调用讯飞大模型并返回结果（带超时）
     *
     * @param contentData    请求内容
     * @param timeoutSeconds 超时时间（秒）
     * @return 模型返回的结果
     * @throws Exception 调用过程中的异常
     */
    public static List<String> callXunfeiModel(String contentData, int timeoutSeconds) throws Exception {
        CompletableFuture<List<String>> resultFuture = new CompletableFuture<>();

        // 1. 构建鉴权url
        String authUrl = ChatAIXProxy.getAuthUrl(ChatAIXProxy.hostUrl, ChatAIXProxy.apiKey, ChatAIXProxy.apiSecret);

        // 2. 创建OkHttp客户端
        OkHttpClient client = new OkHttpClient.Builder().build();
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(wsUrl).build();

        // 3. 创建自定义WebSocketListener
        XunfeiResponseListener listener = new XunfeiResponseListener(resultFuture);

        // 4. 建立WebSocket连接
        WebSocket webSocket = client.newWebSocket(request, listener);

        // 5. 发送请求数据
        log.info("Sending request to Xunfei model");
        JSONObject requestJson = buildRequestJson(contentData);
        webSocket.send(requestJson.toString());

        // 6. 等待结果（带超时）
        try {
            return resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error waiting for Xunfei model response: {}", e.getMessage());
            webSocket.close(1000, "Timeout or error");
            throw e;
        }
    }

    /**
     * 构建讯飞模型请求JSON
     *
     * @param contentData 用户内容
     * @return 请求JSON对象
     */
    private static JSONObject buildRequestJson(String contentData) {
        JSONObject requestJson = new JSONObject();

        // 构建header部分
        JSONObject header = new JSONObject();
        header.put("app_id", ChatAIXProxy.appid);

        // 构建parameter部分
        JSONObject parameter = new JSONObject();
        JSONObject chat = new JSONObject();

        // 构建tools数组
        JSONArray toolsArray = new JSONArray();

        // 创建web_search对象
        JSONObject webSearch = new JSONObject();
        webSearch.put("search_mode", "normal");
        webSearch.put("enable", false);

        // 创建web_search工具对象
        JSONObject webSearchTool = new JSONObject();
        webSearchTool.put("web_search", webSearch);
        webSearchTool.put("type", "web_search");

        // 将工具添加到数组中
        toolsArray.add(webSearchTool);

        chat.put("domain", ChatAIXProxy.domain);
        chat.put("temperature", 0.5);
        chat.put("max_tokens", 32768);
        chat.put("top_k", 4);
        chat.put("tools", toolsArray);
        parameter.put("chat", chat);

        // 构建payload部分
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        JSONArray text = new JSONArray();

        // 添加system角色消息
        JSONObject systemRole = new JSONObject();
        systemRole.put("role", "system");
        systemRole.put("content", ChatAIXProxy.SYSTEM_DATA);
        text.add(systemRole);

        // 添加user角色消息
        JSONObject userRole = new JSONObject();
        userRole.put("role", "user");
        userRole.put("content", contentData);
        text.add(userRole);

        message.put("text", text);
        payload.put("message", message);

        // 组合完整请求
        requestJson.put("header", header);
        requestJson.put("parameter", parameter);
        requestJson.put("payload", payload);

        return requestJson;
    }

    /**
     * 自定义WebSocketListener处理讯飞大模型的流式响应
     */
    private static class XunfeiResponseListener extends okhttp3.WebSocketListener {
        private final CompletableFuture<List<String>> resultFuture;
        private final List<String> responses = new ArrayList<>();
        private final StringBuilder currentResponse = new StringBuilder();
        private boolean wsCloseFlag = false;

        public XunfeiResponseListener(CompletableFuture<List<String>> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response) {
            log.info("WebSocket connection opened to Xunfei");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                // 解析响应JSON
                JSONObject response = JSON.parseObject(text);
                JSONObject header = response.getJSONObject("header");

                // 检查响应状态码
                if (header.getIntValue("code") != 0) {
                    String errMsg = "Error from Xunfei, code: " + header.getIntValue("code") +
                            ", sid: " + header.getString("sid");
                    log.error(errMsg);
                    resultFuture.completeExceptionally(new RuntimeException(errMsg));
                    return;
                }

                // 提取响应内容
                if (response.containsKey("payload")) {
                    JSONObject payload = response.getJSONObject("payload");
                    if (payload.containsKey("choices")) {
                        JSONObject choices = payload.getJSONObject("choices");

                        // 检查是否有text数组
                        if (choices.containsKey("text")) {
                            JSONArray textArray = choices.getJSONArray("text");
                            if (!textArray.isEmpty()) {
                                JSONObject textObj = textArray.getJSONObject(0);

                                // 提取content字段
                                if (textObj.containsKey("content")) {
                                    String content = textObj.getString("content");
                                    currentResponse.append(content);
                                }
                            }
                        }
                    }
                }

                // 检查是否是最终响应
                int status = header.getIntValue("status");
                if (status == 2) {
                    log.info("Received final response from Xunfei");
                    wsCloseFlag = true;

                    // 添加完整的响应到结果列表
                    if (currentResponse.length() > 0) {
                        responses.add(currentResponse.toString());
                    }

                    // 完成Future
                    resultFuture.complete(responses);
                    webSocket.close(1000, "Completed");
                }
            } catch (Exception e) {
                log.error("Error processing Xunfei response: {}", e.getMessage(), e);
                resultFuture.completeExceptionally(e);
                webSocket.close(1000, "Error: " + e.getMessage());
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
            String errMsg = "WebSocket failure: " + t.getMessage();
            log.error(errMsg, t);

            if (response != null) {
                log.error("Response code: {}", response.code());
            }

            resultFuture.completeExceptionally(t);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            log.info("WebSocket closing: {}, {}", code, reason);
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.info("WebSocket closed: {}, {}", code, reason);
            wsCloseFlag = true;

            // 如果Future还没完成，完成它
            if (!resultFuture.isDone()) {
                if (currentResponse.length() > 0) {
                    responses.add(currentResponse.toString());
                }
                resultFuture.complete(responses);
            }
        }

        public boolean isWsCloseFlag() {
            return wsCloseFlag;
        }
    }
}