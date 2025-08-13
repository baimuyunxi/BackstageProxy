package com.example.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.AgentProxyRequest;
import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author：baimuyunxi
 * @Package：com.example.service
 * @Project：xunFeiProxy
 * @name：AgentProxyService
 * @Date：2025/8/7 15:35
 * @Filename：AgentProxyService
 */
@Service
public class AgentProxyService {

    private static final Logger logger = LoggerFactory.getLogger(AgentProxyService.class);

    @Value("${agent.api.key}")
    private String apiKey;

    private static final String TARGET_URL = "https://agent.teleai.com.cn/v1/chat-messages";

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)  // 读取超时设置为5分钟
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        logger.info("AgentProxyService初始化完成");
    }

    /**
     * 发送流式请求
     */
    public SseEmitter sendStreamRequest(AgentProxyRequest request) {
        // 设置5分钟超时
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5分钟 = 300秒 = 300000毫秒

        // 异步处理流式响应
        new Thread(() -> {
            try {
                String requestBody = buildRequestBody(request);

                Request httpRequest = new Request.Builder()
                        .url(TARGET_URL)
                        .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "text/event-stream")
                        .build();

                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    if (!response.isSuccessful()) {
                        JSONObject errorResponse = new JSONObject();
                        errorResponse.put("event", "error");
                        errorResponse.put("status", response.code());
                        errorResponse.put("message", "请求失败: " + response.code() + " " + response.message());
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorResponse.toJSONString()));
                        emitter.completeWithError(new IOException("请求失败: " + response.code()));
                        return;
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        emitter.completeWithError(new IOException("响应体为空"));
                        return;
                    }

                    // 处理流式响应 - 使用字节流读取，避免一次性读取全部内容
                    try (BufferedReader reader = new BufferedReader(
                            new java.io.InputStreamReader(responseBody.byteStream(),
                                    java.nio.charset.StandardCharsets.UTF_8))) {

                        String line;
                        long lastActivityTime = System.currentTimeMillis();

                        while ((line = reader.readLine()) != null) {
                            lastActivityTime = System.currentTimeMillis();

                            if (line.startsWith("data: ")) {
                                String jsonData = line.substring(6); // 移除 "data: " 前缀
                                if (StringUtils.isNotBlank(jsonData)) {
                                    try {
                                        JSONObject eventJson = JSON.parseObject(jsonData);
                                        String eventType = eventJson.getString("event");

                                        logger.debug("收到事件: {} - {}", eventType, jsonData);

                                        // 发送事件到客户端
                                        emitter.send(SseEmitter.event()
                                                .name(eventType)
                                                .data(jsonData));

                                        // 如果收到结束事件，完成流式传输
                                        if ("message_end".equals(eventType) ||
                                                "workflow_finished".equals(eventType) ||
                                                "error".equals(eventType)) {
                                            emitter.complete();
                                            return;
                                        }

                                    } catch (Exception e) {
                                        logger.error("解析事件数据失败: {}", jsonData, e);
                                    }
                                }
                            } else if ("data: [DONE]".equals(line)) {
                                // 某些 SSE 实现使用 [DONE] 表示结束
                                logger.debug("收到结束标志");
                                emitter.complete();
                                return;
                            } else if (line.trim().isEmpty()) {
                                // 空行，继续处理下一行
                                continue;
                            } else if (line.startsWith("event: ping")) {
                                // ping 事件，用于保持连接
                                logger.debug("收到 ping 事件");
                                emitter.send(SseEmitter.event().name("ping").data("{}"));
                            }

                            // 检查是否超过5分钟
                            if (System.currentTimeMillis() - lastActivityTime > 5 * 60 * 1000) {
                                logger.info("连接超过5分钟，主动断开");
                                emitter.complete();
                                return;
                            }
                        }

                        logger.debug("流式数据读取完成");
                        emitter.complete();
                    }
                }

            } catch (Exception e) {
                logger.error("流式请求处理失败", e);
                try {
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("event", "error");
                    errorResponse.put("message", "处理失败: " + e.getMessage());
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(errorResponse.toJSONString()));
                } catch (IOException ioException) {
                    logger.error("发送错误事件失败", ioException);
                }
                emitter.completeWithError(e);
            }
        }).start();

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            logger.info("SSE连接超时，自动断开");
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            logger.debug("SSE连接已完成");
        });

        emitter.onError((throwable) -> {
            logger.error("SSE连接发生错误", throwable);
        });

        return emitter;
    }

    /**
     * 发送阻塞模式请求（保持原有方法）
     */
    public JSONObject sendRequest(AgentProxyRequest request) throws IOException {
        String requestBody = buildRequestBody(request);

        Request httpRequest = new Request.Builder()
                .url(TARGET_URL)
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            logger.debug("收到响应: {}", responseBody);

            // 解析响应并提取answer字段
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            return jsonResponse;
        }
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(AgentProxyRequest request) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("input_data", new HashMap<>());

        // 使用userQuery作为query，如果不存在或为空则使用默认值
        String query = (request.getUserQuery() != null && StringUtils.isNotBlank(request.getUserQuery())) ?
                request.getUserQuery() : "请帮助处理这个请求";
        requestMap.put("query", query);

        requestMap.put("mode", "streaming");
        requestMap.put("conversation_id", "");
        requestMap.put("user", "admin");

        // 构建inputs
        Map<String, Object> inputData = new HashMap<>();

        // 映射请求字段到inputs，处理可能为null的情况
        addToInputIfNotEmpty(inputData, "call_id", request.getCallId());
        addToInputIfNotEmpty(inputData, "call_time", request.getCallDayYd());
        addToInputIfNotEmpty(inputData, "differ", request.getDifferentiation());
        addToInputIfNotEmpty(inputData, "business", request.getBusinessAll());
        addToInputIfNotEmpty(inputData, "dl_label", request.getDlLabel());
        addToInputIfNotEmpty(inputData, "rg_label", request.getRgLabel());
        addToInputIfNotEmpty(inputData, "first_id", request.getFirstSolution());
        addToInputIfNotEmpty(inputData, "call_number", request.getIncomingCall());
        addToInputIfNotEmpty(inputData, "refuse", request.getRejection());
        addToInputIfNotEmpty(inputData, "rob_call", request.getDialogueText());
        addToInputIfNotEmpty(inputData, "hw_text", request.getContent());
        addToInputIfNotEmpty(inputData, "hw_label", request.getTrafficLabel());

        // userNo和userQuery可能不存在，只在非空时添加
        addToInputIfNotEmpty(inputData, "user_no", request.getUserNo());
        addToInputIfNotEmpty(inputData, "user_query", request.getUserQuery());

        requestMap.put("inputs", inputData);

        String jsonBody = JSON.toJSONString(requestMap);
        logger.debug("构建的请求体: {}", jsonBody);

        return jsonBody;
    }

    /**
     * 只有当值不为null且不为空时才添加到输入数据中
     */
    private void addToInputIfNotEmpty(Map<String, Object> inputData, String key, String value) {
        if (value != null && StringUtils.isNotBlank(value)) {
            inputData.put(key, value);
        }
    }
}