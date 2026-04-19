package com.flashsale.server.infrastructure.mq.outbox;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutboxPublishResult {

    private boolean success;

    private String error;
}
