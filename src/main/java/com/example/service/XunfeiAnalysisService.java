package com.example.service;

import com.example.xunfei.XunfeiAdapterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @Author：baimuyunxi
 * @Package：com.example.service
 * @Project：xunFeiProxy
 * @name：XunfeiAnalysisService
 * @Date：2025/5/16 09:45
 * @Filename：XunfeiAnalysisService
 */
@Service
public class XunfeiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(XunfeiAnalysisService.class);

    // 控制并发请求数，避免对讯飞API造成过大压力
    private final Semaphore rateLimitSemaphore = new Semaphore(10);

    /**
     * 分析内容数据
     *
     * @param contentData 要分析的内容
     * @param timeoutSeconds 超时时间（秒）
     * @return 分析结果
     * @throws Exception 分析过程中的异常
     */
    public List<String> analyzeContent(String contentData, int timeoutSeconds) throws Exception {
        // 获取限流许可
        rateLimitSemaphore.acquire();

        try {
            log.info("Starting analysis, content length: {}, timeout: {}s",
                    contentData.length(), timeoutSeconds);

            // 调用讯飞适配器
            List<String> results = XunfeiAdapterProxy.callXunfeiModel(contentData, timeoutSeconds);

            log.info("Analysis completed successfully, result count: {}", results.size());
            return results;

        } catch (Exception e) {
            log.error("Analysis failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            // 释放限流许可
            rateLimitSemaphore.release();
        }
    }

    /**
     * 获取服务状态
     *
     * @return 服务状态信息
     */
    public String getServiceStatus() {
        return String.format("Service running, available permits: %d",
                rateLimitSemaphore.availablePermits());
    }
}
