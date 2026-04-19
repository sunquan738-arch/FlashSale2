package com.flashsale.server.infrastructure.mq.outbox;

public enum OutboxEventStatus {
    NEW(0),
    SENT(1),
    DEAD(2);

    private final int code;

    OutboxEventStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
