package com.seedtosale.config;

import com.seedtosale.dto.SignupDto;
import com.seedtosale.repository.SignupDtoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final SignupDtoRepository signupDtoRepository;

    public GlobalControllerAdvice(SignupDtoRepository signupDtoRepository) {
        this.signupDtoRepository = signupDtoRepository;
    }

    @ModelAttribute
    public void addAuthenticatedUserToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            model.addAttribute("username", username);

            Optional<SignupDto> userOptional = signupDtoRepository.findByEmailIgnoreCase(username);
            if (userOptional.isPresent()) {
                SignupDto user = userOptional.get();
                model.addAttribute("userid", user.getId());
                System.out.println("GlobalControllerAdvice: Added userid to model: " + user.getId());
            } else {
                System.out.println("GlobalControllerAdvice: User not found for username: " + username);
            }
        } else {
            System.out.println("GlobalControllerAdvice: No authenticated user found");
        }
    }
}
