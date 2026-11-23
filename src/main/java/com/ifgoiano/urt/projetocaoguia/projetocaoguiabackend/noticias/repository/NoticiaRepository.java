package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.repository;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.CategoriaNoticia;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.Noticia;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.StatusNoticia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    @Query("SELECT n FROM Noticia n WHERE " +
            "(:titulo IS NULL OR LOWER(n.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) AND " +
            "(:autor IS NULL OR LOWER(n.autor) LIKE LOWER(CONCAT('%', :autor, '%'))) AND " +
            "(:categoria IS NULL OR n.categoria = :categoria) AND " +
            "(:status IS NULL OR n.status = :status) AND " +
            "(:tags IS NULL OR LOWER(n.tags) LIKE LOWER(CONCAT('%', :tags, '%'))) AND " +
            "(:dataInicio IS NULL OR n.criadoEm >= :dataInicio) AND " +
            "(:dataFim IS NULL OR n.criadoEm <= :dataFim)")
    Page<Noticia> buscarComFiltros(
            @Param("titulo") String titulo,
            @Param("autor") String autor,
            @Param("categoria") CategoriaNoticia categoria,
            @Param("status") StatusNoticia status,
            @Param("tags") String tags,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    long countByStatus(StatusNoticia status);

    long countByCategoria(CategoriaNoticia categoria);

    Optional<Noticia> findByLinkOrigem(String linkOrigem);

    List<Noticia> findTop20ByOrderByCriadoEmDesc();

    List<Noticia> findByCriadoEmBeforeOrderByCriadoEmAsc(LocalDateTime dataLimite);
}