package com.senai.mariana.controllers;

import com.senai.mariana.models.Aluno;
import com.senai.mariana.models.Turma;
import com.senai.mariana.repositories.AlunoRepository;
import com.senai.mariana.repositories.TurmaRepository;
import com.senai.mariana.services.ImportacaoPlanilhaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImportacaoController {

    private final ImportacaoPlanilhaService importacaoPlanilhaService;
    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;

    public ImportacaoController(
            ImportacaoPlanilhaService importacaoPlanilhaService,
            AlunoRepository alunoRepository,
            TurmaRepository turmaRepository
    ) {
        this.importacaoPlanilhaService = importacaoPlanilhaService;
        this.alunoRepository = alunoRepository;
        this.turmaRepository = turmaRepository;
    }

    @PostMapping("/importacao/alunos")
    public ResponseEntity<Map<String, Object>> importarAlunos(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(importacaoPlanilhaService.importar(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "erro",
                    "mensagem", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "erro",
                    "mensagem", "Falha ao processar a planilha: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/alunos")
    public ResponseEntity<List<Map<String, Object>>> listarAlunos() {
        List<Map<String, Object>> alunos = alunoRepository.findAllWithTurma().stream()
                .map(this::mapearAlunoSemDadosSensiveis)
                .toList();
        return ResponseEntity.ok(alunos);
    }

    @GetMapping("/turmas")
    public ResponseEntity<List<Turma>> listarTurmas() {
        return ResponseEntity.ok(turmaRepository.findAll());
    }

    private Map<String, Object> mapearAlunoSemDadosSensiveis(Aluno aluno) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", aluno.getId());
        item.put("matricula", aluno.getMatricula());
        item.put("nome", aluno.getNome());
        item.put("turma", aluno.getTurma().getCodigoTurma());
        item.put("curso", aluno.getTurma().getCurso());
        item.put("tipoCurso", aluno.getTurma().getTipoCurso());
        return item;
    }
}
