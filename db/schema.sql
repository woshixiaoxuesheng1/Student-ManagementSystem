-- =============================================================
-- 学生管理系统 · 数据库结构（MySQL 8.0）
-- 执行：mysql -u root -p < db/schema.sql
-- 脚本可重复执行：DDL 使用 IF NOT EXISTS，基础数据使用 upsert
-- =============================================================
CREATE DATABASE IF NOT EXISTS student_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE student_management;

-- 学科配置：满分与顺序由数据库维护，前端不再写死
CREATE TABLE IF NOT EXISTS subject (
  subject_key  VARCHAR(16)  NOT NULL COMMENT '学科编码',
  subject_name VARCHAR(16)  NOT NULL COMMENT '学科名称',
  full_score   INT          NOT NULL COMMENT '满分',
  sort_no      INT          NOT NULL COMMENT '显示顺序',
  PRIMARY KEY (subject_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学科配置';

-- 教学班：班主任与班额上限属于教务配置
CREATE TABLE IF NOT EXISTS class_info (
  class_id     VARCHAR(32) NOT NULL COMMENT '班级编码，如 高一-1',
  class_name   VARCHAR(64) NOT NULL COMMENT '班级名称，如 高一（1）班',
  grade        VARCHAR(16) NOT NULL COMMENT '年级',
  grade_index  INT         NOT NULL COMMENT '年级序号，用于排序',
  class_no     INT         NOT NULL COMMENT '班号',
  head_teacher VARCHAR(32)          COMMENT '班主任',
  capacity     INT         NOT NULL DEFAULT 28 COMMENT '人数上限',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (class_id),
  UNIQUE KEY uk_class_grade_no (grade, class_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学班';

-- 学校信息（单行配置，id 固定为 1）
CREATE TABLE IF NOT EXISTS school_setting (
  id              TINYINT      NOT NULL DEFAULT 1,
  school_name     VARCHAR(64)  NOT NULL,
  school_code     VARCHAR(32)  NOT NULL,
  term            VARCHAR(64)  NOT NULL,
  principal       VARCHAR(32)  NOT NULL,
  address         VARCHAR(128) NOT NULL,
  phone           VARCHAR(32)  NOT NULL,
  notify_absence  TINYINT(1)   NOT NULL DEFAULT 1,
  notify_score    TINYINT(1)   NOT NULL DEFAULT 0,
  notify_transfer TINYINT(1)   NOT NULL DEFAULT 1,
  auto_archive    TINYINT(1)   NOT NULL DEFAULT 1,
  class_capacity  INT          NOT NULL DEFAULT 28,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT chk_setting_single_row CHECK (id = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学校信息与通知设置';

-- 学籍档案
CREATE TABLE IF NOT EXISTS student (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  sid               CHAR(8)      NOT NULL COMMENT '学号',
  name              VARCHAR(32)  NOT NULL COMMENT '姓名',
  gender            VARCHAR(4)   NOT NULL COMMENT '性别',
  class_id          VARCHAR(32)  NOT NULL COMMENT '所属班级',
  status            VARCHAR(8)   NOT NULL DEFAULT '在读' COMMENT '学籍状态',
  phone             VARCHAR(20)           COMMENT '学生手机号',
  guardian          VARCHAR(32)           COMMENT '监护人',
  guardian_relation VARCHAR(16)           COMMENT '与监护人关系',
  guardian_phone    VARCHAR(20)           COMMENT '监护人手机号',
  birthday          DATE                  COMMENT '出生日期',
  enrollment_date   DATE                  COMMENT '入学日期',
  address           VARCHAR(128)          COMMENT '家庭住址',
  dorm              VARCHAR(32)           COMMENT '住宿情况',
  note              VARCHAR(255)          COMMENT '异动说明',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_sid (sid),
  KEY idx_student_class (class_id),
  KEY idx_student_status (status),
  KEY idx_student_name (name),
  KEY idx_student_guardian_phone (guardian_phone),
  CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES class_info (class_id),
  CONSTRAINT chk_student_gender CHECK (gender IN ('男', '女')),
  CONSTRAINT chk_student_status CHECK (status IN ('在读', '休学', '转学', '退学'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学籍档案';

-- 成绩（按学科一行，便于扩展科目）
CREATE TABLE IF NOT EXISTS student_score (
  student_id  BIGINT       NOT NULL,
  subject_key VARCHAR(16)  NOT NULL,
  score       DECIMAL(5,1) NOT NULL DEFAULT 0,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id, subject_key),
  KEY idx_score_subject (subject_key),
  CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
  CONSTRAINT fk_score_subject FOREIGN KEY (subject_key) REFERENCES subject (subject_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生成绩';

-- 考勤：每人每天一条，人工标记即写入/更新该行
CREATE TABLE IF NOT EXISTS attendance_record (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  student_id  BIGINT      NOT NULL,
  attend_date DATE        NOT NULL,
  status      VARCHAR(8)  NOT NULL COMMENT '出勤/迟到/缺勤/请假',
  remark      VARCHAR(64)          COMMENT '备注',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_student_date (student_id, attend_date),
  KEY idx_attendance_date (attend_date),
  KEY idx_attendance_student_status (student_id, status),
  CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
  CONSTRAINT chk_attendance_status CHECK (status IN ('出勤', '迟到', '缺勤', '请假'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考勤记录';

-- 系统账号：角色决定权限范围（ADMIN 全校管理 / TEACHER 本班管理 / VIEWER 全校只读）
CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(32)  NOT NULL COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
  real_name     VARCHAR(32)  NOT NULL COMMENT '姓名',
  role          VARCHAR(16)  NOT NULL COMMENT 'ADMIN/TEACHER/VIEWER',
  phone         VARCHAR(20)           COMMENT '联系电话',
  enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  last_login_at DATETIME              COMMENT '最近登录时间',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  CONSTRAINT chk_sys_user_role CHECK (role IN ('ADMIN', 'TEACHER', 'VIEWER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统账号';

-- 班主任负责的班级：TEACHER 只能访问这里配置的班级数据
CREATE TABLE IF NOT EXISTS sys_user_class (
  user_id  BIGINT      NOT NULL,
  class_id VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, class_id),
  CONSTRAINT fk_user_class_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_class_class FOREIGN KEY (class_id) REFERENCES class_info (class_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账号负责的班级';

-- ---------------- 基础数据 ----------------
INSERT INTO subject (subject_key, subject_name, full_score, sort_no) VALUES
  ('chinese',   '语文', 150, 1),
  ('math',      '数学', 150, 2),
  ('english',   '英语', 150, 3),
  ('physics',   '物理', 100, 4),
  ('chemistry', '化学', 100, 5)
ON DUPLICATE KEY UPDATE
  subject_name = VALUES(subject_name),
  full_score   = VALUES(full_score),
  sort_no      = VALUES(sort_no);

INSERT INTO class_info (class_id, class_name, grade, grade_index, class_no, head_teacher, capacity) VALUES
  ('高一-1', '高一（1）班', '高一', 0, 1, '陆敏华', 28),
  ('高一-2', '高一（2）班', '高一', 0, 2, '周立群', 28),
  ('高一-3', '高一（3）班', '高一', 0, 3, '方静文', 28),
  ('高一-4', '高一（4）班', '高一', 0, 4, '万俊峰', 28),
  ('高二-1', '高二（1）班', '高二', 1, 1, '曹亦菲', 28),
  ('高二-2', '高二（2）班', '高二', 1, 2, '韩昱',   28),
  ('高二-3', '高二（3）班', '高二', 1, 3, '陆定一', 28),
  ('高二-4', '高二（4）班', '高二', 1, 4, '史晓芸', 28),
  ('高三-1', '高三（1）班', '高三', 2, 1, '祁明远', 28),
  ('高三-2', '高三（2）班', '高三', 2, 2, '杨文彦', 28),
  ('高三-3', '高三（3）班', '高三', 2, 3, '毕嘉宁', 28),
  ('高三-4', '高三（4）班', '高三', 2, 4, '汤慧兰', 28)
ON DUPLICATE KEY UPDATE
  class_name   = VALUES(class_name),
  grade        = VALUES(grade),
  grade_index  = VALUES(grade_index),
  class_no     = VALUES(class_no),
  head_teacher = VALUES(head_teacher),
  capacity     = VALUES(capacity);

INSERT INTO school_setting
  (id, school_name, school_code, term, principal, address, phone,
   notify_absence, notify_score, notify_transfer, auto_archive, class_capacity)
VALUES
  (1, '明德中学', 'MD-3301', '2026—2027 学年 第一学期', '沈立群', '浙江省杭州市文三路 118 号', '0571-8812 6600',
   1, 0, 1, 1, 28)
ON DUPLICATE KEY UPDATE school_name = VALUES(school_name);
