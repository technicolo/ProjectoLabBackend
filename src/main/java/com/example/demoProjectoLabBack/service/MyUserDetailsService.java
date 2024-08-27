package com.example.demoProjectoLabBack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder; // Inject the password encoder

    private InMemoryUserDetailsManager inMemoryUserDetailsManager;

    @Bean
    public UserDetailsService userDetailsService() {
        inMemoryUserDetailsManager = new InMemoryUserDetailsManager();

        inMemoryUserDetailsManager.createUser(User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER").build());

        inMemoryUserDetailsManager.createUser(User.withUsername("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("ADMIN").build());

        return inMemoryUserDetailsManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Delegate the call to the in-memory user details manager
        return inMemoryUserDetailsManager.loadUserByUsername(username);
    }
}
