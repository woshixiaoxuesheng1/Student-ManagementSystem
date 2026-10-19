package com.mdzx.student.mapper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 成绩筛选范围内的聚合：人数、平均分、最高分、及格/优秀人数 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradeStatsRow {

    private Integer studentCount;
    private BigDecimal avgTotal;
    private BigDecimal bestTotal;
    private Integer passCount;
    private Integer excellentCount;

    /*public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public BigDecimal getAvgTotal() {
        return avgTotal;
    }

    public void setAvgTotal(BigDecimal avgTotal) {
        this.avgTotal = avgTotal;
    }

    public BigDecimal getBestTotal() {
        return bestTotal;
    }

    public void setBestTotal(BigDecimal bestTotal) {
        this.bestTotal = bestTotal;
    }

    public Integer getPassCount() {
        return passCount;
    }

    public void setPassCount(Integer passCount) {
        this.passCount = passCount;
    }

    public Integer getExcellentCount() {
        return excellentCount;
    }

    public void setExcellentCount(Integer excellentCount) {
        this.excellentCount = excellentCount;
    }*/
}
