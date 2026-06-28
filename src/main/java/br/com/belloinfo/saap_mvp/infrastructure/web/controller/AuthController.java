package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.infrastructure.security.SecurityProperties;
import br.com.belloinfo.saap_mvp.infrastructure.security.TokenService;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService, 
                          UserRepository userRepository, SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication auth = this.authenticationManager.authenticate(usernamePassword);

        org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado no banco de dados"));

        String token = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", securityProperties.getSecurity().getToken().getExpiration()));
    }
}
