package com.alura.literalura.repository;

import com.alura.literalura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Long> {
    Autor findByNome(String nome);

    // autores vivos em um ano: nascimento <= ano AND (falecimento is null OR falecimento >= ano)
    @Query("select a from Autor a where (a.anoNascimento is null OR a.anoNascimento <= ?1) and (a.anoFalecimento is null OR a.anoFalecimento >= ?1)")
    List<Autor> findAutoresVivosEmAno(int ano);
}
