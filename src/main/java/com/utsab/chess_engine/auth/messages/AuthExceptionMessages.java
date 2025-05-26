package com.utsab.chess_engine.auth.messages;

public class AuthExceptionMessages {
    public static final String INVALID_EMAIL = "Invalid recipientEmail format";
    public static final String INVALID_PHONE = "Invalid phone number format";
    public static final String ROLES_NOT_FOUND = "Roles not found";

    private AuthExceptionMessages(){}
    public static final String USER_NOT_FOUND = "User not found with recipientEmail: ";
    public static final String INVALID_CREDENTIALS = "Invalid recipientEmail or password";
    public static final String REFRESH_TOKEN_MISSING = "Refresh token is missing";
    public static final String REFRESH_TOKEN_REVOKED = "Refresh token revoked";
    public static final String TRY_AGAIN = "Please Try Again";
}
