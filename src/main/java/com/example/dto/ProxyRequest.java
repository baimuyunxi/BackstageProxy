package com.example.dto;

/**
 * @Author：baimuyunxi
 * @Package：com.example.dto
 * @Project：xunFeiProxy
 * @name：ProxyRequest
 * @Date：2025/5/16 09:47
 * @Filename：ProxyRequest
 */
public class ProxyRequest {
    private String contentData;
    private int timeoutSeconds;

    public ProxyRequest() {
    }

    public ProxyRequest(String contentData, int timeoutSeconds) {
        this.contentData = contentData;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getContentData() {
        return contentData;
    }

    public void setContentData(String contentData) {
        this.contentData = contentData;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
