package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.AgentProxyRequest;
import com.example.service.AgentProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/agentProxy")
    public JSONObject agentProxy(@RequestBody AgentProxyRequest request) {
        try {
            logger.info("接收到代理请求，callId: {}, query: {}",
                    request.getCallId(), request.getUserQuery());

            return agentProxyService.sendRequest(request);

        } catch (Exception e) {
            logger.error("代理请求处理失败", e);
            return new JSONObject(Integer.parseInt("请求处理失败: " + e.getMessage()));
        }
    }
}