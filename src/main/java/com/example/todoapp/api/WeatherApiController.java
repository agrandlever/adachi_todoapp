package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部APIから取得した天気を、ブラウザへ返す窓口です。
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private final WeatherClient weatherClient;

    public WeatherApiController(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getWeather(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        WeatherClient.WeatherFetchResult result = weatherClient.getWeather();
        Map<String, String> weatherByDate = result.weatherByDate();

        if (from != null || to != null) {
            // カレンダーが表示している月に含まれる日だけを返します。
            weatherByDate = weatherByDate.entrySet().stream()
                    .filter(entry -> isWithinRange(LocalDate.parse(entry.getKey()), from, to))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (first, second) -> first,
                            LinkedHashMap::new));
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.unavailable()) {
            response.header("X-Weather-Unavailable", "true");
        }
        return response.body(weatherByDate);
    }

    private boolean isWithinRange(LocalDate date, LocalDate from, LocalDate to) {
        boolean isOnOrAfterFrom = from == null || !date.isBefore(from);
        boolean isOnOrBeforeTo = to == null || !date.isAfter(to);
        return isOnOrAfterFrom && isOnOrBeforeTo;
    }
}
