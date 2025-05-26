package com.utsab.chess_engine.user.messages;


import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

public class UserAPIConstants {
    private UserAPIConstants() {}
    public static final String API_USER = API_BASE + "/user";
    public static final String CHANGE_PASSWORD = "/change-password";
}
