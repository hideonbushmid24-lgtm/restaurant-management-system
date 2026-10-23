package com.restaurant.service;

import com.restaurant.dao.RevenueReportDAO;
import com.restaurant.model.RevenueRecord;
import com.restaurant.model.RevenueSummary;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class RevenueReportService {

    private final RevenueReportDAO revenueReportDAO;

    public RevenueReportService(
            RevenueReportDAO revenueReportDAO
    ) {
        this.revenueReportDAO =
                Objects.requireNonNull(
                        revenueReportDAO,
                        "RevenueReportDAO không được để trống."
                );
    }

    public RevenueSummary getSummary(
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        validateDateRange(startDate, endDate);

        return revenueReportDAO.getSummary(
                startDate,
                endDate
        );
    }

    public List<RevenueRecord> findRecords(
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        validateDateRange(startDate, endDate);

        return revenueReportDAO.findRecords(
                startDate,
                endDate
        );
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu và ngày kết thúc không được để trống."
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc."
            );
        }
    }
}