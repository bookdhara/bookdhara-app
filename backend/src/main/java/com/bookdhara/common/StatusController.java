package com.bookdhara.common;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A plain JSON status page for people opening the API in a browser.
 * (/actuator/health uses a special content type that phone browsers download instead of showing.)
 */
@RestController
public class StatusController {

    @GetMapping(value = {"/", "/api/status"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> status() {
        return Map.of("app", "BookDhara API", "status", "UP");
    }
}
