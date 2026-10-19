package com.mdzx.student.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** HS256 令牌：只放用户 id，角色与班级每次请求从库里读，保证改权限立即生效 */
@Component
public class JwtTokenProvider {

    /** token： Header.payload.signature */
    private final SecretKey key;
    private final String issuer;
    private final long expireMillis;

    /** 读取密钥、签发者和有效期配置以创建令牌服务。 */
    public JwtTokenProvider(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        // 设置存在时间
        this.expireMillis = expireHours * 3600_000L;
    }

    /** 为登录用户签发 JWT，并将用户编号写入令牌。 */
    public String createToken(LoginUser user) {
        return Jwts.builder()
                //将当前登录用户的id转换为字符串类型并标识当前登录用户ID
                .subject(String.valueOf(user.getId()))
                // 设置token的签发者，验证时也需要相同的签发者
                .issuer(issuer)
                // 自定义声明 将该信息存在payload "username": "user.getUsername()"
                .claim("username", user.getUsername())
                // 获取当前登录用户角色 并写入payload
                .claim("role", user.getRole().name())
                // 将当前时间设置为token的签发时间
                .issuedAt(new Date())
                // 设置token的过期时间 当前时间加上expireMills
                .expiration(new Date(new Date().getTime() + expireMillis))
                // 使用密钥对token签名, 可以防止token被修改，以及验证
                .signWith(key)
                // 将之前配置好的 JWT 内容进行序列化、编码和签名，最终生成 Token 字符串
                .compact();
    }

    /** 解析失败（过期、签名不对）统一抛 JwtException，由过滤器当作未登录处理 */
    public Long parseUserId(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /** 返回令牌的有效时长（秒）。 */
    public long getExpireSeconds() {
        return expireMillis / 1000;
    }
}
