package com.wiki.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，绑定 application.yml 中 {@code wiki.jwt} 节点。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
@Component
@ConfigurationProperties(prefix = "wiki.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret;

    /** 过期时间（分钟） */
    private Long expireMinutes = 720L;

    /** 签发者 */
    private String issuer = "wikiService";
}
