package com.example.controller;

import com.example.service.rebotProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Author：baimuyunxi
 * @Package：com.example.controller
 * @Project：xunFeiProxy
 * @name：rebotProxyController
 * @Date：2025/6/11 09:11
 * @Filename：rebotProxyController
 */
@RestController
@RequestMapping("/api/rebot")
public class rebotProxyController {

    private static final Logger logger = LoggerFactory.getLogger(rebotProxyController.class);

    @Autowired
    private rebotProxyService rebotProxyService;

    @PostMapping("/botProxy")
    public ResponseEntity<String> botProxy(@RequestBody Map<String, Object> requestMap,
                                           @RequestParam("url") String targetUrl) {
        try {
            logger.info("收到代理请求，目标URL: {}", targetUrl);
            rebotProxyService.sendProxyRequest(requestMap, targetUrl);
            return ResponseEntity.ok("代理请求发送成功");
        } catch (Exception e) {
            logger.error("代理请求发送失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("代理请求发送失败: " + e.getMessage());
        }
    }
}


