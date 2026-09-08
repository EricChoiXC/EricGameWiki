package com.wiki.common.security;

import com.wiki.common.constant.CommonConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器：从请求头解析 Token，加载用户上下文并写入
 * {@link UserContext}（供 resolver/Service 使用）与 Spring Security 上下文。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 4.6 鉴权流转：
 * 批量权限码仅用于按钮预判与上下文携带，最终鉴权仍由各接口 Resolver 完成。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserContextLoader userContextLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                String userId = jwtUtil.parseUserId(token);
                UserContext context = userContextLoader.loadByUserId(userId);
                if (context != null) {
                    UserContext.set(context);
                    List<SimpleGrantedAuthority> authorities = toAuthorities(context.getPermissionKeys());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(context.getUserId(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | com.wiki.common.exception.BusinessException e) {
                log.debug("JWT 认证失败: {}", e.getMessage());
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(CommonConstants.BEARER_PREFIX)) {
            return header.substring(CommonConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private List<SimpleGrantedAuthority> toAuthorities(List<String> permissionKeys) {
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>(permissionKeys.size());
        for (String key : permissionKeys) {
            authorities.add(new SimpleGrantedAuthority(key));
        }
        return authorities;
    }
}
