package com.example.dto;

import java.util.List;

/**
 * @Author：baimuyunxi
 * @Package：com.example.dto
 * @Project：xunFeiProxy
 * @name：ProxyResponse
 * @Date：2025/5/16 09:47
 * @Filename：ProxyResponse
 */
public class ProxyResponse {
    private boolean success;
    private List<String> data;
    private String errorMessage;

    public ProxyResponse() {
    }

    public ProxyResponse(boolean success, List<String> data) {
        this.success = success;
        this.data = data;
    }

    public ProxyResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
