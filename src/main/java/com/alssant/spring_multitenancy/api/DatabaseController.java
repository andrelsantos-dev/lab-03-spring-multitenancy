package com.alssant.spring_multitenancy.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/database")
public class DatabaseController {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/user")
    public Map<String, String> currentUser() {

        String currentUser = jdbcTemplate.queryForObject(
                "select current_user",
                String.class
        );

        return Map.of(
                "currentUser",
                currentUser
        );
    }
}
