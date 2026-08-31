package com.example.todoapp.api;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 外部の祝日APIから祝日を取得します。
 */
@Component
public class HolidayClient {

    private static final String HOLIDAY_API_URL =
            "https://holidays-jp.github.io/api/v1/date.json";

    private final RestClient restClient;

    public HolidayClient() {
        // このアプリでは Builder を注入せず、ここで通信に使う道具を作ります。
        this.restClient = RestClient.create();
    }

    public Map<String, String> getHolidays() {
        return restClient.get()
                .uri(HOLIDAY_API_URL)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {
                });
    }
}
