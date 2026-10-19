# 学生管理系统 · 后端服务

配合前端项目 `../student-management` 使用的后端：Spring Boot 3.5 + MyBatis 3 + MySQL 8，Java 17 编译。
接口按前端页面的数据形状设计。**前端已经接入本服务**：学籍、班级、成绩、考勤、设置全部读写 MySQL，不再使用 localStorage。

## 认证与权限

无状态 JWT：登录换取令牌，之后每个请求带 `Authorization: Bearer <token>`。
令牌里只放用户 id，角色与负责班级每次请求从库里读，改权限立即生效。

| 角色 | 数据范围 | 写操作 |
| --- | --- | --- |
| `ADMIN` 系统管理员 | 全校 | 学籍增删改、考勤标记、系统设置、重置数据、查看账号列表 |
| `TEACHER` 班主任 | 仅自己负责的班级（`sys_user_class` 配置） | 本班学籍可改、本班考勤可标记；不能删除学生、不能改设置 |
| `VIEWER` 只读账号 | 全校 | 全部禁止 |

演示账号（首次启动 `sys_user` 为空时自动创建，密码用 BCrypt 存储）：

| 账号 | 密码 | 角色 | 负责班级 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 系统管理员 | 全校 |
| `teacher1` | `teacher123` | 班主任 | 高一（1）班、高一（2）班 |
| `teacher2` | `teacher123` | 班主任 | 高三（1）班、高三（2）班 |
| `viewer` | `viewer123` | 只读账号 | 全校只读 |

实现位置：

- `security/SecurityConfig`：过滤器链（除 `/api/auth/login` 外全部需要令牌）、BCrypt、401/403 统一响应
- `security/JwtTokenProvider` + `JwtAuthenticationFilter`：签发与校验令牌，每次请求按当前库状态恢复登录态
- `security/AuthContext`：业务层取当前用户与数据范围（`scopedClassIds()` 班主任返回自己的班级，其他角色返回 null 表示不限）
- 控制器用 `@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")` 这类注解控制写操作
- Mapper 里的 `scope` 片段把班级范围拼进 SQL，班主任查不到别班数据

生产环境请用环境变量覆盖密钥：`AUTH_JWT_SECRET=<随机长字符串>`；有效期由 `auth.jwt.expire-hours` 控制（默认 12 小时）。

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 运行环境 | JDK 17+（本项目在 JDK 25 上编译、启动、跑测试均通过） |
| 框架 | Spring Boot 3.5.0（Web / Validation） |
| 持久层 | MyBatis 3.5 + XML Mapper（动态 SQL、批量 upsert） |
| 数据库 | MySQL 8.0（窗口函数、CTE、CHECK 约束） |
| 测试 | JUnit 5 + Spring Boot Test + MockMvc（连真实 MySQL 测试库） |
| 构建 | Maven 3.9 |

## 快速开始

```bash
# 1. 建库建表 + 基础数据（脚本可重复执行）
mysql -u root -p < db/schema.sql

# 2. 按需修改数据库账号，默认 root/123456
#    src/main/resources/application.yml

# 3. 编译打包并启动（启动时若 student 表为空会自动生成演示数据）
mvn clean package -DskipTests
java -jar target/student-management-api-1.0.0.jar
```

服务默认监听 `http://127.0.0.1:8080`，前端开发服务器（5178）与预览端口（4173）已在 `WebConfig` 中放行跨域。

