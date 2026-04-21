package com.example.integrated.oauth.service;

import com.example.integrated.oauth.entity.Role;
import com.example.integrated.oauth.entity.User;
import com.example.integrated.oauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. 부모 클래스의 loadUser()를 호출하여 구글로부터 유저 정보를 받아옵니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        return new DefaultOAuth2User(
                Set.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "sub");
    }
}
