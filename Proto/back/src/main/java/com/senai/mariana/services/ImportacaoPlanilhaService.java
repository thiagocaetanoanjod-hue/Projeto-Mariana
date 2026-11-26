package com.senai.mariana.services;

import com.senai.mariana.models.Aluno;
import com.senai.mariana.models.Turma;
import com.senai.mariana.repositories.AlunoRepository;
import com.senai.mariana.repositories.TurmaRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/**
 * Serviço responsável por importar alunos e turmas a partir de planilhas (.xlsx, .xls ou .csv).
 * Colunas esperadas: Nº de Matrícula, Nome, CPF, Tipo de Curso, Curso, Turma.
 */
@Service
public class ImportacaoPlanilhaService {

    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;

    public ImportacaoPlanilhaService(TurmaRepository turmaRepository, AlunoRepository alunoRepository) {
        this.turmaRepository = turmaRepository;
        this.alunoRepository = alunoRepository;
    }

    @Transactional
    public Map<String, Object> importar(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        String nomeArquivo = Optional.ofNullable(arquivo.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<List<String>> linhas = nomeArquivo.endsWith(".csv")
                ? lerCsv(arquivo.getInputStream())
                : lerExcel(arquivo.getInputStream());

        if (linhas.isEmpty()) {
            throw new IllegalArgumentException("A planilha está vazia.");
        }

        Map<String, Integer> indices = mapearCabecalho(linhas.get(0));
        validarColunasObrigatorias(indices);

        int inseridos = 0;
        int atualizados = 0;
        int turmasCriadas = 0;
        int linhasLidas = 0;
        List<Map<String, Object>> erros = new ArrayList<>();
        Map<String, Turma> cacheTurmas = new HashMap<>();

        for (int i = 1; i < linhas.size(); i++) {
            List<String> linha = linhas.get(i);
            int numeroLinha = i + 1;

            String matricula = valorColuna(linha, indices.get("matricula"));
            String nome = valorColuna(linha, indices.get("nome"));
            String cpfRaw = valorColuna(linha, indices.get("cpf"));
            String tipoCurso = valorColuna(linha, indices.get("tipoCurso"));
            String curso = valorColuna(linha, indices.get("curso"));
            String codigoTurma = valorColuna(linha, indices.get("turma"));

            if (matricula.isEmpty() && nome.isEmpty() && cpfRaw.isEmpty()
                    && tipoCurso.isEmpty() && curso.isEmpty() && codigoTurma.isEmpty()) {
                continue;
            }

            linhasLidas++;
            String cpf = cpfRaw.replaceAll("\\D", "");

            String erroValidacao = validarLinha(matricula, nome, cpf, tipoCurso, curso, codigoTurma);
            if (erroValidacao != null) {
                erros.add(criarErro(numeroLinha, erroValidacao));
                continue;
            }

            String chaveTurma = (tipoCurso + "|" + curso + "|" + codigoTurma).toLowerCase(Locale.ROOT);
            Turma turma = cacheTurmas.get(chaveTurma);
            if (turma == null) {
                Optional<Turma> existente = turmaRepository
                        .findByTipoCursoIgnoreCaseAndCursoIgnoreCaseAndCodigoTurmaIgnoreCase(tipoCurso, curso, codigoTurma);
                if (existente.isPresent()) {
                    turma = existente.get();
                } else {
                    turma = turmaRepository.save(new Turma(tipoCurso, curso, codigoTurma));
                    turmasCriadas++;
                }
                cacheTurmas.put(chaveTurma, turma);
            }

            Optional<Aluno> porCpf = alunoRepository.findByCpf(cpf);
            Optional<Aluno> porMatricula = alunoRepository.findByMatricula(matricula);

            if (porCpf.isPresent() && porMatricula.isPresent()
                    && !porCpf.get().getId().equals(porMatricula.get().getId())) {
                erros.add(criarErro(numeroLinha, "Conflito: CPF e matrícula pertencem a alunos diferentes no banco."));
                continue;
            }

            Aluno aluno = porCpf.or(() -> porMatricula).orElse(null);
            if (aluno == null) {
                alunoRepository.save(new Aluno(matricula, nome, cpf, turma));
                inseridos++;
            } else {
                aluno.setMatricula(matricula);
                aluno.setNome(nome);
                aluno.setCpf(cpf);
                aluno.setTurma(turma);
                alunoRepository.save(aluno);
                atualizados++;
            }
        }

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("status", "sucesso");
        resumo.put("linhasLidas", linhasLidas);
        resumo.put("inseridos", inseridos);
        resumo.put("atualizados", atualizados);
        resumo.put("turmasCriadas", turmasCriadas);
        resumo.put("erros", erros);
        return resumo;
    }

    private List<List<String>> lerExcel(InputStream inputStream) throws IOException {
        List<List<String>> linhas = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(new Locale("pt", "BR"));

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                return linhas;
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);
            int ultimaLinha = sheet.getLastRowNum();
            for (int r = 0; r <= ultimaLinha; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    linhas.add(Collections.emptyList());
                    continue;
                }
                int ultimaColuna = row.getLastCellNum();
                List<String> valores = new ArrayList<>();
                for (int c = 0; c < ultimaColuna; c++) {
                    Cell cell = row.getCell(c);
                    valores.add(extrairTextoCelula(cell, formatter, evaluator));
                }
                linhas.add(valores);
            }
        }
        return linhas;
    }

    private String extrairTextoCelula(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            String formatado = formatter.formatCellValue(cell, evaluator).trim();
            if (!formatado.isEmpty() && !formatado.contains("E") && !formatado.contains("e")
                    && !formatado.contains(",") && !formatado.contains(".")) {
                return formatado;
            }
            BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return bd.toPlainString();
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private List<List<String>> lerCsv(InputStream inputStream) throws IOException {
        List<List<String>> linhas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            Character separador = null;
            while ((linha = reader.readLine()) != null) {
                if (linhas.isEmpty() && linha.startsWith("\uFEFF")) {
                    linha = linha.substring(1);
                }
                if (separador == null) {
                    separador = detectarSeparadorCsv(linha);
                }
                linhas.add(parsearLinhaCsv(linha, separador));
            }
        }
        return linhas;
    }

    private char detectarSeparadorCsv(String cabecalho) {
        int pontoVirgula = cabecalho.split(";", -1).length;
        int virgula = cabecalho.split(",", -1).length;
        return pontoVirgula > virgula ? ';' : ',';
    }

    private List<String> parsearLinhaCsv(String linha, char separador) {
        List<String> colunas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char ch = linha.charAt(i);
            if (ch == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (ch == separador && !entreAspas) {
                colunas.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        colunas.add(atual.toString().trim());
        return colunas;
    }

    private Map<String, Integer> mapearCabecalho(List<String> cabecalho) {
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < cabecalho.size(); i++) {
            String normalizado = normalizarTexto(cabecalho.get(i));
            switch (normalizado) {
                case "no de matricula", "n de matricula", "numero de matricula", "matricula" ->
                        indices.putIfAbsent("matricula", i);
                case "nome", "nome completo", "aluno" ->
                        indices.putIfAbsent("nome", i);
                case "cpf" ->
                        indices.putIfAbsent("cpf", i);
                case "tipo de curso", "tipodecurso" ->
                        indices.putIfAbsent("tipoCurso", i);
                case "curso" ->
                        indices.putIfAbsent("curso", i);
                case "turma", "codigo da turma", "codigo turma" ->
                        indices.putIfAbsent("turma", i);
                default -> {
                    // Colunas extras ignoradas
                }
            }
        }
        return indices;
    }

    private void validarColunasObrigatorias(Map<String, Integer> indices) {
        List<String> faltantes = new ArrayList<>();
        if (!indices.containsKey("matricula")) faltantes.add("Nº de Matrícula");
        if (!indices.containsKey("nome")) faltantes.add("Nome");
        if (!indices.containsKey("cpf")) faltantes.add("CPF");
        if (!indices.containsKey("tipoCurso")) faltantes.add("Tipo de Curso");
        if (!indices.containsKey("curso")) faltantes.add("Curso");
        if (!indices.containsKey("turma")) faltantes.add("Turma");

        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException("Colunas obrigatórias ausentes na planilha: " + String.join(", ", faltantes));
        }
    }

    private String validarLinha(String matricula, String nome, String cpf, String tipoCurso, String curso, String codigoTurma) {
        if (matricula.isEmpty()) return "Nº de Matrícula vazio.";
        if (nome.isEmpty()) return "Nome vazio.";
        if (cpf.length() != 11) return "CPF inválido (deve conter 11 dígitos).";
        if (tipoCurso.isEmpty()) return "Tipo de Curso vazio.";
        if (curso.isEmpty()) return "Curso vazio.";
        if (codigoTurma.isEmpty()) return "Turma vazia.";
        return null;
    }

    private String valorColuna(List<String> linha, Integer indice) {
        if (indice == null || indice < 0 || indice >= linha.size()) {
            return "";
        }
        return Optional.ofNullable(linha.get(indice)).orElse("").trim();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento
                .replace("º", "o")
                .replace("°", "o")
                .replaceAll("[^a-zA-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> criarErro(int linha, String motivo) {
        Map<String, Object> erro = new LinkedHashMap<>();
        erro.put("linha", linha);
        erro.put("motivo", motivo);
        return erro;
    }
}
