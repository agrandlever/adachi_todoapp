package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "view", defaultValue = "month") String view,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate today = LocalDate.now();
        boolean weekView = "week".equals(view);

        if (weekView) {
            addWeekViewAttributes(date != null ? date : today, model);
        } else {
            addMonthViewAttributes(year, month, today, model);
        }

        model.addAttribute("isWeekView", weekView);
        model.addAttribute("viewMode", weekView ? "week" : "month");

        return "calendar";
    }

    private void addMonthViewAttributes(Integer year, Integer month, LocalDate today, Model model) {

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
        model.addAttribute("calendarTitle", displayYearMonth.getYear() + "年"
                + displayYearMonth.getMonthValue() + "月");
        model.addAttribute("calendarWeeks", createCalendarWeeks(displayYearMonth));
        model.addAttribute("firstDate", firstDate);
        model.addAttribute("lastDate", lastDate);
        model.addAttribute("weekReferenceDate", firstDate);
        model.addAttribute("previousYear", previousMonth.getYear());
        model.addAttribute("previousMonth", previousMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());
    }

    private void addWeekViewAttributes(LocalDate referenceDate, Model model) {
        // DayOfWeek は月曜=1～日曜=7なので、余りを使うと日曜だけ0になります。
        int daysSinceSunday = referenceDate.getDayOfWeek().getValue() % 7;
        LocalDate firstDate = referenceDate.minusDays(daysSinceSunday);
        LocalDate lastDate = firstDate.plusDays(6);

        // 日曜から土曜までの7日を、月表示と同じ「1週間分の升目」として渡します。
        List<LocalDate> week = firstDate.datesUntil(lastDate.plusDays(1)).toList();

        model.addAttribute("calendarYear", referenceDate.getYear());
        model.addAttribute("calendarMonth", referenceDate.getMonthValue());
        model.addAttribute("calendarTitle", firstDate + " ～ " + lastDate);
        model.addAttribute("calendarWeeks", List.of(week));
        model.addAttribute("firstDate", firstDate);
        model.addAttribute("lastDate", lastDate);
        model.addAttribute("previousWeekDate", firstDate.minusWeeks(1));
        model.addAttribute("nextWeekDate", firstDate.plusWeeks(1));
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
