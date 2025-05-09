package com.accesoriosApolo.ws.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.accesoriosApolo.ws.model.Rol;
import com.accesoriosApolo.ws.repository.RolRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles si no existen
        if (rolRepository.findByNombre("ROLE_USER").isEmpty()) {
            Rol userRole = new Rol();
            userRole.setNombre("ROLE_USER");
            rolRepository.save(userRole);
        }

        if (rolRepository.findByNombre("ROLE_MODERATOR").isEmpty()) {
            Rol modRole = new Rol();
            modRole.setNombre("ROLE_MODERATOR");
            rolRepository.save(modRole);
        }

        if (rolRepository.findByNombre("ROLE_ADMIN").isEmpty()) {
            Rol adminRole = new Rol();
            adminRole.setNombre("ROLE_ADMIN");
            rolRepository.save(adminRole);
        }
    }
}