跑测试（需要一个 `student_management_test` 库，测试会自己建表）：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS student_management_test DEFAULT CHARACTER SET utf8mb4;"
mvn test
```

## 目录结构

```
student-management-api/
├─ db/schema.sql                   建库脚本：表结构 + 学科/班级/学校信息
├─ docs/API.md                     接口文档（含请求响应示例）
├─ src/main/java/com/mdzx/student/
│  ├─ StudentManagementApplication.java
│  ├─ common/                      统一响应、分页、异常、教学日工具
│  ├─ config/WebConfig.java        跨域配置
│  ├─ controller/                  6 个 REST 控制器
│  ├─ service/                     业务接口 + impl 实现
│  ├─ service/support/             派生值组装（出勤率等口径集中在这里）
│  ├─ mapper/                      MyBatis 接口
│  ├─ mapper/model/                查询结果行对象
│  ├─ domain/                      表对应的实体
│  ├─ dto/                         入参（record + 校验注解）
│  └─ vo/                          出参（record）
├─ src/main/resources/
│  ├─ application.yml
│  └─ mapper/*.xml                 5 个 Mapper XML
└─ src/test/                       集成测试 + 单元测试（含独立测试库配置）
```

## 数据模型

```
class_info ──< student ──< student_score >── subject
                  └──< attendance_record
school_setting（单行配置）
```

- **student** 只存学籍本身：年级、班号、班主任都在 `class_info`，避免冗余字段不一致。
- **student_score** 按「学生 + 学科」一行，新增学科只需往 `subject` 插一行，不改表结构。
- **attendance_record** 每人每天一条，`uk_attendance_student_date` 保证幂等，人工标记就是 upsert。
- 删除学生会级联清理成绩与考勤（`ON DELETE CASCADE`）。
- **派生值不落库**：总分、年级排名（`RANK() OVER`）、班级平均分、出勤率、待办数量全部现算，避免缓存与源数据不一致。

## 演示数据

启动时如果 `student` 表为空，`DemoDataService` 会自动生成：

- 3 个年级 × 4 个班 × 24 人 = 288 名学生（`demo.students-per-class` 可调）
- 每人 5 科成绩（按能力值 + 抖动生成）
- 最近 62 个教学日的考勤记录（约 1.7 万条）

生成规则与前端 `seed.ts` 一致：同一套姓氏/名字池、同一套 mulberry32 确定性随机，因此每次生成的数据都可复现。
考勤状态由「学生 + 日期」散列决定（缺勤 0.6% / 请假 3.5% / 迟到 2.9% / 出勤约 93%），休学学生后段记为请假，
转学、退学学生只统计在校期间。

`POST /api/settings/reset-demo` 可随时重建演示数据（对应前端设置页的「重置演示数据」）。

## 与前端对接（已完成）

前端通过 `src/api/` 这一层访问本服务，`src/store/AppStore.tsx` 负责首屏加载与写操作后刷新：

| 前端动作 | 调用 |
| --- | --- |
| 首屏初始化 | `GET /api/students` + `GET /api/classes` + `GET /api/students/options` + `GET /api/settings` |
| 新增学籍 | `POST /api/students` |
| 编辑学籍 | `PUT /api/students/{id}` |
| 删除学籍 | `DELETE /api/students/{id}`、批量 `DELETE /api/students` |
| 学生详情抽屉 | `GET /api/students/{id}` |
| 概览页 | `GET /api/dashboard/overview` |
| 考勤页与标记 | `GET /api/attendance`、`PUT /api/attendance`、`POST /api/attendance/batch` |
| 设置保存 / 重置 | `PUT /api/settings`、`POST /api/settings/reset-demo` |

字段对应（后端 → 前端）：

- `gradeRank` 对应前端 `rank`，`total` 同名同义
- `attendance: {present, late, absent, leave, termDays, rate}` 对应前端中文键 `{出勤, 迟到, 缺勤, 请假}`
- 其余字段（`sid/name/gender/grade/classId/className/classNo/status/guardian*/birthday/enrollmentDate/address/dorm/note`）同名同义
- 统一响应 `{ code, message, data }`，`code = 0` 表示成功
- 日期一律 `YYYY-MM-DD`；`flag=缺勤偏多`、`status=休学` 等取值与前端筛选参数一致
- 前端用 `VITE_API_BASE` 覆盖接口地址，默认 `http://127.0.0.1:8080`

## 已实现能力

- **学生**：多条件 + 关键词查询、白名单排序（学号/姓名/班级/总分）、分页、详情（档案 + 成绩 + 排名 + 近 9 个教学日考勤）、新增、编辑、单个与批量删除、下拉选项
- **概览**：关键指标、近 8 周出勤趋势、4 类待办、年级汇总、近期学籍异动
- **班级**：人数、平均总分、出勤率、第一名，支持按年级筛选与四种排序
- **成绩**：按总分或任一单科排序、及格率/优秀率统计、学科配置
- **考勤**：按日汇总与逐人列表、单人标记、批量标记、仅看异常、教学日判断
- **设置**：学校信息读写、班级人数上限（同步更新各班容量）、演示数据重置
- **工程**：统一响应与全局异常处理、参数校验、事务、批量 upsert、跨域配置

## 约定与取舍

- 学籍状态、考勤状态直接用中文取值（`在读/休学/转学/退学`、`出勤/迟到/缺勤/请假`），与前端展示和 CSV 导出保持一致，不做二次映射。
- 未标记考勤的学生按「出勤」计入当日汇总（与前端默认口径一致），已标记的以记录为准。
- 姓名排序使用 MySQL 默认排序规则（按编码序）。若要按拼音排序，可增加拼音列或使用拼音排序规则。
- 学生、成绩和考勤列表统一使用 PageHelper 分页与自动总数统计；排序字段用 `<choose>` 白名单，不拼接用户输入。接口继续返回 `list/total/page/size/pages`，单页大小上限为 100。
- DTO/VO 使用 Java record（不可变、自带访问器），实体与查询行对象使用常规 POJO 以配合 MyBatis 自动映射。
