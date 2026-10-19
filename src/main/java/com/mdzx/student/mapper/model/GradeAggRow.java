package com.mdzx.student.mapper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 年级聚合结果：班级数、人数、平均总分、出勤率 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradeAggRow {

    private String grade;
    private Integer gradeIndex;
    private Integer classCount;
    private Integer totalCount;
    private Integer activeCount;
    private BigDecimal avgTotal;
    private Double avgRate;

    /*public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Integer getGradeIndex() {
        return gradeIndex;
    }

    public void setGradeIndex(Integer gradeIndex) {
        this.gradeIndex = gradeIndex;
    }

    public Integer getClassCount() {
        return classCount;
    }

    public void setClassCount(Integer classCount) {
        this.classCount = classCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Integer activeCount) {
        this.activeCount = activeCount;
    }

    public BigDecimal getAvgTotal() {
        return avgTotal;
    }

    public void setAvgTotal(BigDecimal avgTotal) {
        this.avgTotal = avgTotal;
    }

    public Double getAvgRate() {
        return avgRate;
    }

    public void setAvgRate(Double avgRate) {
        this.avgRate = avgRate;
    }*/
}
