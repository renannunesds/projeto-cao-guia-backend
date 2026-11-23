package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticiaRequestDTO {

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    @NotBlank(message = "Conteúdo é obrigatório")
    private String conteudo;

    private String resumo;

    @NotBlank(message = "Autor é obrigatório")
    private String autor;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaNoticia categoria;

    private StatusNoticia status;

    private String tags;

    private String imagemUrl;

    private String fonteOrigem;

    private String linkOrigem;

    private LocalDateTime publicadoEm;

    private String audiodescricaoIA;
}