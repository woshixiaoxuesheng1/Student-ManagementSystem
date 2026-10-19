package com.mdzx.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 测试用登录工具：拿真实 token，走完整的鉴权链路 */
final class AuthTestSupport {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    private AuthTestSupport() {
    }

    static String login(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password)
            throws Exception {
        String body = """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        if (json.get("code").asInt() != 0) {
            throw new IllegalStateException("登录失败：" + json.get("message").asText());
        }
        return json.at("/data/token").asText();
    }
}
