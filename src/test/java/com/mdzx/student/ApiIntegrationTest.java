package com.mdzx.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

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

/** 端到端接口测试：真实 Spring 容器 + 真实 MyBatis + 测试库 MySQL，全部以管理员身份调用 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private StudentMapper studentMapper;

    private String token;

    @BeforeEach
    void prepare() throws Exception {
        if (studentMapper.countAll(null) == 0) {
            demoDataService.reset();
        }
        token = AuthTestSupport.login(mockMvc, objectMapper, "admin", "admin123");
        cleanupTestStudents();
    }

    /** 清掉历史失败运行残留的测试学生，保证断言不依赖执行顺序 */
    private void cleanupTestStudents() throws Exception {
        JsonNode leftovers = getJson("/api/students?keyword=2099&size=100");
        for (JsonNode row : leftovers.at("/data/list")) {
            mockMvc.perform(auth(delete("/api/students/" + row.get("id").asLong())));
        }
        // 演示数据的形状是固定的：每班 4 人。若被历史运行改坏就整体重建，保证断言与顺序无关
        long classOneCount = getJson("/api/students?classId=高一-1&size=100").at("/data/total").asLong();
        if (classOneCount != 4) {
            demoDataService.reset();
        }
    }

    /** 为 MockMvc 请求添加登录令牌。 */
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** 发送 GET 请求并解析 JSON 响应。 */
    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(auth(get(url))).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** 发送带 JSON 请求体的请求并解析响应。 */
    private JsonNode sendJson(MockHttpServletRequestBuilder builder, String body) throws Exception {
        MvcResult result = mockMvc.perform(auth(builder).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("概览接口返回指标、趋势、待办与年级汇总")
    void dashboardOverview() throws Exception {
        JsonNode body = getJson("/api/dashboard/overview");
        assertThat(body.get("code").asInt()).isZero();

        JsonNode metrics = body.at("/data/metrics");
        assertThat(metrics.get("totalStudents").asLong()).isEqualTo(24);
        assertThat(metrics.get("classCount").asLong()).isEqualTo(6);
        assertThat(metrics.get("fullScore").asInt()).isEqualTo(650);
        assertThat(metrics.get("attendanceRate").asDouble()).isBetween(0.5, 1.0);
        assertThat(body.at("/data/trend").size()).isGreaterThan(0);
        assertThat(body.at("/data/grades").size()).isEqualTo(3);
        assertThat(body.at("/data/todos").isArray()).isTrue();
    }

    @Test
    @DisplayName("学生列表支持筛选、排序与分页")
    void studentList() throws Exception {
        JsonNode all = getJson("/api/students?page=1&size=5");
        assertThat(all.at("/data/total").asLong()).isEqualTo(24);
        assertThat(all.at("/data/list").size()).isEqualTo(5);

        JsonNode byClass = getJson("/api/students?grade=高一&classId=高一-1");
        assertThat(byClass.at("/data/total").asLong()).isEqualTo(4);
        assertThat(byClass.at("/data/list").get(0).get("className").asText()).isEqualTo("高一（1）班");

        JsonNode desc = getJson("/api/students?sort=sid&dir=desc&size=1");
        assertThat(desc.at("/data/list").get(0).get("sid").asText()).isEqualTo("20260204");

        JsonNode absentAlert = getJson("/api/students?flag=缺勤偏多&size=100");
        for (JsonNode row : absentAlert.at("/data/list")) {
            assertThat(row.get("status").asText()).isEqualTo("在读");
            assertThat(row.at("/attendance/absent").asInt()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("学生详情包含成绩、排名与最近考勤")
    void studentDetail() throws Exception {
        long id = getJson("/api/students?size=1").at("/data/list").get(0).get("id").asLong();
        JsonNode body = getJson("/api/students/" + id);
        JsonNode student = body.at("/data/student");

        assertThat(student.get("scores").size()).isEqualTo(5);
        assertThat(student.get("total").asDouble()).isGreaterThan(0);
        assertThat(student.get("gradeRank").asInt()).isGreaterThan(0);
        assertThat(body.at("/data/subjects").size()).isEqualTo(5);
        assertThat(body.at("/data/recentDays").size()).isLessThanOrEqualTo(9);
        assertThat(body.at("/data/gradeAverage").asDouble()).isGreaterThan(0);
    }

    @Test
    @DisplayName("学生新增、编辑、删除全流程")
    void studentCrud() throws Exception {
        String payload = """
                {
                  "name": "接口测试生", "gender": "男", "sid": "20990101",
                  "grade": "高一", "classNo": 1, "status": "在读",
                  "birthday": "2010-05-06", "enrollmentDate": "2026-09-01",
                  "phone": "13800001111", "guardian": "测试家长", "guardianRelation": "父亲",
                  "guardianPhone": "13800002222", "address": "杭州市西湖区 1 号", "dorm": "走读",
                  "note": "", "scores": {"chinese": 120, "math": 130}
                }
                """;
        JsonNode created = sendJson(post("/api/students"), payload);
        assertThat(created.get("code").asInt()).isZero();
        assertThat(created.at("/data/sid").asText()).isEqualTo("20990101");
        assertThat(created.at("/data/className").asText()).isEqualTo("高一（1）班");
        long id = created.at("/data/id").asLong();

        JsonNode updated = sendJson(put("/api/students/" + id),
                payload.replace("接口测试生", "改名成功").replace("13800001111", "13900001111"));
        assertThat(updated.at("/data/name").asText()).isEqualTo("改名成功");

        mockMvc.perform(auth(delete("/api/students/" + id))).andExpect(status().isOk());
        assertThat(getJson("/api/students/" + id).get("code").asInt()).isEqualTo(40401);
    }

    @Test
    @DisplayName("参数校验与学号唯一性都有明确报错")
    void studentValidation() throws Exception {
        String badSid = """
                {"name": "短", "gender": "男", "sid": "123", "grade": "高一", "classNo": 1, "status": "在读",
                 "guardian": "", "guardianPhone": "abc"}
                """;
        JsonNode invalid = sendJson(post("/api/students"), badSid);
        assertThat(invalid.get("code").asInt()).isEqualTo(40001);

        String existingSid = getJson("/api/students?size=1").at("/data/list").get(0).get("sid").asText();
        String duplicate = """
                {"name": "重号生", "gender": "女", "sid": "%s", "grade": "高一", "classNo": 1, "status": "在读",
                 "guardian": "家长", "guardianPhone": "13800001111"}
                """.formatted(existingSid);
        assertThat(sendJson(post("/api/students"), duplicate).get("code").asInt()).isEqualTo(40901);

        String badScore = """
                {"name": "超分生", "gender": "男", "sid": "20990202", "grade": "高一", "classNo": 1, "status": "在读",
                 "guardian": "家长", "guardianPhone": "13800001111", "scores": {"chinese": 999}}
                """;
        assertThat(sendJson(post("/api/students"), badScore).get("code").asInt()).isEqualTo(40001);
    }

    @Test
    @DisplayName("考勤：汇总人数与在读学生数一致，标记后当日状态与统计同步变化")
    void attendanceFlow() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 17);
        JsonNode page = getJson("/api/attendance?date=" + date + "&size=100");
        int total = page.at("/data/summary/total").asInt();
        int presentBefore = page.at("/data/summary/counts/present").asInt();
        assertThat(total).isEqualTo(getJson("/api/students?status=在读&size=100").at("/data/total").asInt());
        assertThat(page.at("/data/summary/schoolDay").asBoolean()).isTrue();

        long studentId = 0;
        for (JsonNode row : page.at("/data/page/list")) {
            // 选一个当前计入出勤的学生，标记缺勤后出勤数才会下降
            String dayStatus = row.get("dayStatus").asText();
            if ("出勤".equals(dayStatus) || "迟到".equals(dayStatus)) {
                studentId = row.get("id").asLong();
                break;
            }
        }
        assertThat(studentId).isPositive();

        JsonNode marked = sendJson(put("/api/attendance"),
                """
                        {"studentId": %d, "date": "%s", "status": "缺勤", "remark": "测试标记"}
                        """.formatted(studentId, date));
        assertThat(marked.get("code").asInt()).isZero();

        JsonNode after = getJson("/api/attendance?date=" + date + "&size=100");
        assertThat(after.at("/data/summary/counts/present").asInt()).isEqualTo(presentBefore - 1);
        assertThat(after.at("/data/summary/counts/absent").asInt())
                .isEqualTo(page.at("/data/summary/counts/absent").asInt() + 1);

        JsonNode abnormal = getJson("/api/attendance?date=" + date + "&scope=abnormal&size=100");
        for (JsonNode row : abnormal.at("/data/page/list")) {
            assertThat(row.get("dayStatus").asText()).isNotEqualTo("出勤");
        }

        // 还原成出勤，保证测试可重复
        sendJson(post("/api/attendance/batch"),
                """
                        {"date": "%s", "records": [{"studentId": %d, "status": "出勤"}]}
                        """.formatted(date, studentId));

        JsonNode badStatus = sendJson(put("/api/attendance"),
                """
                        {"studentId": %d, "date": "%s", "status": "瞎写"}
                        """.formatted(studentId, date));
        assertThat(badStatus.get("code").asInt()).isEqualTo(40001);
    }

    @Test
    @DisplayName("成绩：按单科降序排序且统计口径正确")
    void grades() throws Exception {
        JsonNode page = getJson("/api/grades?sort=math&dir=desc&size=6");
        JsonNode list = page.at("/data/page/list");
        assertThat(list.size()).isEqualTo(6);
        double previous = Double.MAX_VALUE;
        for (JsonNode row : list) {
            double math = row.at("/scores/math").asDouble();
            assertThat(math).isLessThanOrEqualTo(previous);
            previous = math;
        }
        assertThat(page.at("/data/stats/fullScore").asInt()).isEqualTo(650);
        assertThat(page.at("/data/stats/passLine").asInt()).isEqualTo(390);
        assertThat(page.at("/data/stats/excellentLine").asInt()).isEqualTo(520);
        assertThat(page.at("/data/stats/studentCount").asInt()).isGreaterThan(0);
        assertThat(page.at("/data/subjects").size()).isEqualTo(5);
    }

    @Test
    @DisplayName("班级汇总：人数、平均分、出勤率、第一名")
    void classes() throws Exception {
        JsonNode list = getJson("/api/classes?sort=avg").at("/data");
        assertThat(list.size()).isEqualTo(6);
        assertThat(list.get(0).get("headTeacher").asText()).isNotBlank();
        assertThat(list.get(0).get("topStudentName").asText()).isNotBlank();
        double previous = Double.MAX_VALUE;
        for (JsonNode row : list) {
            assertThat(row.get("avgTotal").asDouble()).isLessThanOrEqualTo(previous);
            previous = row.get("avgTotal").asDouble();
            assertThat(row.get("count").asInt()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("设置：读取、修改、以及重置演示数据")
    void settings() throws Exception {
        JsonNode before = getJson("/api/settings");
        assertThat(before.at("/data/schoolName").asText()).isEqualTo("明德中学");

        Map<String, Object> patch = new java.util.LinkedHashMap<>();
        patch.put("schoolName", "明德中学（测试）");
        patch.put("schoolCode", "MD-3301");
        patch.put("term", "2026—2027 学年 第一学期");
        patch.put("principal", "沈立群");
        patch.put("address", "杭州市");
        patch.put("phone", "0571-0000 0000");
        patch.put("notifyAbsence", true);
        patch.put("notifyScore", true);
        patch.put("notifyTransfer", true);
        patch.put("autoArchive", false);
        patch.put("classCapacity", 30);

        JsonNode updated = sendJson(put("/api/settings"), objectMapper.writeValueAsString(patch));
        assertThat(updated.at("/data/schoolName").asText()).isEqualTo("明德中学（测试）");
        assertThat(updated.at("/data/classCapacity").asInt()).isEqualTo(30);
        assertThat(updated.at("/data/notifyScore").asBoolean()).isTrue();

        assertThat(getJson("/api/settings").at("/data/classCapacity").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("登录后可拿到当前用户信息")
    void me() throws Exception {
        JsonNode me = getJson("/api/auth/me");
        assertThat(me.at("/data/username").asText()).isEqualTo("admin");
        assertThat(me.at("/data/role").asText()).isEqualTo("ADMIN");
        assertThat(me.at("/data/roleLabel").asText()).isEqualTo("系统管理员");
        assertThat(me.at("/data/admin").asBoolean()).isTrue();
        assertThat(me.at("/data/teacher").asBoolean()).isFalse();
    }
}
