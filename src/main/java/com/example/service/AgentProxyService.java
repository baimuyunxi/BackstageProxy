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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Value("${agent.api.key:your_api_key_here}")
    private String apiKey;

    private static final String TARGET_URL = "https://agent.sxteyou.com/v1/chat-messages";

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        logger.info("AgentProxyService初始化完成");
    }

    /**
     * 发送阻塞模式请求
     */
    public String sendRequest(AgentProxyRequest request) throws IOException {
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
            return jsonResponse.getString("answer");
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

        requestMap.put("mode", "blocking");
        requestMap.put("conversation_id", "");
        requestMap.put("user", "admin");

        // 构建inputs数组
        List<Map<String, Object>> inputs = new ArrayList<>();
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

        inputs.add(inputData);
        requestMap.put("inputs", inputs);

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