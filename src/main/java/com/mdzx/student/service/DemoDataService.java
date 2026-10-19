package com.mdzx.student.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdzx.student.common.Constants;
import com.mdzx.student.common.SchoolCalendar;
import com.mdzx.student.domain.AttendanceRecord;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.Student;
import com.mdzx.student.domain.StudentScore;
import com.mdzx.student.domain.Subject;
import com.mdzx.student.mapper.AttendanceMapper;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.ScoreMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.mapper.SubjectMapper;
import com.mdzx.student.dto.AttendanceDtos;

/**
 * 演示数据生成器：与前端 seed.ts 使用同一套规则（同样的姓氏/名字池、同样的确定性随机、
 * 同样的 FNV-1a 考勤散列），所以后端生成的数据看起来和前端演示数据一致。
 * 启动时 student 表为空会自动生成；系统设置页的「重置演示数据」也走这里。
 */
/**
 * DemoDataService。
 * <p>定义业务服务对外提供的操作。</p>
 */
@Service("demoDataService")
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);
    private static final long SEED = 20260917L;
    private static final int BATCH_SIZE = 500;

    private static final String[] SURNAMES = {
        "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
        "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗",
        "郑", "梁", "谢", "宋", "唐", "许", "韩", "冯", "邓", "曹"
    };
    private static final String[] MALE_CHARS = {
        "浩", "宇", "俊", "泽", "博", "思", "天", "嘉", "一", "振", "文", "明",
        "志", "鹏", "立", "沐", "亦", "承", "锦", "冠", "家", "景", "启", "子"
    };
    private static final String[] FEMALE_CHARS = {
        "雨", "梓", "诗", "佳", "欣", "梦", "若", "语", "婉", "静", "雅", "书",
        "晶", "可", "晓", "舒", "楚", "以", "宁", "灵", "嘉", "子", "亦", "思"
    };
    private static final String[] MALE_TAIL = {
        "然", "轩", "航", "杰", "楷", "文", "宇", "远", "佑", "豪", "鸣",
        "辰", "诚", "飞", "宸", "程", "霖", "墨", "阳", "凯", "伦", "翔"
    };
    private static final String[] FEMALE_TAIL = {
        "欣", "涵", "怡", "彤", "曦", "嫣", "清", "宜", "雯", "瑶", "滢",
        "菲", "楠", "桐", "窈", "宁", "萱", "晴", "珊", "月", "淇", "玥"
    };
    private static final String[] GUARDIAN_MALE = { "建国", "志强", "海涛", "立新", "国庆", "明辉", "伟东", "永刚" };
    private static final String[] GUARDIAN_FEMALE = { "秀英", "丽华", "素芬", "玉兰", "慧敏", "亚萍", "晓红", "美玲" };
    private static final String[] STREETS = {
        "文三路", "教工路", "学院路", "古翠路", "天目山路", "西溪路",
        "莫干山路", "保俶北路", "丰潭路", "嘉绿苑"
    };
    private static final String[] DORMS = { "1 号楼", "2 号楼", "3 号楼", "5 号楼" };

    private final StudentMapper studentMapper;
    private final ScoreMapper scoreMapper;
    private final AttendanceMapper attendanceMapper;
    private final ClassMapper classMapper;
    private final SubjectMapper subjectMapper;

    @Value("${demo.auto-init:true}")
    private boolean autoInit;

    @Value("${demo.school-days:62}")
    private int schoolDays;

    @Value("${demo.students-per-class:24}")
    private int studentsPerClass;
    public DemoDataService(StudentMapper studentMapper, ScoreMapper scoreMapper, AttendanceMapper attendanceMapper,
            ClassMapper classMapper, SubjectMapper subjectMapper) {
        this.studentMapper = studentMapper;
        this.scoreMapper = scoreMapper;
        this.attendanceMapper = attendanceMapper;
        this.classMapper = classMapper;
        this.subjectMapper = subjectMapper;
    }

    /** 按启动配置决定是否生成初始演示数据。 */
    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        if (!autoInit) {
            return;
        }
        try {
            if (studentMapper.countAll(null) == 0) {
                Map<String, Object> stats = generate();
                log.info("演示数据已生成：{}", stats);
            }
        } catch (Exception ex) {
            log.warn("演示数据初始化失败（请确认已执行 db/schema.sql）：{}", ex.getMessage());
        }
    }

    /** 清空并重新生成，供「重置演示数据」调用 */
    @Transactional
    public synchronized Map<String, Object> reset() {
        attendanceMapper.deleteAll();
        scoreMapper.deleteAll();
        studentMapper.deleteAll();
        return generate();
    }

    /** 生成学生、成绩和考勤演示数据并返回生成摘要。 */
    @Transactional
    public synchronized Map<String, Object> generate() {
        DoubleSupplier random = makeRandom(SEED);
        List<Subject> subjects = subjectMapper.selectAll();
        List<ClassInfo> classes = classMapper.selectAll(null, null);
        List<LocalDate> termDays = SchoolCalendar.lastSchoolDays(LocalDate.now(), schoolDays);

        List<StudentSeed> seeds = new ArrayList<>();
        List<StudentScore> scoreBuffer = new ArrayList<>();
        int sequence = 0;
        int studentTotal = 0;
        int scoreTotal = 0;

        for (ClassInfo classInfo : classes) {
            int enrollYear = enrollYear(classInfo.getGrade());
            for (int i = 1; i <= studentsPerClass; i++) {
                sequence++;
                Student student = buildStudent(classInfo, enrollYear, i, sequence, random);
                studentMapper.insert(student);
                studentTotal++;
                seeds.add(new StudentSeed(student.getId(), sequence, student.getStatus()));

                scoreTotal += appendScores(scoreBuffer, student.getId(), subjects, random);
            }
        }
        if (!scoreBuffer.isEmpty()) {
            scoreMapper.batchUpsert(scoreBuffer);
        }

        int attendanceTotal = generateAttendance(seeds, termDays);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("students", studentTotal);
        stats.put("scores", scoreTotal);
        stats.put("attendanceRecords", attendanceTotal);
        stats.put("schoolDays", termDays.size());
        return stats;
    }

    /** 根据现有数据组装 student。 */
    private Student buildStudent(ClassInfo classInfo, int enrollYear, int indexInClass, int sequence,
            DoubleSupplier random) {
        String gender = random.getAsDouble() < 0.51 ? Constants.GENDER_MALE : Constants.GENDER_FEMALE;
        String name = nextName(gender, random);
        double statusRoll = random.getAsDouble();
        // 约 1.5% 退学 / 2% 转学 / 2% 休学，其余在读
        String status = statusRoll > 0.985 ? "退学"
                : statusRoll > 0.965 ? "转学"
                : statusRoll > 0.945 ? "休学" : Constants.STATUS_ACTIVE;
        boolean maleGuardian = random.getAsDouble() < 0.5;

        Student student = new Student();
        student.setSid("%d%02d%02d".formatted(enrollYear, classInfo.getClassNo(), indexInClass));
        student.setName(name);
        student.setGender(gender);
        student.setClassId(classInfo.getClassId());
        student.setStatus(status);
        student.setPhone(mobile(random, new String[] { "3", "5", "7", "8", "9" }));
        student.setGuardian(name.substring(0, 1)
                + pick(maleGuardian ? GUARDIAN_MALE : GUARDIAN_FEMALE, random));
        student.setGuardianRelation(maleGuardian ? "父亲" : "母亲");
        student.setGuardianPhone(mobile(random, new String[] { "3", "5", "7", "8" }));
        student.setBirthday(LocalDate.of(enrollYear - 16, 1 + (int) (random.getAsDouble() * 12),
                1 + (int) (random.getAsDouble() * 28)));
        student.setEnrollmentDate(LocalDate.of(enrollYear, 9, 1));
        student.setAddress("杭州市西湖区" + pick(STREETS, random) + (1 + (int) (random.getAsDouble() * 199)) + " 号");
        student.setDorm(random.getAsDouble() < 0.42
                ? pick(DORMS, random) + " " + (2 + (int) (random.getAsDouble() * 5)) + "0"
                        + (1 + (int) (random.getAsDouble() * 9)) + " 室"
                : "走读");
        student.setNote("");
        return student;
    }

    /** 按学年能力值生成 5 个学科成绩，返回写入条数 */
    private int appendScores(List<StudentScore> buffer, Long studentId, List<Subject> subjects,
            DoubleSupplier random) {
        int count = 0;
        double ability = clamp(0.52 + random.getAsDouble() * 0.42, 0.4, 0.98);
        for (Subject subject : subjects) {
            double jitter = (random.getAsDouble() - 0.5) * 0.16;
            double ratio = clamp(ability + jitter, 0.36, 0.99);
            BigDecimal score = BigDecimal.valueOf(Math.round(subject.getFullScore() * ratio))
                    .setScale(1, RoundingMode.HALF_UP);
            buffer.add(new StudentScore(studentId, subject.getSubjectKey(), score));
            count++;
        }
        if (buffer.size() >= BATCH_SIZE) {
            scoreMapper.batchUpsert(buffer);
            buffer.clear();
        }
        return count;
    }

    /** 逐日写入考勤：同一天只发一条批量插入语句 */
    private int generateAttendance(List<StudentSeed> seeds, List<LocalDate> termDays) {
        int inserted = 0;
        for (int day = 0; day < termDays.size(); day++) {
            LocalDate date = termDays.get(day);
            List<AttendanceDtos.BatchRequest.Item> items = new ArrayList<>();
            for (StudentSeed seed : seeds) {
                if (!hasAttendance(seed.status(), day, termDays.size())) {
                    continue;
                }
                items.add(new AttendanceDtos.BatchRequest.Item(seed.id(),
                        attendanceStatus(seed.sequence(), date, seed.status(), day, termDays.size())));
            }
            for (int start = 0; start < items.size(); start += BATCH_SIZE) {
                List<AttendanceDtos.BatchRequest.Item> chunk = items.subList(start,
                        Math.min(start + BATCH_SIZE, items.size()));
                attendanceMapper.batchUpsert(date, chunk);
                inserted += chunk.size();
            }
        }
        return inserted;
    }

    /** 演示数据生成时使用的学生索引：编号、生成顺序和学籍状态。 */
    private record StudentSeed(Long id, int sequence, String status) {
    }

    /** 休学学生整学期在册（后段为请假），转学/退学只统计在校期间 */
    private boolean hasAttendance(String status, int dayIndex, int termSize) {
        if ("转学".equals(status) || "退学".equals(status)) {
            return dayIndex < Math.round(termSize * 0.56);
        }
        return true;
    }

    /** 根据学生状态、日期和确定性随机序列生成当天考勤状态。 */
    private String attendanceStatus(int sequence, LocalDate date, String status, int dayIndex, int termSize) {
        if ("休学".equals(status) && dayIndex >= Math.round(termSize * 0.34)) {
            return Constants.ATTENDANCE_LEAVE;
        }
        double roll = mixedHash("stu-%04d@%s".formatted(sequence, date));
        // 口径：缺勤 0.6%、请假 3.5%、迟到 2.9%，其余出勤 → 日出勤率约 96.4%
        if (roll > 0.994) {
            return Constants.ATTENDANCE_ABSENT;
        }
        if (roll > 0.959) {
            return Constants.ATTENDANCE_LEAVE;
        }
        if (roll > 0.93) {
            return Constants.ATTENDANCE_LATE;
        }
        return Constants.ATTENDANCE_PRESENT;
    }

    /** 根据性别和随机源从姓名素材中生成学生姓名。 */
    private String nextName(String gender, DoubleSupplier random) {
        boolean male = Constants.GENDER_MALE.equals(gender);
        String[] chars = male ? MALE_CHARS : FEMALE_CHARS;
        String[] tails = male ? MALE_TAIL : FEMALE_TAIL;
        String given = random.getAsDouble() < 0.08
                ? pick(chars, random)
                : pick(chars, random) + pick(tails, random);
        return pick(SURNAMES, random) + given;
    }

    /** 根据当前年级推算学生的入学年份。 */
    private static int enrollYear(String grade) {
        return switch (grade) {
            case "高一" -> 2026;
            case "高二" -> 2025;
            default -> 2024;
        };
    }

    /** 随机选择手机号前缀并生成演示联系电话。 */
    private static String mobile(DoubleSupplier random, String[] prefixes) {
        long tail = (long) (random.getAsDouble() * 900_000_000L) + 100_000_000L;
        return "1" + pick(prefixes, random) + tail;
    }

    /** 将数值限制在给定的最小值和最大值之间。 */
    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    /** 使用给定随机源从候选值数组中选取一项。 */
    private static String pick(String[] values, DoubleSupplier random) {
        return values[(int) Math.floor(random.getAsDouble() * values.length) % values.length];
    }

    /** mulberry32：与前端同一套确定性随机 */
    private static DoubleSupplier makeRandom(long seed) {
        final long[] state = { seed & 0xFFFFFFFFL };
        return () -> {
            state[0] = (state[0] + 0x6D2B79F5L) & 0xFFFFFFFFL;
            long t = state[0];
            t = (((t ^ (t >>> 15)) * (t | 1L)) & 0xFFFFFFFFL);
            long inner = (((t ^ (t >>> 7)) * (t | 61L)) & 0xFFFFFFFFL);
            t = (t ^ ((t + inner) & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296d;
        };
    }

    /** FNV-1a：与前端 hashString 一致，保证考勤分布可复现 */
    private static double hash(String value) {
        long hash = 2166136261L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash = (hash * 16777619L) & 0xFFFFFFFFL;
        }
        return (hash & 0xFFFFFFFFL) / 4294967296d;
    }

    /**
     * 在 FNV-1a 之后再做一轮雪崩混合。
     * 单纯 FNV-1a 对「同一个人不同日期」这类相似输入的高位分布不够均匀，
     * 会让个别学生反复命中缺勤、统计尾部失真；混合后按天分布接近均匀。
     */
    private static double mixedHash(String value) {
        long x = (long) (hash(value) * 4294967296d) & 0xFFFFFFFFL;
        x ^= x >>> 16;
        x = (x * 0x7FEB352DL) & 0xFFFFFFFFL;
        x ^= x >>> 15;
        x = (x * 0x846CA68BL) & 0xFFFFFFFFL;
        x ^= x >>> 16;
        return (x & 0xFFFFFFFFL) / 4294967296d;
    }
}
