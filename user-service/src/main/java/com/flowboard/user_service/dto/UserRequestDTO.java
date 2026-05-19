package com.flowboard.user_service.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserRequestDTO {

	@NotBlank(message = "Full name is required")
	private String fullName;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
   

    private String username;
    private String role;
    private String avatarUrl;
}
