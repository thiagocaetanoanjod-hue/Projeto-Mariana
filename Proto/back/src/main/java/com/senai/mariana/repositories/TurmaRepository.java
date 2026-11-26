package com.senai.mariana.repositories;

import com.senai.mariana.models.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {

    Optional<Turma> findByTipoCursoIgnoreCaseAndCursoIgnoreCaseAndCodigoTurmaIgnoreCase(
        String tipoCurso,
        String curso,
        String codigoTurma
    );
}
