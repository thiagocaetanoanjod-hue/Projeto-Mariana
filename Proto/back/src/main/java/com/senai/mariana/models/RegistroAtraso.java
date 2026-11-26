package com.senai.mariana.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Registro de atraso ou saída adiantada vinculado a um aluno.
 */
@Entity
@Table(
    name = "registros_atraso",
    indexes = {
        @Index(name = "idx_registros_aluno_data", columnList = "aluno_id, data_registro")
    }
)
public class RegistroAtraso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;

    @Column(name = "horario_registro", nullable = false)
    private LocalTime horarioRegistro;

    @Column(name = "origem_registro", nullable = false, length = 20)
    private String origemRegistro = "Manual";

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (origemRegistro == null || origemRegistro.isBlank()) {
            origemRegistro = "Manual";
        }
    }

    public RegistroAtraso() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public void setAluno(Aluno aluno) {
        this.aluno = aluno;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public LocalTime getHorarioRegistro() {
        return horarioRegistro;
    }

    public void setHorarioRegistro(LocalTime horarioRegistro) {
        this.horarioRegistro = horarioRegistro;
    }

    public String getOrigemRegistro() {
        return origemRegistro;
    }

    public void setOrigemRegistro(String origemRegistro) {
        this.origemRegistro = origemRegistro;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
