package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.usuarios.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "noticias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    private String resumo;

    @Column(nullable = false)
    private String autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaNoticia categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusNoticia status = StatusNoticia.RASCUNHO;

    private String tags;

    @Column(length = 1000)
    private String imagemUrl;

    @Column(unique = true, length = 500)
    private String linkOrigem;

    @Column(length = 50)
    private String fonteOrigem; // "INSTAGRAM" ou "IF_GOIANO"

    @Column(columnDefinition = "TEXT")
    private String audiodescricaoIA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_id")
    private Usuario atualizadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    private LocalDateTime publicadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
        if (status == StatusNoticia.PUBLICADO) publicadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
        if (status == StatusNoticia.PUBLICADO && publicadoEm == null) publicadoEm = LocalDateTime.now();
    }
}