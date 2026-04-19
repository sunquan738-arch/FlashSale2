package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordMigrationProperties {

    /**
     * 是否允许兼容明文历史密码（仅迁移期启用）。
     */
    private boolean allowPlainCompat = true;

    /**
     * 命中明文兼容后，是否自动升级为 PBKDF2 存储。
     */
    private boolean autoUpgradeOnLogin = true;
}

