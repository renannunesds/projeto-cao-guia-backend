package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NoticiaResponseDTO {
    private Long id;
    private String titulo;
    private String conteudo;
    private String resumo;
    private String autor;
    private CategoriaNoticia categoria;
    private StatusNoticia status;
    private String tags;
    private String imagemUrl;
    private String linkOrigem;
    private String fonteOrigem;
    private String audiodescricaoIA;
    private LocalDateTime criadoEm;
    private LocalDateTime publicadoEm;

    public static NoticiaResponseDTO from(Noticia noticia) {
        return NoticiaResponseDTO.builder()
                .id(noticia.getId())
                .titulo(noticia.getTitulo())
                .conteudo(noticia.getConteudo())
                .resumo(noticia.getResumo())
                .autor(noticia.getAutor())
                .categoria(noticia.getCategoria())
                .status(noticia.getStatus())
                .tags(noticia.getTags())
                .imagemUrl(noticia.getImagemUrl())
                .linkOrigem(noticia.getLinkOrigem())
                .fonteOrigem(noticia.getFonteOrigem())
                .audiodescricaoIA(noticia.getAudiodescricaoIA())
                .criadoEm(noticia.getCriadoEm())
                .publicadoEm(noticia.getPublicadoEm())
                .build();
    }
}