package com.evidencepilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only projection of the {@code User} entity returned to the frontend.
 *
 * <p>Deliberately omits the password hash and any other internal fields
 * to prevent leaking sensitive data through the API.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Integer id;
    private String  email;
    private String  role;
    private String  firstName;
    private String  lastName;
    private Integer age;
}
