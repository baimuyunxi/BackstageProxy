package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author：baimuyunxi
 * @Package：com.example.service
 * @Project：xunFeiProxy
 * @name：rebotProxyService
 * @Date：2025/6/11 09:12
 * @Filename：rebotProxyService
 */
@Service
public class rebotProxyService {

    private static final Logger logger = LoggerFactory.getLogger(rebotProxyService.class);

    // 缓存最近的请求，key为请求标识，value为时间戳
    private final ConcurrentHashMap<String, Long> requestCache = new ConcurrentHashMap<>();

    // 3分钟的毫秒数
    private static final long CACHE_DURATION = 3 * 60 * 1000;

    public void sendProxyRequest(Map<String, Object> requestData, String targetUrl) {
        try {
            logger.info("开始处理代理请求，目标URL: {}", targetUrl);
//            logger.info("请求数据: {}", requestData);

            // 生成请求的唯一标识
            String requestKey = generateRequestKey(requestData, targetUrl);
            logger.info("生成的请求标识: {}", requestKey);

            // 检查是否在缓存时间内
            if (isDuplicateRequest(requestKey)) {
                logger.info("3分钟内相同请求已存在，跳过发送。请求标识: {}", requestKey);
                logger.info("当前缓存大小: {}", requestCache.size());
                return;
            }

            logger.info("请求不重复，准备发送");
            // 记录当前请求
            requestCache.put(requestKey, System.currentTimeMillis());
            logger.info("请求已缓存，当前缓存大小: {}", requestCache.size());

            // 清理过期的缓存
            cleanExpiredCache();

            // 发送实际请求
            sendActualRequest(requestData, targetUrl);

        } catch (Exception e) {
            logger.error("代理请求处理失败：{}", e.getMessage(), e);
            throw new RuntimeException("代理请求处理失败", e);
        }
    }

    /**
     * 生成请求的唯一标识
     */
    private String generateRequestKey(Map<String, Object> requestData, String targetUrl) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(requestData);

            // 移除时间戳，只保留核心业务内容用于比较
            String normalizedContent = normalizeContent(jsonString);
            String combined = targetUrl + ":" + normalizedContent;

            logger.debug("原始内容: {}", jsonString);
            logger.debug("标准化后内容: {}", normalizedContent);
            logger.debug("组合字符串: {}", combined);

            // 使用MD5生成哈希值作为唯一标识
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String result = sb.toString();
            logger.debug("生成的MD5哈希: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("生成请求标识失败", e);
            // 如果生成失败，使用简单的字符串拼接
            String fallback = targetUrl + ":" + requestData.toString();
            logger.info("使用备用标识: {}", fallback);
            return fallback;
        }
    }

    /**
     * 标准化内容，移除时间戳等动态部分
     */
    private String normalizeContent(String content) {
        // 移除时间戳 "截止至 HH:mm:ss" 部分
        String normalized = content.replaceAll("截止至 \\d{2}:\\d{2}:\\d{2}", "截止至 XX:XX:XX");

        // 还可以根据需要移除其他动态内容
        // 比如：normalized = normalized.replaceAll("\\d{4}-\\d{2}-\\d{2}", "YYYY-MM-DD");

        return normalized;
    }

    /**
     * 检查是否为重复请求
     */
    private boolean isDuplicateRequest(String requestKey) {
        Long lastRequestTime = requestCache.get(requestKey);
        logger.info("检查重复请求 - 请求标识: {}, 上次请求时间: {}", requestKey, lastRequestTime);

        if (lastRequestTime == null) {
            logger.info("未找到相同请求记录，允许发送");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastRequestTime;
        logger.info("时间差: {} ms, 缓存时间限制: {} ms", timeDiff, CACHE_DURATION);

        boolean isDuplicate = timeDiff < CACHE_DURATION;
        logger.info("是否重复请求: {}", isDuplicate);

        return isDuplicate;
    }

    /**
     * 清理过期的缓存
     */
    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int sizeBefore = requestCache.size();
        requestCache.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) >= CACHE_DURATION
        );
        int sizeAfter = requestCache.size();
        if (sizeBefore != sizeAfter) {
            logger.info("清理过期缓存，清理前: {}, 清理后: {}", sizeBefore, sizeAfter);
        }
    }

    /**
     * 发送实际的HTTP请求
     */
    private void sendActualRequest(Map<String, Object> requestData, String targetUrl) {
        try {
            logger.info("开始发送实际HTTP请求");

            // 忽略 SSL 验证
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // 忽略主机名验证
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            URI uri = new URI(targetUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = new ObjectMapper().writeValueAsString(requestData);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            logger.info("代理请求发送成功！状态码：{}", responseCode);

            // 如果需要返回目标服务器的响应内容，可以在这里读取
        } catch (Exception e) {
            logger.error("代理请求发送失败：{}", e.getMessage(), e);
            throw new RuntimeException("代理请求发送失败", e);
        }
    }
}
