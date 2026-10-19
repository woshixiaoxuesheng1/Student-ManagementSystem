package com.mdzx.student.mapper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 出勤率趋势：按周聚合 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendRow {

    private String weekKey;
    private LocalDate startDate;
    private Double rate;

    /*public String getWeekKey() {
        return weekKey;
    }

    public void setWeekKey(String weekKey) {
        this.weekKey = weekKey;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }*/
}
