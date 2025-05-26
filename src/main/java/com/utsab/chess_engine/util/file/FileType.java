package com.utsab.chess_engine.util.file;

import lombok.Getter;

@Getter
public enum FileType {
    IMAGE("image"),
    DOCUMENT("document"),
    VIDEO("video"),
    AUDIO("audio");

    private final String type;

    FileType(String type) {
        this.type = type;
    }

}
