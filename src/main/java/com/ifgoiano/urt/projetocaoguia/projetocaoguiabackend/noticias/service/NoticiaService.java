package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.service;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.core.NoticiaNotFoundException;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.core.security.AuthenticationFacade;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.estatisticas.model.TipoEntidade;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.estatisticas.model.TipoEventoEstatistica;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.estatisticas.service.EstatisticaService;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.*;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.repository.NoticiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticiaService {

    private final NoticiaRepository repository;
    private final EstatisticaService estatisticaService;
    private final AuthenticationFacade authenticationFacade;

    @Transactional
    public NoticiaResponseDTO criar(NoticiaRequestDTO dto) {
        var usuarioLogado = authenticationFacade.getAuthenticatedUser();

        var noticia = Noticia.builder()
                .titulo(dto.getTitulo())
                .conteudo(dto.getConteudo())
                .resumo(dto.getResumo())
                .autor(dto.getAutor())
                .categoria(dto.getCategoria())
                .status(dto.getStatus() != null ? dto.getStatus() : StatusNoticia.RASCUNHO)
                .tags(dto.getTags())
                .imagemUrl(dto.getImagemUrl())
                .criadoPor(usuarioLogado)
                .atualizadoPor(usuarioLogado)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

        if (noticia.getStatus() == StatusNoticia.PUBLICADO) {
            noticia.setPublicadoEm(LocalDateTime.now());
        }

        var salva = repository.save(noticia);
        estatisticaService.registrarEventoInterno(salva.getId(), TipoEntidade.NOTICIA, TipoEventoEstatistica.CRIACAO);
        return NoticiaResponseDTO.from(salva);
    }

    public NoticiaResponseDTO buscarPorId(Long id) {
        var noticia = findOrThrow(id);
        estatisticaService.registrarEventoInterno(id, TipoEntidade.NOTICIA, TipoEventoEstatistica.VISUALIZACAO);
        return NoticiaResponseDTO.from(noticia);
    }

    @Transactional
    public NoticiaResponseDTO atualizar(Long id, NoticiaRequestDTO dto) {
        var usuarioLogado = authenticationFacade.getAuthenticatedUser();
        var noticia = findOrThrow(id);

        noticia.setTitulo(dto.getTitulo());
        noticia.setTitulo(dto.getTitulo());
        noticia.setConteudo(dto.getConteudo());
        noticia.setResumo(dto.getResumo());
        noticia.setAutor(dto.getAutor());
        noticia.setCategoria(dto.getCategoria());
        if (dto.getStatus() != null) noticia.setStatus(dto.getStatus());
        noticia.setTags(dto.getTags());
        noticia.setImagemUrl(dto.getImagemUrl());
        noticia.setAtualizadoPor(usuarioLogado);
        noticia.setAtualizadoEm(LocalDateTime.now());

        if (noticia.getStatus() == StatusNoticia.PUBLICADO && noticia.getPublicadoEm() == null) {
            noticia.setPublicadoEm(LocalDateTime.now());
        }

        var salva = repository.save(noticia);
        estatisticaService.registrarEventoInterno(id, TipoEntidade.NOTICIA, TipoEventoEstatistica.ATUALIZACAO);
        return NoticiaResponseDTO.from(salva);
    }

    @Transactional
    public NoticiaResponseDTO atualizarStatus(Long id, StatusNoticia status) {
        var usuarioLogado = authenticationFacade.getAuthenticatedUser();
        var noticia = findOrThrow(id);

        noticia.setStatus(status);
        noticia.setAtualizadoPor(usuarioLogado);
        noticia.setAtualizadoEm(LocalDateTime.now());

        if (status == StatusNoticia.PUBLICADO && noticia.getPublicadoEm() == null) {
            noticia.setPublicadoEm(LocalDateTime.now());
        }

        var salva = repository.save(noticia);
        estatisticaService.registrarEventoInterno(id, TipoEntidade.NOTICIA, TipoEventoEstatistica.ATUALIZACAO);
        return NoticiaResponseDTO.from(salva);
    }

    @Transactional
    public void deletar(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
        estatisticaService.registrarEventoInterno(id, TipoEntidade.NOTICIA, TipoEventoEstatistica.EXCLUSAO);
    }

    public Page<NoticiaResponseDTO> listar(
            String titulo, String autor, CategoriaNoticia categoria,
            StatusNoticia status, String tags,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        Pageable paginacaoSegura = (pageable == null || pageable.getPageSize() < 6)
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publicadoEm"))
                : pageable;

        return repository
                .buscarComFiltros(titulo, autor, categoria, status, tags, dataInicio, dataFim, paginacaoSegura)
                .map(NoticiaResponseDTO::from);
    }

    private Noticia findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoticiaNotFoundException(id));
    }
}
