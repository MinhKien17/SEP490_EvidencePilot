package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.UserResponse;
import com.evidencepilot.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}
