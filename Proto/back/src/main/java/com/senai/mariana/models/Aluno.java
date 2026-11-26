package com.senai.mariana.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa um aluno importado da planilha e vinculado a uma turma.
 */
@Entity
@Table(
    name = "alunos",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_alunos_matricula", columnNames = "matricula"),
        @UniqueConstraint(name = "uk_alunos_cpf", columnNames = "cpf")
    },
    indexes = {
        @Index(name = "idx_alunos_nome", columnList = "nome")
    }
)
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "matricula", nullable = false, length = 20)
    private String matricula;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Aluno() {
    }

    public Aluno(String matricula, String nome, String cpf, Turma turma) {
        this.matricula = matricula;
        this.nome = nome;
        this.cpf = cpf;
        this.turma = turma;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
