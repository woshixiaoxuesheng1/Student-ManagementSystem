-- 测试库结构与 db/schema.sql 一致，去掉 CREATE DATABASE / USE，交给 Spring 初始化
CREATE TABLE IF NOT EXISTS subject (
  subject_key  VARCHAR(16)  NOT NULL,
  subject_name VARCHAR(16)  NOT NULL,
  full_score   INT          NOT NULL,
  sort_no      INT          NOT NULL,
  PRIMARY KEY (subject_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS class_info (
  class_id     VARCHAR(32) NOT NULL,
  class_name   VARCHAR(64) NOT NULL,
  grade        VARCHAR(16) NOT NULL,
  grade_index  INT         NOT NULL,
  class_no     INT         NOT NULL,
  head_teacher VARCHAR(32),
  capacity     INT         NOT NULL DEFAULT 28,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (class_id),
  UNIQUE KEY uk_class_grade_no (grade, class_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

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
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS student (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  sid               CHAR(8)      NOT NULL,
  name              VARCHAR(32)  NOT NULL,
  gender            VARCHAR(4)   NOT NULL,
  class_id          VARCHAR(32)  NOT NULL,
  status            VARCHAR(8)   NOT NULL DEFAULT '在读',
  phone             VARCHAR(20),
  guardian          VARCHAR(32),
  guardian_relation VARCHAR(16),
  guardian_phone    VARCHAR(20),
  birthday          DATE,
  enrollment_date   DATE,
  address           VARCHAR(128),
  dorm              VARCHAR(32),
  note              VARCHAR(255),
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_sid (sid),
  KEY idx_student_class (class_id),
  KEY idx_student_status (status),
  CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES class_info (class_id),
  CONSTRAINT chk_student_gender CHECK (gender IN ('男', '女')),
  CONSTRAINT chk_student_status CHECK (status IN ('在读', '休学', '转学', '退学'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS student_score (
  student_id  BIGINT       NOT NULL,
  subject_key VARCHAR(16)  NOT NULL,
  score       DECIMAL(5,1) NOT NULL DEFAULT 0,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id, subject_key),
  CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
  CONSTRAINT fk_score_subject FOREIGN KEY (subject_key) REFERENCES subject (subject_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS attendance_record (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  student_id  BIGINT      NOT NULL,
  attend_date DATE        NOT NULL,
  status      VARCHAR(8)  NOT NULL,
  remark      VARCHAR(64),
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_student_date (student_id, attend_date),
  KEY idx_attendance_date (attend_date),
  CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
  CONSTRAINT chk_attendance_status CHECK (status IN ('出勤', '迟到', '缺勤', '请假'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(32)  NOT NULL,
  password      VARCHAR(100) NOT NULL,
  real_name     VARCHAR(32)  NOT NULL,
  role          VARCHAR(16)  NOT NULL,
  phone         VARCHAR(20),
  enabled       TINYINT(1)   NOT NULL DEFAULT 1,
  last_login_at DATETIME,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  CONSTRAINT chk_sys_user_role CHECK (role IN ('ADMIN', 'TEACHER', 'VIEWER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_class (
  user_id  BIGINT      NOT NULL,
  class_id VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, class_id),
  CONSTRAINT fk_user_class_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_class_class FOREIGN KEY (class_id) REFERENCES class_info (class_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO subject (subject_key, subject_name, full_score, sort_no) VALUES
  ('chinese', '语文', 150, 1), ('math', '数学', 150, 2), ('english', '英语', 150, 3),
  ('physics', '物理', 100, 4), ('chemistry', '化学', 100, 5)
ON DUPLICATE KEY UPDATE subject_name = VALUES(subject_name), full_score = VALUES(full_score);

INSERT INTO class_info (class_id, class_name, grade, grade_index, class_no, head_teacher, capacity) VALUES
  ('高一-1', '高一（1）班', '高一', 0, 1, '陆敏华', 28),
  ('高一-2', '高一（2）班', '高一', 0, 2, '周立群', 28),
  ('高二-1', '高二（1）班', '高二', 1, 1, '曹亦菲', 28),
  ('高二-2', '高二（2）班', '高二', 1, 2, '韩昱', 28),
  ('高三-1', '高三（1）班', '高三', 2, 1, '祁明远', 28),
  ('高三-2', '高三（2）班', '高三', 2, 2, '杨文彦', 28)
ON DUPLICATE KEY UPDATE class_name = VALUES(class_name), head_teacher = VALUES(head_teacher);

INSERT INTO school_setting
  (id, school_name, school_code, term, principal, address, phone,
   notify_absence, notify_score, notify_transfer, auto_archive, class_capacity)
VALUES (1, '明德中学', 'MD-3301', '2026—2027 学年 第一学期', '沈立群',
        '浙江省杭州市文三路 118 号', '0571-8812 6600', 1, 0, 1, 1, 28)
ON DUPLICATE KEY UPDATE school_name = VALUES(school_name);
