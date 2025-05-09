package com.accesoriosApolo.ws.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String correo;

    @NotBlank
    private String contrasena;

    public @NotBlank String getCorreo() {
        return correo;
    }

    public void setCorreo(@NotBlank String correo) {
        this.correo = correo;
    }

    public @NotBlank String getContrasena() {
        return contrasena;
    }

    public void setContrasena(@NotBlank String contrasena) {
        this.contrasena = contrasena;
    }
}