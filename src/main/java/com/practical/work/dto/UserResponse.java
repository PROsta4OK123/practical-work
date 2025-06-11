package com.practical.work.dto;

import com.practical.work.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private User.Role role;
    private Integer points;
    private Boolean isActive;
} 