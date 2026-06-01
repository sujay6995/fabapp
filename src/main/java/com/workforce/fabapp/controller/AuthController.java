package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.LoginRequestDto;
import com.workforce.fabapp.dto.LoginResponseDto;
import com.workforce.fabapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }
}