package com.utsab.chess_engine.user.controller;

import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.response.ApiResponse;
import com.utsab.chess_engine.user.messages.UserSwaggerDocumentationMessage;
import com.utsab.chess_engine.user.service.UserService;
import com.utsab.chess_engine.util.messages.ResponseMessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.utsab.chess_engine.user.messages.UserAPIConstants.API_USER;


@RestController
@RequestMapping(API_USER)
@RequiredArgsConstructor
@Slf4j
public class UserController extends BaseController {

    private static final String USER = "User";
    private final UserService userService;


    @Operation(
            summary = UserSwaggerDocumentationMessage.GET_LOGGED_IN_USER_SUMMARY,
            description = UserSwaggerDocumentationMessage.GET_LOGGED_IN_USER_DESCRIPTION
    )
    @GetMapping
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> getLoggedInUser(){
        return successResponse(userService.getLoggedInUser(), ResponseMessageUtil.fetchedSuccessfully(USER));
    }

    //getUserById
    @Operation(
            summary = UserSwaggerDocumentationMessage.GET_USER_BY_ID_SUMMARY,
            description = UserSwaggerDocumentationMessage.GET_USER_BY_ID_DESCRIPTION
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> getUserById(@PathVariable Long userId){
        return successResponse(userService.getUserById(userId), ResponseMessageUtil.fetchedSuccessfully(USER));
    }

}