package com.minicob.cob;

import com.minicob.businessdate.BusinessDateService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CobController {

    private final CobService cobService;
    private final BusinessDateService businessDateService;

    public CobController(CobService cobService, BusinessDateService businessDateService) {
        this.cobService = cobService;
        this.businessDateService = businessDateService;
    }

    @GetMapping("/business-date")
    public Map<String, LocalDate> businessDate() {
        return Map.of("businessDate", businessDateService.getBusinessDate());
    }

    /** Closes the current business date and advances it when COB completes. */
    @PostMapping("/cob/close-business-day")
    public CobService.CobRunResult closeBusinessDay() {
        return cobService.closeBusinessDay();
    }

    /** Re-runs COB for a date that was already (partly) closed, e.g. after fixing failed loans. */
    @PostMapping("/cob/runs")
    public CobService.CobRunResult rerun(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cobDate) {
        return cobService.runCob(cobDate);
    }
}
