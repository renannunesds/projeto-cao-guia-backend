package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostagemInstagramDTO {
    private String urlImagem;
    private String textoDescricao;
    private String linkPostagem;
    private LocalDateTime dataPublicacao;
}