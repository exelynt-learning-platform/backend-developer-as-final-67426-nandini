package com.example.demo.service;

import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}