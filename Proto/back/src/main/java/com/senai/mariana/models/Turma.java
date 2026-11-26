package com.senai.mariana.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa uma turma cadastrada no sistema (tipo de curso, curso e código da turma).
 */
@Entity
@Table(
    name = "turmas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_turmas_identidade", columnNames = {"tipo_curso", "curso", "codigo_turma"})
    }
)
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tipo_curso", nullable = false, length = 100)
    private String tipoCurso;

    @Column(name = "curso", nullable = false, length = 150)
    private String curso;

    @Column(name = "codigo_turma", nullable = false, length = 50)
    private String codigoTurma;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Turma() {
    }

    public Turma(String tipoCurso, String curso, String codigoTurma) {
        this.tipoCurso = tipoCurso;
        this.curso = curso;
        this.codigoTurma = codigoTurma;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoCurso() {
        return tipoCurso;
    }

    public void setTipoCurso(String tipoCurso) {
        this.tipoCurso = tipoCurso;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getCodigoTurma() {
        return codigoTurma;
    }

    public void setCodigoTurma(String codigoTurma) {
        this.codigoTurma = codigoTurma;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
