package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AuditService;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.infrastructure.security.SecurityProperties;
import br.com.belloinfo.saap_mvp.infrastructure.security.TokenBlacklistService;
import br.com.belloinfo.saap_mvp.infrastructure.security.TokenService;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService, 
                          UserRepository userRepository, SecurityProperties securityProperties,
                          AuditService auditService, TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest, HttpServletRequest httpRequest) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication auth = this.authenticationManager.authenticate(usernamePassword);

        org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado no banco de dados"));

        String token = tokenService.generateToken(user);

        auditService.log("LOGIN_USUARIO", user.getId(), "USER", user.getEmail(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", securityProperties.getSecurity().getToken().getExpiration()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklist(token);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
