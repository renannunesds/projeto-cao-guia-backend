package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.controller;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.dto.PostagemInstagramDTO;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model.PostagemInstagram;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.service.InstagramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
@Tag(name = "Instagram", description = "Endpoints para recebimento e gerenciamento de postagens do Instagram")
public class InstagramController {

    private final InstagramService service;

    @PostMapping("/sincronizar")
    @Operation(summary = "Receber e salvar nova postagem enviada pelo script de automação")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostagemInstagram> receberPostagem(@RequestBody PostagemInstagramDTO dto) {
        PostagemInstagram novaPostagem = service.salvarPostagem(dto);
        return ResponseEntity.ok(novaPostagem);
    }
}