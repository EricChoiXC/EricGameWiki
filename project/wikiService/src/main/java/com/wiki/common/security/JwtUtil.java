package com.wiki.common.security;

import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具，负责登录 Token 的签发与校验。
 * <p>
 * 纯函数工具，不承载业务逻辑，遵循 {@code docs/common/业务流转公共规范.md} 2.3 分层职责。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USER_NAME = "userName";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Token。
     */
    public String generate(String userId, String userName) {
        long expireMillis = properties.getExpireMinutes() * 60L * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USER_NAME, userName)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token，返回用户ID；非法或过期抛 401 业务异常。
     */
    public String parseUserId(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            return claims.get(CLAIM_USER_ID, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token无效或已过期");
        }
    }
}
