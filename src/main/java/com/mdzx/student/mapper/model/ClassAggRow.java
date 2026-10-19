package com.mdzx.student.mapper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 班级聚合结果：人数、平均总分、出勤率 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassAggRow {

    private String classId;
    private Integer totalCount;
    private Integer activeCount;
    private BigDecimal avgTotal;
    private Double avgRate;

    /*public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
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
