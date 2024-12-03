package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.security.JwtService;
import ru.mastkey.cloudservice.service.UserService;
import ru.mastkey.model.AuthUserRequest;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.TokenResponse;

import static ru.mastkey.cloudservice.util.Constants.MSG_USER_ALREADY_EXIST;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final ConversionService conversionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ServiceException(ErrorType.CONFLICT, MSG_USER_ALREADY_EXIST, request.getUsername());
        }

        var user = conversionService.convert(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return conversionService.convert(userRepository.save(user), CreateUserResponse.class);
    }

    @Override
    public TokenResponse auth(AuthUserRequest authUserRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authUserRequest.getUsername(), authUserRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new ServiceException(ErrorType.UNAUTHORIZED, "Incorrect password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(authUserRequest.getUsername());
        var userId = userRepository.findByUsername(authUserRequest.getUsername()).get().getId();

        var token = jwtService.generateToken(userDetails.getUsername(), userId);

        return new TokenResponse(token);
    }
}
