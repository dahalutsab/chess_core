package com.utsab.chess_engine.security;

import lombok.Getter;
import org.springframework.http.HttpMethod;

@Getter
public enum WHITE_LIST_URLS {
    AUTH("/api/v1/auth/**", new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}),
    SWAGGER("/v1/swagger-ui/**", new HttpMethod[]{HttpMethod.GET}),
    BOTS("/api/v1/bots/**", new HttpMethod[]{HttpMethod.GET}),
    FILES("/files/**", new HttpMethod[]{HttpMethod.GET});

    private final String url;
    private final HttpMethod[] methods;

    WHITE_LIST_URLS(String url, HttpMethod[] methods) {
        this.url = url;
        this.methods = methods;
    }
}