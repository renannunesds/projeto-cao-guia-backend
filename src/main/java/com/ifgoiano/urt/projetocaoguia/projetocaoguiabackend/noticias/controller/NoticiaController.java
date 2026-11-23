package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.controller;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.*;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.service.NoticiaService;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.service.SincronizacaoNoticiasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/noticias")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(
        name = "Notícias",
        description = "Gerenciamento das notícias do Projeto Cão-Guia Digital"
)
public class NoticiaController {

    private final NoticiaService service;
    private final SincronizacaoNoticiasService sincronizacaoService;

    @PostMapping("/sincronizar")
    @Operation(summary = "Sincroniza notícia vinda do scraper (Instagram ou IF Goiano) com deduplicação, data real e IA")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NoticiaResponseDTO> sincronizarNoticia(@RequestBody NoticiaRequestDTO dto) {
        Noticia salva = sincronizacaoService.sincronizarItem(
                dto.getFonteOrigem() != null ? dto.getFonteOrigem() : "INSTAGRAM",
                dto.getLinkOrigem(),
                dto.getTitulo(),
                dto.getConteudo(),
                dto.getImagemUrl(),
                dto.getPublicadoEm(),
                dto.getAudiodescricaoIA()
        );
        return ResponseEntity.ok(NoticiaResponseDTO.from(salva));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar notícia por ID")
    public ResponseEntity<NoticiaResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar notícia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NoticiaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody NoticiaRequestDTO dto) {

        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da notícia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NoticiaResponseDTO> atualizarStatus(
            @PathVariable Long id,

            @Parameter(description = "Novo status da notícia")
            @RequestParam StatusNoticia status) {

        return ResponseEntity.ok(service.atualizarStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover notícia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Listar notícias",
            description = "Retorna uma lista paginada de notícias com filtros opcionais"
    )
    public ResponseEntity<Page<NoticiaResponseDTO>> listar(
            @Parameter(description = "Filtrar por título")
            @RequestParam(required = false) String titulo,

            @Parameter(description = "Filtrar por autor")
            @RequestParam(required = false) String autor,

            @Parameter(description = "Filtrar por categoria")
            @RequestParam(required = false) CategoriaNoticia categoria,

            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) StatusNoticia status,

            @Parameter(description = "Filtrar por tags")
            @RequestParam(required = false) String tags,

            @Parameter(description = "Data de início do intervalo (formato ISO)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

            @Parameter(description = "Data de fim do intervalo (formato ISO)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,

            @PageableDefault(size = 10, sort = "publicadoEm", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(service.listar(titulo, autor, categoria, status, tags, dataInicio, dataFim, pageable));
    }

    @PostMapping("/sincronizar-ifgoiano")
    @Operation(summary = "Força a varredura imediata no portal do IF Goiano")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sincronizarIfGoianoAgora() {
        sincronizacaoService.varrerPortalIfGoiano();
        return ResponseEntity.ok().build();
    }
}