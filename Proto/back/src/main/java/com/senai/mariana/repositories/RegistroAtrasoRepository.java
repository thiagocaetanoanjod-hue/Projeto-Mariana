package com.senai.mariana.repositories;

import com.senai.mariana.models.RegistroAtraso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegistroAtrasoRepository extends JpaRepository<RegistroAtraso, Long> {

    @Query("SELECT r FROM RegistroAtraso r JOIN FETCH r.aluno a JOIN FETCH a.turma ORDER BY r.dataRegistro ASC, r.horarioRegistro ASC, r.id ASC")
    List<RegistroAtraso> findAllWithAlunoAndTurma();
}
