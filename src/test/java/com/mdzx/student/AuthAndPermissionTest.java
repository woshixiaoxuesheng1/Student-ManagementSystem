package com.mdzx.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.service.DemoDataService;

/** 登录与角色权限：未登录、越权写、班主任数据范围 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private StudentMapper studentMapper;

    @BeforeEach
    void prepare() throws Exception {
        if (studentMapper.countAll(null) == 0) {
            demoDataService.reset();
        }
        String admin = AuthTestSupport.login(mockMvc, objectMapper, "admin", "admin123");
        JsonNode leftovers = call(admin, get("/api/students?keyword=2099&size=100"), null);
        for (JsonNode row : leftovers.at("/data/list")) {
            call(admin, delete("/api/students/" + row.get("id").asLong()), null);
        }
        if (call(admin, get("/api/students?classId=高一-1&size=100"), null).at("/data/total").asLong() != 4) {
            demoDataService.reset();
        }
    }

    /** 携带指定身份发送请求，并返回解析后的 JSON 响应。 */
    private JsonNode call(String token, MockHttpServletRequestBuilder builder, String body) throws Exception {
        MockHttpServletRequestBuilder request = builder;
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    @Test
    @DisplayName("未登录访问业务接口返回 401 与统一错误码")
    void anonymousIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/students")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(40100);
        assertThat(body.get("message").asText()).contains("未登录");
    }

    @Test
    @DisplayName("登录：错误密码被拒绝，正确密码返回令牌与身份")
    void login() throws Exception {
        JsonNode wrong = call(null, post("/api/auth/login"),
                """
                        {"username": "admin", "password": "wrong-password"}
                        """);
        assertThat(wrong.get("code").asInt()).isEqualTo(40100);
        assertThat(wrong.get("message").asText()).contains("密码");

        JsonNode ok = call(null, post("/api/auth/login"),
                """
                        {"username": "teacher1", "password": "teacher123"}
                        """);
        assertThat(ok.get("code").asInt()).isZero();
        assertThat(ok.at("/data/token").asText()).isNotBlank();
        assertThat(ok.at("/data/expiresIn").asLong()).isGreaterThan(0);
        assertThat(ok.at("/data/user/role").asText()).isEqualTo("TEACHER");
        assertThat(ok.at("/data/user/roleLabel").asText()).isEqualTo("班主任");
        assertThat(ok.at("/data/user/classNames")).isNotEmpty();

        JsonNode paramMissing = call(null, post("/api/auth/login"), """
                {"username": "", "password": ""}
                """);
        assertThat(paramMissing.get("code").asInt()).isEqualTo(40001);
    }

    @Test
    @DisplayName("只读账号只能查询，任何写操作都被拒绝")
    void viewerIsReadOnly() throws Exception {
        String viewer = AuthTestSupport.login(mockMvc, objectMapper, "viewer", "viewer123");

        assertThat(call(viewer, get("/api/students?size=1"), null).get("code").asInt()).isZero();
        assertThat(call(viewer, get("/api/dashboard/overview"), null).get("code").asInt()).isZero();

        JsonNode forbiddenCreate = call(viewer, post("/api/students"), """
                {"name": "只读测试", "gender": "男", "sid": "20990601", "grade": "高一", "classNo": 1,
                 "status": "在读", "guardian": "家长", "guardianPhone": "13800001111"}
                """);
        assertThat(forbiddenCreate.get("code").asInt()).isEqualTo(40300);

        JsonNode forbiddenSettings = call(viewer, put("/api/settings"), """
                {"schoolName": "改不了"}
                """);
        assertThat(forbiddenSettings.get("code").asInt()).isEqualTo(40300);

        JsonNode forbiddenReset = call(viewer, post("/api/settings/reset-demo"), null);
        assertThat(forbiddenReset.get("code").asInt()).isEqualTo(40300);
    }

    @Test
    @DisplayName("班主任的数据范围限制在自己负责的班级")
    void teacherIsScopedToOwnClasses() throws Exception {
        String teacher = AuthTestSupport.login(mockMvc, objectMapper, "teacher1", "teacher123");

        JsonNode students = call(teacher, get("/api/students?size=100"), null);
        assertThat(students.at("/data/total").asInt()).isEqualTo(8); // teacher1 负责 2 个班 × 4 人
        List<String> classIds = students.at("/data/list").findValuesAsText("classId");
        assertThat(classIds).isNotEmpty().allMatch(id -> id.startsWith("高一-"));

        JsonNode classes = call(teacher, get("/api/classes"), null);
        assertThat(classes.at("/data").size()).isEqualTo(2);

        JsonNode overview = call(teacher, get("/api/dashboard/overview"), null);
        assertThat(overview.at("/data/metrics/totalStudents").asInt()).isEqualTo(8);
        assertThat(overview.at("/data/metrics/classCount").asInt()).isEqualTo(2);

        // 其他班级的名单查不到
        JsonNode otherClass = call(teacher, get("/api/students?classId=高三-1"), null);
        assertThat(otherClass.at("/data/total").asInt()).isZero();

        // 其他班级的学生详情也拿不到
        String admin = AuthTestSupport.login(mockMvc, objectMapper, "admin", "admin123");
        long outsideStudentId = call(admin, get("/api/students?classId=高三-1&size=1"), null)
                .at("/data/list").get(0).get("id").asLong();
        JsonNode detail = call(teacher, get("/api/students/" + outsideStudentId), null);
        assertThat(detail.get("code").asInt()).isEqualTo(40300);

        // 考勤标记：自己班可以，别班不行
        long ownStudentId = call(teacher, get("/api/students?size=1"), null).at("/data/list").get(0).get("id").asLong();
        String date = LocalDate.now().toString();
        JsonNode ownMark = call(teacher, put("/api/attendance"),
                """
                        {"studentId": %d, "date": "%s", "status": "出勤"}
                        """.formatted(ownStudentId, date));
        assertThat(ownMark.get("code").asInt()).isZero();

        JsonNode otherMark = call(teacher, put("/api/attendance"),
                """
                        {"studentId": %d, "date": "%s", "status": "出勤"}
                        """.formatted(outsideStudentId, date));
        assertThat(otherMark.get("code").asInt()).isEqualTo(40300);

        // 班主任可以改自己班学生，但不能删学生、不能改设置
        JsonNode self = call(teacher, get("/api/students?size=1"), null).at("/data/list").get(0);
        JsonNode update = call(teacher, put("/api/students/" + ownStudentId), """
                {"name": "%s", "gender": "%s", "sid": "%s", "grade": "%s", "classNo": %d,
                 "status": "在读", "guardian": "%s", "guardianPhone": "13800001111", "note": "班主任维护记录"}
                """.formatted(self.get("name").asText(), self.get("gender").asText(), self.get("sid").asText(),
                self.get("grade").asText(), self.get("classNo").asInt(), self.get("guardian").asText()));
        assertThat(update.get("code").asInt()).isZero();
        assertThat(update.at("/data/note").asText()).isEqualTo("班主任维护记录");

        JsonNode forbiddenDelete = call(teacher, delete("/api/students/" + ownStudentId), null);
        assertThat(forbiddenDelete.get("code").asInt()).isEqualTo(40300);

        JsonNode forbiddenSettings = call(teacher, put("/api/settings"), """
                {"schoolName": "班主任改不了"}
                """);
        assertThat(forbiddenSettings.get("code").asInt()).isEqualTo(40300);
    }

    @Test
    @DisplayName("账号列表只有管理员能看")
    void userListIsAdminOnly() throws Exception {
        String admin = AuthTestSupport.login(mockMvc, objectMapper, "admin", "admin123");
        JsonNode users = call(admin, get("/api/auth/users"), null);
        assertThat(users.get("code").asInt()).isZero();
        assertThat(users.at("/data").size()).isGreaterThanOrEqualTo(4);
        boolean hasAdmin = false;
        for (JsonNode user : users.at("/data")) {
            if ("admin".equals(user.get("username").asText())) {
                hasAdmin = true;
                assertThat(user.get("roleLabel").asText()).isEqualTo("系统管理员");
            }
        }
        assertThat(hasAdmin).isTrue();

        String teacher = AuthTestSupport.login(mockMvc, objectMapper, "teacher1", "teacher123");
        assertThat(call(teacher, get("/api/auth/users"), null).get("code").asInt()).isEqualTo(40300);
    }

    @Test
    @DisplayName("伪造或过期的令牌按未登录处理")
    void invalidToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asInt())
                .isEqualTo(40100);
    }
}
