package com.example.todoapp.api;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 外部のOpen-Meteoから日ごとの天気を取得します。
 */
@Component
public class WeatherClient {

    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast";
    private static final double LATITUDE = 35.713889;
    private static final double LONGITUDE = 139.777222;

    private final RestClient restClient;

    public WeatherClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 外部APIとの接続完了までは3秒で諦め、カレンダーの表示を待たせ続けません。
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherFetchResult getWeather() {
        try {
            OpenMeteoResponse response = restClient.get()
                    .uri(WEATHER_API_URL
                            + "?latitude={latitude}&longitude={longitude}"
                            + "&daily=weather_code&timezone=Asia/Tokyo&forecast_days=16",
                            LATITUDE, LONGITUDE)
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response == null || response.daily() == null
                    || response.daily().dates() == null || response.daily().weatherCodes() == null) {
                return WeatherFetchResult.unavailableResult();
            }

            List<String> dates = response.daily().dates();
            List<Integer> weatherCodes = response.daily().weatherCodes();
            if (dates.size() != weatherCodes.size()) {
                return WeatherFetchResult.unavailableResult();
            }

            Map<String, String> weatherByDate = new LinkedHashMap<>();
            for (int i = 0; i < dates.size(); i++) {
                weatherByDate.put(dates.get(i), weatherLabel(weatherCodes.get(i)));
            }
            return new WeatherFetchResult(weatherByDate, false);
        } catch (RestClientException exception) {
            // 接続失敗、時間切れ、外部APIのエラーは、空の天気一覧として扱います。
            return WeatherFetchResult.unavailableResult();
        }
    }

    private String weatherLabel(Integer code) {
        if (code == null) {
            return "天気不明";
        }
        return switch (code) {
            case 0 -> "晴れ";
            case 1, 2 -> "晴れ時々くもり";
            case 3 -> "くもり";
            case 45, 48 -> "霧";
            case 51, 53, 55, 56, 57 -> "霧雨";
            case 61, 63, 65, 66, 67 -> "雨";
            case 71, 73, 75, 77 -> "雪";
            case 80, 81, 82 -> "にわか雨";
            case 85, 86 -> "にわか雪";
            case 95, 96, 99 -> "雷雨";
            default -> "天気不明";
        };
    }

    public record WeatherFetchResult(Map<String, String> weatherByDate, boolean unavailable) {

        private static WeatherFetchResult unavailableResult() {
            return new WeatherFetchResult(Collections.emptyMap(), true);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenMeteoResponse(DailyWeather daily) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DailyWeather(
            @JsonProperty("time") List<String> dates,
            @JsonProperty("weather_code") List<Integer> weatherCodes) {
    }
}
