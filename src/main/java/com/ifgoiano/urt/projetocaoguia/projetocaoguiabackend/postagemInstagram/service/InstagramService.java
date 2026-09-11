package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.service;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.dto.PostagemInstagramDTO;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model.PostagemInstagram;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.repository.InstagramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstagramService {

    private final InstagramRepository repository;

    @Transactional
    public PostagemInstagram salvarPostagem(PostagemInstagramDTO dto) {
        PostagemInstagram postagem = PostagemInstagram.builder()
                .urlImagem(dto.getUrlImagem())
                .textoDescricao(dto.getTextoDescricao() != null ? dto.getTextoDescricao() : "Sem descrição")
                .linkPostagem(dto.getLinkPostagem())
                .dataPublicacao(dto.getDataPublicacao() != null ? dto.getDataPublicacao() : java.time.LocalDateTime.now())
                .build();

        return repository.save(postagem);
    }
}