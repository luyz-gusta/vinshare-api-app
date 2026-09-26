package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.auth.ChangePasswordRequestDTO;
import com.fiap.vinshare.domain.dto.auth.MeResponseDTO;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MeResponseDTO buildMe(User user) {
        String fullName = user.getDisplayName();
        String phone = null;

        var customer = customerRepository.findByUser(user).orElse(null);
        if (customer != null) {
            if (fullName == null) fullName = customer.getFullName();
            phone = customer.getPhone();
        }

        return MeResponseDTO.builder()
                .userId(user.getId())
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(fullName)
                .phone(phone)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequestDTO request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Senha atual incorreta");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
