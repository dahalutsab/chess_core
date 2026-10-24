package com.utsab.chess_engine.util.logged_in_user;

import com.utsab.chess_engine.config.user.UserInfoConfig;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoggedInUserUtil {

    private final UserInfoRepository userInfoRepository;

    /**
     * The signed-in user. The security filter already loaded and checked them, so when it left their id this returns a
     * lazy reference: reading the id costs nothing, and the rest is loaded only if used.
     */
    public UserEntity getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserInfoConfig details && details.getUserId() != null) {
                return userInfoRepository.getReferenceById(details.getUserId());
            }
            String username = getUsername(authentication);
            return userInfoRepository.findByEmailId(username)
                    .orElseThrow(() -> new EntityNotFoundException("No user found with username: " + username));
        }
        throw new IllegalStateException("No authenticated user found in SecurityContext");
    }

    private String getUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username;

        switch (principal) {
            case UserDetails userDetails -> username = userDetails.getUsername(); // Extract username from UserDetails
            case String user -> username = user;
            default -> throw new IllegalStateException("Authentication principal is not of expected type: " + principal.getClass());
        }
        return username;
    }

}