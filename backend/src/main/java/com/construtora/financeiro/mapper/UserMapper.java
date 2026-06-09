package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.user.UserResponse;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getDevelopments().stream().map(Development::getId).toList());
    }
}
