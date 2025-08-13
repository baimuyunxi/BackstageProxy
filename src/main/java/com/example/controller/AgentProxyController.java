package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.AgentProxyRequest;
import com.example.service.AgentProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author：baimuyunxi
 * @Package：com.example.controller
 * @Project：xunFeiProxy
 * @name：AgentProxyController
 * @Date：2025/8/7 15:33
 * @Filename：AgentProxyController
 */
@RestController
@RequestMapping("/api")
public class AgentProxyController {

    private static final Logger logger = LoggerFactory.getLogger(AgentProxyController.class);

    @Autowired
    private AgentProxyService agentProxyService;

    /**
     * 流式响应接口
     */
    @PostMapping(value = "/agentProxy/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter agentProxyStream(@RequestBody AgentProxyRequest request) {
        try {
            logger.info("接收到流式代理请求，callId: {}", request.getCallId());

            return agentProxyService.sendStreamRequest(request);

        } catch (Exception e) {
            logger.error("流式代理请求处理失败", e);
            SseEmitter errorEmitter = new SseEmitter();
            try {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("event", "error");
                errorResponse.put("message", "请求处理失败: " + e.getMessage());
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorResponse.toJSONString()));
                errorEmitter.complete();
            } catch (Exception ex) {
                logger.error("发送错误响应失败", ex);
                errorEmitter.completeWithError(ex);
            }
            return errorEmitter;
        }
    }

    /**
     * 原有的阻塞模式接口（保持兼容）
     */
    @PostMapping("/agentProxy")
    public JSONObject agentProxy(@RequestBody AgentProxyRequest request) {
        try {
            logger.info("接收到代理请求，callId: {}, query: {}",
                    request.getCallId(), request.getUserQuery());

            return agentProxyService.sendRequest(request);

        } catch (Exception e) {
            logger.error("代理请求处理失败", e);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", true);
            errorResponse.put("message", "请求处理失败: " + e.getMessage());
            return errorResponse;
        }
    }
}