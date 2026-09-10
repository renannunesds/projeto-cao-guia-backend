package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.repository;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.postagemInstagram.model.PostagemInstagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstagramRepository extends JpaRepository<PostagemInstagram, Long> {
}