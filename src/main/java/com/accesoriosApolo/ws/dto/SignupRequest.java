package com.accesoriosApolo.ws.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Email
    private String correo;

    private Integer cedula;

    private String telefono;

    @NotBlank
    @Size(min = 6, max = 100)
    private String contrasena;

    private Set<String> roles;

    public @NotBlank @Size(max = 100) String getNombre() {
        return nombre;
    }

    public void setNombre(@NotBlank @Size(max = 100) String nombre) {
        this.nombre = nombre;
    }

    public @NotBlank @Size(max = 100) @Email String getCorreo() {
        return correo;
    }

    public void setCorreo(@NotBlank @Size(max = 100) @Email String correo) {
        this.correo = correo;
    }

    public Integer getCedula() {
        return cedula;
    }

    public void setCedula(Integer cedula) {
        this.cedula = cedula;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public @NotBlank @Size(min = 6, max = 100) String getContrasena() {
        return contrasena;
    }

    public void setContrasena(@NotBlank @Size(min = 6, max = 100) String contrasena) {
        this.contrasena = contrasena;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}