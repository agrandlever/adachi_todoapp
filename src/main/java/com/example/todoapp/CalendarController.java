package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month, Model model) {
        LocalDate today = LocalDate.now();

        // year または month が省略された場合は、その項目だけ現在の値で補います。
        int displayYear = year != null ? year : today.getYear();
        int displayMonth = month != null ? month : today.getMonthValue();
        YearMonth displayYearMonth = YearMonth.of(displayYear, displayMonth);

        LocalDate firstDate = displayYearMonth.atDay(1);
        LocalDate lastDate = displayYearMonth.atEndOfMonth();
        YearMonth previousMonth = displayYearMonth.minusMonths(1);
        YearMonth nextMonth = displayYearMonth.plusMonths(1);

        model.addAttribute("calendarYear", displayYearMonth.getYear());
        model.addAttribute("calendarMonth", displayYearMonth.getMonthValue());
        model.addAttribute("calendarWeeks", createCalendarWeeks(displayYearMonth));
        model.addAttribute("firstDate", firstDate);
        model.addAttribute("lastDate", lastDate);
        model.addAttribute("previousYear", previousMonth.getYear());
        model.addAttribute("previousMonth", previousMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());

        return "calendar";
    }

    private List<List<LocalDate>> createCalendarWeeks(YearMonth yearMonth) {
        List<List<LocalDate>> weeks = new ArrayList<>();
        List<LocalDate> week = new ArrayList<>();

        // DayOfWeek は月曜=1～日曜=7なので、余りを使って日曜=0に直します。
        int leadingEmptyCells = yearMonth.atDay(1).getDayOfWeek().getValue() % 7;
        int usedCells = leadingEmptyCells + yearMonth.lengthOfMonth();
        int totalCells = ((usedCells + 6) / 7) * 7;

        for (int cellIndex = 0; cellIndex < totalCells; cellIndex++) {
            int dayOfMonth = cellIndex - leadingEmptyCells + 1;

            // 月の外側は null（値がないことを表すもの）にして、空のマスにします。
            LocalDate date = dayOfMonth >= 1 && dayOfMonth <= yearMonth.lengthOfMonth()
                    ? yearMonth.atDay(dayOfMonth)
                    : null;
            week.add(date);

            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }

        return weeks;
    }
}
