package com.example.todoapp.api;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 外部の祝日APIから祝日を取得します。
 */
@Component
public class HolidayClient {

    private static final String HOLIDAY_API_URL =
            "https://holidays-jp.github.io/api/v1/date.json";

    private final RestClient restClient;

    public HolidayClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 外部APIとの接続完了までは3秒で諦めます。
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        // 接続後、外部APIからデータが返るまでは5秒で諦めます。
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public HolidayFetchResult getHolidays() {
        try {
            Map<String, String> holidays = restClient.get()
                    .uri(HOLIDAY_API_URL)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() {
                    });

            if (holidays == null) {
                return HolidayFetchResult.unavailableResult();
            }
            return new HolidayFetchResult(holidays, false);
        } catch (RestClientException exception) {
            // 接続失敗や時間切れの場合は、空の祝日一覧として扱います。
            return HolidayFetchResult.unavailableResult();
        }
    }

    public record HolidayFetchResult(Map<String, String> holidays, boolean unavailable) {

        private static HolidayFetchResult unavailableResult() {
            return new HolidayFetchResult(Collections.emptyMap(), true);
        }
    }
}
