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
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

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

    public void sendProxyRequest(Map<String, Object> requestData, String targetUrl) {
        try {
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
