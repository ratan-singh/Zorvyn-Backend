package com.zorvyn.financedashboard.security;

import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security calls this with the "username" — which in our system is the email.
     * The User entity directly implements UserDetails, so no mapping is needed.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication attempt for non-existent email: {}", email);
                    return new UsernameNotFoundException(
                            "No account found with email: " + email);
                });
    }
}
