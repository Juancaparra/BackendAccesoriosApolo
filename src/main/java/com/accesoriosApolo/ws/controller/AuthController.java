package com.accesoriosApolo.ws.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.accesoriosApolo.ws.model.Rol;
import com.accesoriosApolo.ws.model.Usuario;
import com.accesoriosApolo.ws.dto.LoginRequest;
import com.accesoriosApolo.ws.dto.SignupRequest;
import com.accesoriosApolo.ws.dto.JwtResponse;
import com.accesoriosApolo.ws.dto.MessageResponse;
import com.accesoriosApolo.ws.repository.RolRepository;
import com.accesoriosApolo.ws.repository.UsuarioRepository;
import com.accesoriosApolo.ws.security.JwtUtils;
import com.accesoriosApolo.ws.security.UserDetailsImpl;
import com.accesoriosApolo.ws.service.OTPService;
import com.accesoriosApolo.ws.request.VerificarOTPRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    OTPService otpService;

    private SignupRequest usuarioPendiente = null;

    @PostMapping("/registro-inicial")
    public ResponseEntity<?> registroInicial(@Valid @RequestBody SignupRequest signUpRequest) {
        if (usuarioRepository.existsByCorreo(signUpRequest.getCorreo())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: El correo ya está en uso!"));
        }

        if (signUpRequest.getCedula() != null && usuarioRepository.existsByCedula(signUpRequest.getCedula())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: La cédula ya está registrada!"));
        }

        usuarioPendiente = signUpRequest;
        otpService.enviarCodigoOTP(signUpRequest.getCorreo());

        return ResponseEntity.ok(new MessageResponse("Código OTP enviado. Por favor, verifica tu correo."));
    }

    @PostMapping("/verificar-otp")
    public ResponseEntity<?> verificarOTP(@Valid @RequestBody VerificarOTPRequest verificarOTPRequest) {
        if (usuarioPendiente == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("No hay registro en proceso. Inicia el registro nuevamente."));
        }

        boolean codigoValido = otpService.verificarCodigoOTP(
                usuarioPendiente.getCorreo(),
                verificarOTPRequest.getCodigoOTP()
        );

        if (!codigoValido) {
            return ResponseEntity.badRequest().body(new MessageResponse("Código OTP inválido o expirado."));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioPendiente.getNombre());
        usuario.setCorreo(usuarioPendiente.getCorreo());
        usuario.setContrasena(encoder.encode(usuarioPendiente.getContrasena()));
        usuario.setCedula(usuarioPendiente.getCedula());
        usuario.setTelefono(usuarioPendiente.getTelefono());

        Set<String> strRoles = usuarioPendiente.getRoles();
        Set<Rol> roles = new HashSet<>();

        if (strRoles == null) {
            Rol userRole = rolRepository.findByNombre("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        roles.add(rolRepository.findByNombre("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado.")));
                        break;
                    case "mod":
                        roles.add(rolRepository.findByNombre("ROLE_MODERATOR")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado.")));
                        break;
                    default:
                        roles.add(rolRepository.findByNombre("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado.")));
                }
            });
        }

        usuario.setRoles(roles);
        usuarioRepository.save(usuario);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        usuarioPendiente.getCorreo(),
                        usuarioPendiente.getContrasena()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> rolesList = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        usuarioPendiente = null;

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                usuario.getNombre(),
                rolesList,
                "Usuario registrado e inició sesión correctamente."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        Usuario usuario = usuarioRepository.findByCorreo(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        // ✅ Aquí se agregó el mensaje faltante como sexto argumento
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                usuario.getNombre(),
                roles,
                "Inicio de sesión exitoso." // <-- mensaje agregado
        ));
    }
}
