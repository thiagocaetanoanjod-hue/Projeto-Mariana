package com.senai.mariana.repositories;

import com.senai.mariana.models.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {

    Optional<Aluno> findByCpf(String cpf);

    Optional<Aluno> findByMatricula(String matricula);

    List<Aluno> findByNomeIgnoreCase(String nome);

    @Query("SELECT a FROM Aluno a JOIN FETCH a.turma ORDER BY a.nome ASC")
    List<Aluno> findAllWithTurma();
}
