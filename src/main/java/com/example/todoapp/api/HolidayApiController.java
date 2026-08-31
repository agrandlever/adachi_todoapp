package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 祝日をJSONで返す窓口です。
 */
@RestController
@RequestMapping("/api/holidays")
public class HolidayApiController {

    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping
    public Map<String, String> getHolidays(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String, String> holidays = holidayClient.getHolidays();

        if (from == null && to == null) {
            return holidays;
        }

        // from と to の日付自体も結果に含めます。
        return holidays.entrySet().stream()
                .filter(entry -> isWithinRange(LocalDate.parse(entry.getKey()), from, to))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private boolean isWithinRange(LocalDate date, LocalDate from, LocalDate to) {
        boolean isOnOrAfterFrom = from == null || !date.isBefore(from);
        boolean isOnOrBeforeTo = to == null || !date.isAfter(to);
        return isOnOrAfterFrom && isOnOrBeforeTo;
    }
}
