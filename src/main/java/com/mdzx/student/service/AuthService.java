package com.mdzx.student.service;

import java.util.List;

import com.mdzx.student.dto.AuthDtos;
import com.mdzx.student.vo.AuthVos;

/**
 * AuthService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface AuthService {

    AuthVos.LoginResult login(AuthDtos.LoginRequest request);

    AuthVos.UserInfo currentUser();

    List<AuthVos.UserItem> listUsers();
}
