package com.factory.longMethod.conditional.example1;

import java.util.Arrays;

/**
 * @author FangWenJie
 * @date 2021/4/6 15:31
 */
public enum ChatbotAuditState{
    UNKNOWN(-1, "unknown", "未知"),
    OPERATION_AUDITING(0, "operationAuditing", "待运营审核"),
    OPERATION_AUDIT_FAILED(1, "operationAuditFailed", "运营审核未通过"),
    CHANNEL_AUDITING(2, "channelAuditing", "待渠道商审核"),
    CHANNEL_AUDIT_PASS(3, "channelAuditPass", "渠道商审核通过"),
    CHANNEL_AUDIT_FAILED(4, "channelAuditFailed", "渠道商审核不通过");

    private final int index;
    private final String name;
    private final String message;

    ChatbotAuditState(int index, String name, String message) {
        this.index = index;
        this.name = name;
        this.message = message;
    }

    public static ChatbotAuditState getByIndex(int index) {
        return Arrays.stream(ChatbotAuditState.values())
                .filter(value -> value.getIndex() == index)
                .findFirst()
                .orElse(UNKNOWN);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }
}
