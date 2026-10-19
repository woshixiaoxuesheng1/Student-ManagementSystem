# 接口文档

基础地址：`http://127.0.0.1:8080`

统一响应：`{ "code": 0, "message": "ok", "data": ... }`，`code = 0` 表示成功。

**除 `POST /api/auth/login` 外，所有接口都需要请求头 `Authorization: Bearer <token>`。**

| code | 含义 | HTTP |
| --- | --- | --- |
| 0 | 成功 | 200 |
| 40001 | 参数校验失败 | 400 |
| 40100 | 未登录 / 令牌过期 | 401 |
| 40300 | 权限不足（角色不允许，或数据不在你的范围内） | 403 |
| 40401 | 资源不存在 | 200（业务错误） |
| 40901 | 学号重复 | 200（业务错误） |
| 50000 | 服务器内部错误 | 500（详情看服务端日志） |

## 0. 登录与账号

### POST /api/auth/login — 登录（无需令牌）

```json
{ "username": "teacher1", "password": "teacher123" }
```

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 43200,
    "user": {
      "id": 2, "username": "teacher1", "realName": "陆敏华",
      "role": "TEACHER", "roleLabel": "班主任",
      "admin": false, "teacher": true, "viewer": false,
      "classIds": ["高一-1", "高一-2"],
      "classNames": ["高一（1）班", "高一（2）班"]
    }
  }
}
```

登录名或密码错误返回 `40100`；账号被停用返回 `40300`。

### GET /api/auth/me — 当前登录人

返回与登录响应中 `user` 相同的结构，用于刷新页面后恢复身份。

### POST /api/auth/logout — 退出

无状态令牌，服务端仅作应答，客户端丢弃令牌即可。

### GET /api/auth/users — 账号列表（仅 `ADMIN`）

```json
{ "code": 0, "data": [
  { "id": 2, "username": "teacher1", "realName": "陆敏华", "role": "TEACHER",
    "roleLabel": "班主任", "phone": "13800001001", "enabled": true,
    "lastLoginAt": "2026-09-18 09:40:12", "classNames": ["高一（1）班", "高一（2）班"] }
] }
```

### 权限速查

| 接口 | ADMIN | TEACHER | VIEWER |
| --- | --- | --- | --- |
| `GET /api/students`、`/api/students/{id}`、`/api/dashboard/*`、`/api/classes`、`/api/grades`、`/api/attendance` | 全校 | 仅自己负责班级 | 全校 |
| `POST /api/students`、`PUT /api/students/{id}` | ✅ | ✅（限本班） | ❌ |
| `DELETE /api/students/**` | ✅ | ❌ | ❌ |
| `PUT /api/attendance`、`POST /api/attendance/batch` | ✅ | ✅（限本班） | ❌ |
| `GET /api/settings` | ✅ | ✅ | ✅ |
| `PUT /api/settings`、`POST /api/settings/reset-demo` | ✅ | ❌ | ❌ |
| `GET /api/auth/users` | ✅ | ❌ | ❌ |

## 1. 学生

### GET /api/students — 列表

| 参数 | 说明 | 默认 |
| --- | --- | --- |
| keyword | 姓名 / 学号 / 手机号 / 监护人 / 班级名 模糊搜索 | — |
| grade | `高一`｜`高二`｜`高三`｜`全部` | 全部 |
| classId | 班级编码，如 `高一-1` | 全部 |
| status | 学籍状态 | 全部 |
| flag | 传 `缺勤偏多` 时只返回在读且缺勤≥2 的学生 | — |
| incomplete | `true` 时只返回监护人手机号缺失的学生 | — |
| sort | `sid`｜`name`｜`classId`｜`total` | sid |
| dir | `asc`｜`desc` | asc |
| page / size | 页码 / 每页条数（上限 100） | 1 / 10 |

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 481, "sid": "20240101", "name": "张梦楠", "gender": "女",
        "grade": "高一", "gradeIndex": 0, "classId": "高一-1", "className": "高一（1）班", "classNo": 1,
        "status": "在读", "phone": "13800001111", "guardian": "张建国", "guardianRelation": "父亲",
        "guardianPhone": "13800002222", "birthday": "2010-03-12", "enrollmentDate": "2026-09-01",
        "address": "杭州市西湖区文三路 12 号", "dorm": "2 号楼 301 室", "note": "",
        "total": 512.0, "gradeRank": 37,
        "scores": { "chinese": 118.0, "math": 132.0, "english": 121.0, "physics": 76.0, "chemistry": 65.0 },
        "attendance": { "present": 52, "late": 7, "absent": 1, "leave": 2, "termDays": 62, "rate": 0.9516 }
      }
    ],
    "total": 288, "page": 1, "size": 10, "pages": 29
  }
}
```

### GET /api/students/{id} — 详情

在列表字段基础上额外返回：

- `gradeAverage`：所在年级的平均总分
- `recentDays`：最近 9 个教学日 `[{ "date": "2026-09-18", "status": "出勤" }]`
- `subjects`：学科配置

### GET /api/students/options — 下拉选项

```json
{ "grades": ["高一", "高二", "高三"],
  "classes": [{ "classId": "高一-1", "className": "高一（1）班", "grade": "高一",
                "gradeIndex": 0, "classNo": 1, "headTeacher": "陆敏华", "capacity": 28 }],
  "statuses": ["在读", "休学", "转学", "退学"],
  "subjects": [{ "key": "chinese", "name": "语文", "fullScore": 150, "sortNo": 1 }] }
```

### POST /api/students — 新增

```json
{
  "name": "新同学", "gender": "男", "sid": "20260599",
  "grade": "高一", "classNo": 3, "status": "在读",
  "birthday": "2010-05-06", "enrollmentDate": "2026-09-01",
  "phone": "13800001111", "guardian": "家长姓名", "guardianRelation": "父亲",
  "guardianPhone": "13800002222", "address": "杭州市西湖区 1 号", "dorm": "走读",
  "note": "", "scores": { "chinese": 120, "math": 130 }
}
```

校验规则：姓名 2–32 字、性别 `男/女`、学号 8 位数字且全库唯一、班号 1–20、
手机号需匹配 `1[3-9]xxxxxxxxx`（可为空）、监护人必填、成绩需在 0 到该科满分之间。
`scores` 可以只传部分学科。

### PUT /api/students/{id} — 编辑

请求体同新增。

### DELETE /api/students/{id} — 删除

级联删除该学生的成绩与考勤记录。

### DELETE /api/students — 批量删除

```json
{ "ids": [12, 13, 14] }
```

返回删除条数。

## 2. 概览

### GET /api/dashboard/overview?date=2026-09-18

`date` 可省略，默认今天；出勤率按「最近教学日」计算（周末自动向前找工作日）。

```json
{
  "code": 0,
  "data": {
    "schoolName": "明德中学",
    "term": "2026—2027 学年 第一学期",
    "generatedAt": "2026-09-18",
    "metrics": {
      "totalStudents": 288, "activeStudents": 276, "suspendedStudents": 2,
      "classCount": 12, "avgClassSize": 24.0,
      "attendanceDate": "2026-09-18", "attendanceRate": 0.946,
      "attendanceCounts": { "present": 255, "late": 6, "absent": 12, "leave": 3 },
      "avgTotal": 469.5, "bestTotal": 620.0, "fullScore": 650, "deltaVsLastWeek": -0.0047
    },
    "trend": [{ "weekNo": 8, "label": "第 8 周", "startDate": "2026-09-14", "rate": 0.972 }],
    "todos": [{
      "id": "todo-suspend", "title": "休学学生到期复核", "detail": "核对休学期限与复学材料",
      "count": 2, "tone": "warn", "actionLabel": "查看名单", "actionQuery": { "status": "休学" }
    }],
    "grades": [{ "grade": "高一", "classCount": 4, "totalStudents": 96, "activeStudents": 93,
                 "avgTotal": 473.4, "attendanceRate": 0.9612 }],
    "recentMoves": [{ "id": 12, "sid": "20260311", "name": "张梦楠", "grade": "高一",
                      "className": "高一（3）班", "status": "休学", "attendanceRate": 0.34 }]
  }
}
```

`tone` 取值：`warn`｜`danger`｜`info`｜`neutral`。`actionQuery` 可直接拼成前端筛选链接，例如
`/students?status=休学`、`/students?flag=缺勤偏多`、`/students?incomplete=1`。

## 3. 班级

### GET /api/classes?grade=高一&sort=avg

`sort`：`grade`（默认，按年级班号）｜`avg`｜`count`｜`rate`

```json
{ "code": 0, "data": [{
  "classId": "高一-1", "className": "高一（1）班", "grade": "高一", "gradeIndex": 0, "classNo": 1,
  "headTeacher": "陆敏华", "capacity": 28,
  "count": 24, "activeCount": 23, "avgTotal": 486.3, "attendanceRate": 0.9581,
  "topStudentName": "项亦然", "topStudentTotal": 602.0 }] }
```

## 4. 成绩

### GET /api/grades?grade=&classId=&keyword=&sort=math&dir=desc&page=1&size=20

`sort` 传 `total` 或学科编码（`chinese` / `math` / `english` / `physics` / `chemistry`）。
统计口径为筛选范围内的在读学生。

```json
{
  "code": 0,
  "data": {
    "page": {
      "list": [{
        "id": 1, "sid": "20260101", "name": "吕亦然", "gender": "男", "grade": "高一",
        "classId": "高一-1", "className": "高一（1）班", "status": "在读",
        "total": 588.0, "gradeRank": 3,
        "scores": { "chinese": 132.0, "math": 148.0, "english": 139.0, "physics": 87.0, "chemistry": 82.0 }
      }],
      "total": 276, "page": 1, "size": 20, "pages": 14
    },
    "stats": {
      "studentCount": 276, "avgTotal": 469.5, "bestTotal": 620.0,
      "bestStudentName": "项亦然", "bestClassName": "高一（2）班",
      "fullScore": 650, "passLine": 390, "excellentLine": 520,
      "passRate": 0.9565, "excellentRate": 0.1812
    },
    "subjects": [{ "key": "chinese", "name": "语文", "fullScore": 150, "sortNo": 1 }]
  }
}
```

### GET /api/grades/subjects — 学科配置

返回 `[{ key, name, fullScore, sortNo }]`。

## 5. 考勤

### GET /api/attendance?date=2026-09-18&scope=all&grade=&classId=&keyword=&page=1&size=15

`scope`：`all`（默认）｜`abnormal`（只看非出勤的学生）。

```json
{
  "code": 0,
  "data": {
    "summary": {
      "date": "2026-09-18", "schoolDay": true, "total": 276,
      "counts": { "present": 255, "late": 6, "absent": 12, "leave": 3 },
      "attendanceRate": 0.9457, "markedCount": 276
    },
    "page": {
      "list": [{
        "id": 481, "sid": "20240101", "name": "张梦楠", "gender": "女", "grade": "高一",
        "classId": "高一-1", "className": "高一（1）班", "dayStatus": "出勤",
        "term": { "present": 52, "late": 7, "absent": 1, "leave": 2, "termDays": 62, "rate": 0.9516 }
      }],
      "total": 276, "page": 1, "size": 15, "pages": 19
    },
    "recentSchoolDays": ["2026-09-14", "2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18"]
  }
}
```

未标记的学生 `dayStatus` 返回「出勤」并计入汇总；`markedCount` 是当天已有记录的人数。

### PUT /api/attendance — 单人标记

同一学生同一天重复提交为覆盖更新。

```json
{ "studentId": 481, "date": "2026-09-18", "status": "请假", "remark": "家长电话请假" }
```

### POST /api/attendance/batch — 批量标记

```json
{ "date": "2026-09-18",
  "records": [{ "studentId": 481, "status": "出勤" }, { "studentId": 482, "status": "迟到" }] }
```

`status` 只接受 `出勤`｜`迟到`｜`缺勤`｜`请假`，否则返回 40001。

## 6. 设置

### GET /api/settings

```json
{ "code": 0, "data": {
  "schoolName": "明德中学", "schoolCode": "MD-3301", "term": "2026—2027 学年 第一学期",
  "principal": "沈立群", "address": "浙江省杭州市文三路 118 号", "phone": "0571-8812 6600",
  "notifyAbsence": true, "notifyScore": false, "notifyTransfer": true, "autoArchive": true,
  "classCapacity": 28, "updatedAt": "2026-09-18 08:25:11" } }
```

### PUT /api/settings — 修改

请求体字段同 GET（`updatedAt` 除外）。`schoolName` 必填；传 `classCapacity` 时会同时更新所有班级的容量。
未传的开关字段保持原值。

### POST /api/settings/reset-demo — 重置演示数据

清空学籍、成绩、考勤后按规则重新生成。

```json
{ "code": 0, "data": { "students": 288, "scores": 1440, "attendanceRecords": 17586, "schoolDays": 62 } }
```
