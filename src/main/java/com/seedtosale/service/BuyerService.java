package com.seedtosale.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.seedtosale.repository.BuyerRepository;
import com.seedtosale.dto.SignupDto;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BuyerService implements UserDetailsService {
    @Autowired
    private BuyerRepository buyerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public void registerBuyer(SignupDto signupDto) {
        String encodedPassword = passwordEncoder.encode(signupDto.getPassword());
        signupDto.setPassword(encodedPassword);
        if (signupDto.getRoles() == null || signupDto.getRoles().isEmpty()) {
            signupDto.getRoles().add("ROLE_USER");
        }
        buyerRepository.save(signupDto);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        SignupDto buyer = buyerRepository.findByEmail(email);
        if (buyer == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        List<SimpleGrantedAuthority> authorities = buyer.getRoles().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return new User(
            buyer.getEmail(),
            buyer.getPassword(),
            authorities
        );
    }

    public SignupDto loadBuyerByEmail(String email) {
        return buyerRepository.findByEmail(email);
    }
}
