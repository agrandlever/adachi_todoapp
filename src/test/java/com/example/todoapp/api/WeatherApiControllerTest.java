package com.example.todoapp.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class WeatherApiControllerTest {

    @Test
    void returnsEmptyWeatherWhenExternalApiIsUnavailable() {
        WeatherClient weatherClient = mock(WeatherClient.class);
        when(weatherClient.getWeather()).thenReturn(
                new WeatherClient.WeatherFetchResult(Map.of(), true));
        WeatherApiController controller = new WeatherApiController(weatherClient);

        ResponseEntity<Map<String, String>> response = controller.getWeather(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of(), response.getBody());
        assertEquals("true", response.getHeaders().getFirst("X-Weather-Unavailable"));
    }
}
