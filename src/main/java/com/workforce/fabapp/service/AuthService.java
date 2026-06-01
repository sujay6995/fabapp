package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.LoginRequestDto;
import com.workforce.fabapp.dto.LoginResponseDto;
import com.workforce.fabapp.entity.User;
import com.workforce.fabapp.repository.UserRepository;
import com.workforce.fabapp.security.CustomUserDetails;
import com.workforce.fabapp.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new EntityNotFoundException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("User is inactive");
        }

        if (!user.getPasswordHash().equals(request.getPassword())) {
            throw new IllegalStateException("Invalid username or password");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return LoginResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole().name())
                .employeeId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .supervisorId(user.getSupervisor() != null ? user.getSupervisor().getId() : null)
                .title(user.getTitle())
                .active(user.getActive())
                .token(token)
                .tokenType("Bearer")
                .build();
    }
}