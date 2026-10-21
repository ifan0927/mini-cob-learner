package com.minicob.report;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final OverdueReportService overdueReportService;

    public ReportController(OverdueReportService overdueReportService) {
        this.overdueReportService = overdueReportService;
    }

    @GetMapping("/overdue-loans")
    public List<OverdueReportService.OverdueLoanLine> overdueLoans() {
        return overdueReportService.overdueLoans();
    }
}
