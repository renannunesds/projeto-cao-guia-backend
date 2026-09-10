package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_postagens_instagram")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostagemInstagram{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String urlImagem;

    @Column(columnDefinition = "TEXT")
    private String textoDescricao;

    @Column(nullable = false, length = 500)
    private String linkPostagem;

    @Column(nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        if (dataPublicacao == null) {
            dataPublicacao = LocalDateTime.now();
        }
    }
}