package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.controller;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model.PostagemInstagram;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.service.InstagramScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
@Tag(name = "Instagram", description = "Endpoints para sincronização e gerenciamento de postagens do Instagram")
public class InstagramController {

    private final InstagramScraperService service;

    @GetMapping("/sincronizar")
    @Operation(summary = "Sincronizar postagens do Instagram via Web Scraping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PostagemInstagram>> sincronizar() {
        String urlPerfil = "https://www.instagram.com/seu_perfil_aqui/";
        List<PostagemInstagram> novasPostagens = service.sincronizarUltimasPostagens(urlPerfil);
        return ResponseEntity.ok(novasPostagens);
    }
}