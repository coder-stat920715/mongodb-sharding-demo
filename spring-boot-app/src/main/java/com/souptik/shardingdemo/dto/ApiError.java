package com.souptik.shardingdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
