package com.senai.mariana.controllers;

import com.senai.mariana.models.Aluno;
import com.senai.mariana.models.RegistroAtraso;
import com.senai.mariana.repositories.AlunoRepository;
import com.senai.mariana.repositories.RegistroAtrasoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/frequencia")
@CrossOrigin(origins = "*")
public class PresencaController {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AlunoRepository alunoRepository;
    private final RegistroAtrasoRepository registroAtrasoRepository;

    public PresencaController(
            AlunoRepository alunoRepository,
            RegistroAtrasoRepository registroAtrasoRepository
    ) {
        this.alunoRepository = alunoRepository;
        this.registroAtrasoRepository = registroAtrasoRepository;
    }

    @GetMapping("/registrar")
    public ResponseEntity<List<Map<String, Object>>> listarFrequencias() {
        List<Map<String, Object>> registros = registroAtrasoRepository.findAllWithAlunoAndTurma().stream()
                .map(this::mapearRegistro)
                .toList();
        return ResponseEntity.ok(registros);
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrarFrequencia(@RequestBody Map<String, String> dados) {
        if (dados == null) {
            return responderErro(HttpStatus.BAD_REQUEST, "Dados de registro inválidos.");
        }

        String nome = limpar(dados.get("nome"));
        String matricula = limpar(dados.getOrDefault("matricula", dados.get("ra")));
        String cpfDigits = limpar(dados.get("cpf")).replaceAll("\\D", "");

        AlunoResolucao resolucao = localizarAluno(cpfDigits, matricula, nome);
        if (resolucao.erro() != null) {
            return responderErro(resolucao.status(), resolucao.erro());
        }

        Aluno aluno = resolucao.aluno();
        RegistroAtraso registro = new RegistroAtraso();
        registro.setAluno(aluno);
        registro.setTipo(normalizarTipo(dados.get("tipo")));
        registro.setDataRegistro(parsearData(dados.get("data")));
        registro.setHorarioRegistro(parsearHorario(dados.get("horario")));
        registro.setOrigemRegistro(normalizarOrigem(dados.getOrDefault("origemRegistro", dados.get("origem"))));

        RegistroAtraso salvo = registroAtrasoRepository.save(registro);

        Map<String, Object> resposta = mapearRegistro(salvo);
        resposta.put("status", "sucesso");
        resposta.put("mensagemVoz", "Presença registrada com sucesso para " + aluno.getNome());
        return ResponseEntity.ok(resposta);
    }

    private AlunoResolucao localizarAluno(String cpfDigits, String matricula, String nome) {
        if (!cpfDigits.isEmpty()) {
            if (cpfDigits.length() != 11) {
                return new AlunoResolucao(null, HttpStatus.BAD_REQUEST, "CPF inválido. Informe os 11 dígitos.");
            }
            return alunoRepository.findByCpf(cpfDigits)
                    .map(a -> new AlunoResolucao(a, null, null))
                    .orElseGet(() -> new AlunoResolucao(
                            null,
                            HttpStatus.NOT_FOUND,
                            "Aluno não encontrado no sistema. Verifique o CPF ou importe a planilha de alunos."
                    ));
        }

        if (!matricula.isEmpty()) {
            return alunoRepository.findByMatricula(matricula)
                    .map(a -> new AlunoResolucao(a, null, null))
                    .orElseGet(() -> new AlunoResolucao(
                            null,
                            HttpStatus.NOT_FOUND,
                            "Aluno não encontrado para a matrícula informada."
                    ));
        }

        if (!nome.isEmpty()) {
            List<Aluno> encontrados = alunoRepository.findByNomeIgnoreCase(nome);
            if (encontrados.size() == 1) {
                return new AlunoResolucao(encontrados.get(0), null, null);
            }
            if (encontrados.size() > 1) {
                return new AlunoResolucao(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Mais de um aluno encontrado com este nome. Por favor, informe o CPF ou a matrícula."
                );
            }
            return new AlunoResolucao(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Aluno não encontrado no sistema. Verifique o nome ou importe a planilha de alunos."
            );
        }

        return new AlunoResolucao(
                null,
                HttpStatus.BAD_REQUEST,
                "Informe CPF, matrícula ou nome do aluno para registrar."
        );
    }

    private Map<String, Object> mapearRegistro(RegistroAtraso registro) {
        Aluno aluno = registro.getAluno();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", registro.getId());
        item.put("alunoId", aluno.getId());
        item.put("matricula", aluno.getMatricula());
        item.put("nome", aluno.getNome());
        item.put("turma", aluno.getTurma().getCodigoTurma());
        item.put("curso", aluno.getTurma().getCurso());
        item.put("tipoCurso", aluno.getTurma().getTipoCurso());
        item.put("tipo", registro.getTipo());
        item.put("data", registro.getDataRegistro().toString());
        item.put("horario", registro.getHorarioRegistro().format(FORMATO_HORA));
        item.put("origemRegistro", registro.getOrigemRegistro());
        return item;
    }

    private ResponseEntity<Map<String, Object>> responderErro(HttpStatus status, String mensagemVoz) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("status", "erro");
        resposta.put("mensagemVoz", mensagemVoz);
        return ResponseEntity.status(status).body(resposta);
    }

    private String normalizarTipo(String tipo) {
        if (tipo != null) {
            String t = tipo.toLowerCase(Locale.ROOT);
            if (t.contains("adiantad") || t.contains("saída") || t.contains("saida")) {
                return "Saída Adiantada";
            }
        }
        return "Atraso";
    }

    private String normalizarOrigem(String origem) {
        if (origem != null && origem.trim().equalsIgnoreCase("voz")) {
            return "Voz";
        }
        return "Manual";
    }

    private LocalDate parsearData(String data) {
        if (data != null && !data.isBlank()) {
            try {
                return LocalDate.parse(data.trim());
            } catch (DateTimeParseException ignored) {
                // Usa data atual se o formato não for ISO
            }
        }
        return LocalDate.now();
    }

    private LocalTime parsearHorario(String horario) {
        if (horario != null && !horario.isBlank()) {
            try {
                return LocalTime.parse(horario.trim());
            } catch (DateTimeParseException ignored) {
                // Usa horário atual se o formato não for válido
            }
        }
        return LocalTime.now();
    }

    private String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private record AlunoResolucao(Aluno aluno, HttpStatus status, String erro) {
    }
}
