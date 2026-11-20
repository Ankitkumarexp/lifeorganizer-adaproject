package com.lifeorganizer;

import org.springframework.security.crypto.password.PasswordEncoder;

public final class LifeOrganizerPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return Auth.hash(rawPassword.toString().toCharArray());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return Auth.verify(rawPassword.toString().toCharArray(), encodedPassword);
    }
}
