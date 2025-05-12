package com.accesoriosApolo.ws.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarOTPRequest {
    @NotBlank(message = "El código OTP no puede estar vacío")
    private String codigoOTP;

    public @NotBlank(message = "El código OTP no puede estar vacío") String getCodigoOTP() {
        return codigoOTP;
    }

    public void setCodigoOTP(@NotBlank(message = "El código OTP no puede estar vacío") String codigoOTP) {
        this.codigoOTP = codigoOTP;
    }
}