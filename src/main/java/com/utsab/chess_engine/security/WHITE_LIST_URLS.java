package com.utsab.chess_engine.security;

import lombok.Getter;

@Getter
public enum WHITE_LIST_URLS {
    AUTH("/api/v1/auth/**"),
    V1_ALL("/v1/**"),
    HOUSES("/api/v1/house/all/**"),
    ROOMS("/api/v1/rooms/**"),
    APPLY_FOR_ROOM("/api/v1/tenant_details/apply"),
    FILES("/files/**");
    private final String url;

    WHITE_LIST_URLS(String url) {
        this.url = url;
    }
}