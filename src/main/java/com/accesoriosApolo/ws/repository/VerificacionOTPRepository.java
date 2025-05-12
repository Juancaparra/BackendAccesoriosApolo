package com.accesoriosApolo.ws.repository;

import com.accesoriosApolo.ws.model.VerificacionOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificacionOTPRepository extends JpaRepository<VerificacionOTP, Long> {
    Optional<VerificacionOTP> findByCorreoAndCodigoAndUtilizadoFalse(String correo, String codigo);
    Optional<VerificacionOTP> findFirstByCorreoOrderByFechaCreacionDesc(String correo);
}