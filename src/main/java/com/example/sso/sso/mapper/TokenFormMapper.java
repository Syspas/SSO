package com.example.sso.sso.mapper;

import com.example.sso.sso.dto.TokenForm;
import com.example.sso.sso.dto.TokenRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TokenFormMapper {

    TokenRequest toRequest(TokenForm form);
}
