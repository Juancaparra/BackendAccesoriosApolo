package com.accesoriosApolo.ws.repository;

import com.accesoriosApolo.ws.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreo(String correo);
    Boolean existsByCorreo(String correo);
    Boolean existsByCedula(Integer cedula);
}