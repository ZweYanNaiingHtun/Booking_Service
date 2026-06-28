//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.repository.UserRepository;
import lombok.Generated;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        return (UserDetails)this.userRepository.findByCode(loginIdentifier).or(() -> this.userRepository.findByEmail(loginIdentifier)).orElseThrow(() -> new UsernameNotFoundException("User not found with Code or Email: " + loginIdentifier));
    }

    @Generated
    public CustomUserDetailsService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
