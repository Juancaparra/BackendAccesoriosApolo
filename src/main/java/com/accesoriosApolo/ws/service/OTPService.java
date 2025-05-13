package com.accesoriosApolo.ws.service;

import com.accesoriosApolo.ws.model.VerificacionOTP;
import com.accesoriosApolo.ws.repository.VerificacionOTPRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class OTPService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private VerificacionOTPRepository verificacionOTPRepository;

    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    // Generar código OTP de 6 dígitos
    public String generarCodigoOTP() {
        SecureRandom random = new SecureRandom();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    // Enviar código OTP por correo
    public void enviarCodigoOTP(String correo) {
        // Generar código OTP
        String codigoOTP = generarCodigoOTP();

        // Crear entidad de verificación OTP
        VerificacionOTP verificacion = new VerificacionOTP();
        verificacion.setCorreo(correo);
        verificacion.setCodigo(codigoOTP);
        verificacion.setFechaCreacion(LocalDateTime.now());
        verificacion.setFechaExpiracion(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        verificacion.setUtilizado(false);

        // Guardar en base de datos
        verificacionOTPRepository.save(verificacion);

        // Enviar correo
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(correo);
        mensaje.setSubject("Accesorios Apolo");
        mensaje.setText("Tu código de verificación es: " + codigoOTP +
                "\nEste código expirará en " + otpExpirationMinutes + " minutos.");
        mensaje.setFrom("tu_correo@gmail.com"); // Reemplazar con tu correo

        mailSender.send(mensaje);
    }

    // Verificar código OTP
    public boolean verificarCodigoOTP(String correo, String codigo) {
        // Buscar código OTP no utilizado
        return verificacionOTPRepository.findByCorreoAndCodigoAndUtilizadoFalse(correo, codigo)
                .map(verificacion -> {
                    // Verificar si el código no ha expirado
                    if (verificacion.getFechaExpiracion().isAfter(LocalDateTime.now())) {
                        // Marcar como utilizado
                        verificacion.setUtilizado(true);
                        verificacionOTPRepository.save(verificacion);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}