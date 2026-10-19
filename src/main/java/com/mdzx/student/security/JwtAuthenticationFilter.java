package com.mdzx.student.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.SysUser;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.UserMapper;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 解析 Authorization: Bearer <token>，每次请求按当前库里的账号状态重建登录态 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;

    /** 注入令牌解析器及用户、班级查询器。 */
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserMapper userMapper, ClassMapper classMapper) {
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.classMapper = classMapper;
    }

    /** 从请求令牌恢复当前用户和班级权限，再交给后续过滤器处理。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Long userId = tokenProvider.parseUserId(token);
                SysUser user = userMapper.selectById(userId);
                if (user != null && Boolean.TRUE.equals(user.getEnabled())) {
                    List<String> classIds = userMapper.selectClassIds(userId);
                    List<String> classNames = classIds.isEmpty()
                            ? List.of()
                            : classMapper.selectAll(null, null).stream()
                                    .filter(info -> classIds.contains(info.getClassId()))
                                    .map(ClassInfo::getClassName)
                                    .toList();
                    LoginUser loginUser = new LoginUser(user, classIds, classNames);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // 令牌无效或过期：当作未登录，交给后续的鉴权入口返回 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
