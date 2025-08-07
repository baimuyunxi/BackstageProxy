package com.example.entity;

import lombok.Data;

/**
 * @Author：baimuyunxi
 * @Package：com.example.entity
 * @Project：xunFeiProxy
 * @name：AgentProxyRequest
 * @Date：2025/8/7 15:41
 * @Filename：AgentProxyRequest
 */
@Data
public class AgentProxyRequest {

    private String callId;

    private String callDayYd;

    private String differentiation;

    private String businessAll;

    private String dlLabel;

    private String rgLabel;

    private String firstSolution;

    private String incomingCall;

    private String rejection;

    private String dialogueText;

    private String content;

    private String trafficLabel;

    private String userNo;

    private String userQuery;
}
