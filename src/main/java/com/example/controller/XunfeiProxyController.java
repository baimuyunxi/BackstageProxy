package com.example.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.dto.ProxyRequest;
import com.example.dto.ProxyResponse;
import com.example.service.XunfeiAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：baimuyunxi
 * @Package：com.example.controller
 * @Project：xunFeiProxy
 * @name：XunfeiProxyController
 * @Date：2025/5/16 09:46
 * @Filename：XunfeiProxyController
 */
@RestController
@RequestMapping("/api/xunfei")
public class XunfeiProxyController {

    private static final Logger log = LoggerFactory.getLogger(XunfeiProxyController.class);

    @Autowired
    private XunfeiAnalysisService analysisService;

    @PostMapping("/analyze")
    public ProxyResponse analyze(@RequestBody ProxyRequest request) {
        try {
            log.info("Received analysis request, content length: {}, timeout: {}s",
                    request.getContentData() != null ? request.getContentData().length() : 0,
                    request.getTimeoutSeconds());

            // 参数验证
            if (request.getContentData() == null || request.getContentData().trim().isEmpty()) {
                return new ProxyResponse(false, "Content data cannot be empty");
            }

            // 设置默认超时时间
            int timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 300;

            // 调用分析服务
            List<String> results = analysisService.analyzeContent(request.getContentData(), timeout);

            log.info("Analysis completed successfully, result count: {}", results.size());
            return new ProxyResponse(true, results);

        } catch (Exception e) {
            log.error("Error during analysis: {}", e.getMessage(), e);
            return new ProxyResponse(false, "Analysis failed: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Xunfei Proxy Service is running";
    }

    @GetMapping("/status")
    public String status() {
        return analysisService.getServiceStatus();
    }
}
