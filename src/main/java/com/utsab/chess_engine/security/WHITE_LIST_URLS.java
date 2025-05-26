package com.utsab.chess_engine.security;

import lombok.Getter;

@Getter
public enum WHITE_LIST_URLS {
    AUTH("/api/v1/auth/**"),
    SWAGGER("/v1/**"),;
    private final String url;

    WHITE_LIST_URLS(String url) {
        this.url = url;
    }

}