package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.service;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model.PostagemInstagram;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.repository.InstagramRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstagramScraperService {

    private final InstagramRepository repository;

    @Transactional
    public List<PostagemInstagram> sincronizarUltimasPostagens(String urlPerfil) {
        List<PostagemInstagram> postsSalvos = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(urlPerfil)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            Elements imagens = doc.select("meta[property=og:image]");
            Elements descricoes = doc.select("meta[property=og:description]");

            if (!imagens.isEmpty()) {
                PostagemInstagram post = PostagemInstagram.builder()
                        .urlImagem(imagens.first().attr("content"))
                        .textoDescricao(!descricoes.isEmpty() ? descricoes.first().attr("content") : "Sem descrição")
                        .linkPostagem(urlPerfil)
                        .dataPublicacao(LocalDateTime.now())
                        .build();

                postsSalvos.add(repository.save(post));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return postsSalvos;
    }
}