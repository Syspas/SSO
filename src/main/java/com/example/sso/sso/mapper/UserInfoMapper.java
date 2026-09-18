package com.example.sso.sso.mapper;

import com.example.sso.sso.dto.UserInfoResponse;
import com.example.sso.user.entity.SsoUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserInfoMapper {

    @Mapping(target = "name", expression = "java(user.displayName())")
    @Mapping(target = "roles", source = "roles")
    UserInfoResponse toResponse(SsoUser user);

    default List<String> mapRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
