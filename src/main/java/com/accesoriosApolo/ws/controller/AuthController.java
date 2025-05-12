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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accesoriosApolo.ws.model.Rol;
import com.accesoriosApolo.ws.model.Usuario;
import com.accesoriosApolo.ws.dto.LoginRequest;
import com.accesoriosApolo.ws.dto.SignupRequest;
import com.accesoriosApolo.ws.request.VerificarOTPRequest;
import com.accesoriosApolo.ws.dto.JwtResponse;
import com.accesoriosApolo.ws.dto.MessageResponse;
import com.accesoriosApolo.ws.repository.RolRepository;
import com.accesoriosApolo.ws.repository.UsuarioRepository;
import com.accesoriosApolo.ws.security.JwtUtils;
import com.accesoriosApolo.ws.security.UserDetailsImpl;
import com.accesoriosApolo.ws.service.OTPService;

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

    // Variable temporal para guardar el usuario en proceso de registro
    private SignupRequest usuarioPendiente = null;

    @PostMapping("/registro-inicial")
    public ResponseEntity<?> registroInicial(@Valid @RequestBody SignupRequest signUpRequest) {
        if (usuarioRepository.existsByCorreo(signUpRequest.getCorreo())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: El correo ya está en uso!"));
        }

        if (signUpRequest.getCedula() != null && usuarioRepository.existsByCedula(signUpRequest.getCedula())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: La cédula ya está registrada!"));
        }

        // Guardar temporalmente los datos del usuario
        usuarioPendiente = signUpRequest;

        // Enviar código OTP
        otpService.enviarCodigoOTP(signUpRequest.getCorreo());

        return ResponseEntity.ok(new MessageResponse("Código OTP enviado. Por favor, verifica tu correo."));
    }

    @PostMapping("/verificar-otp")
    public ResponseEntity<?> verificarOTP(@Valid @RequestBody VerificarOTPRequest verificarOTPRequest) {
        // Verificar el código OTP
        if (usuarioPendiente == null) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("No hay registro en proceso. Inicia el registro nuevamente."));
        }

        boolean codigoValido = otpService.verificarCodigoOTP(
                usuarioPendiente.getCorreo(),
                verificarOTPRequest.getCodigoOTP()
        );

        if (!codigoValido) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Código OTP inválido o expirado."));
        }

        // Crear nueva cuenta de usuario
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
                        Rol adminRole = rolRepository.findByNombre("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Rol modRole = rolRepository.findByNombre("ROLE_MODERATOR")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(modRole);
                        break;
                    default:
                        Rol userRole = rolRepository.findByNombre("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(userRole);
                }
            });
        }

        usuario.setRoles(roles);
        usuarioRepository.save(usuario);

        // Limpiar el usuario pendiente
        usuarioPendiente = null;

        return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente!"));
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

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                usuario.getNombre(),
                roles));
    }
}
