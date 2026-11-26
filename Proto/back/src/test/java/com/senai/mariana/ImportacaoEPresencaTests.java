package com.senai.mariana;

import com.senai.mariana.controllers.PresencaController;
import com.senai.mariana.repositories.AlunoRepository;
import com.senai.mariana.repositories.RegistroAtrasoRepository;
import com.senai.mariana.repositories.TurmaRepository;
import com.senai.mariana.services.ImportacaoPlanilhaService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ImportacaoEPresencaTests {

    @Autowired
    private ImportacaoPlanilhaService importacaoPlanilhaService;

    @Autowired
    private PresencaController presencaController;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private RegistroAtrasoRepository registroAtrasoRepository;

    @Test
    void deveImportarPlanilhaXlsxERegistrarFrequenciaComTurma() throws Exception {
        byte[] xlsxBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Table");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nº de Matrícula");
            header.createCell(1).setCellValue("Nome");
            header.createCell(2).setCellValue("CPF");
            header.createCell(3).setCellValue("Tipo de Curso");
            header.createCell(4).setCellValue("Curso");
            header.createCell(5).setCellValue("Turma");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("Afonso da Silva Santos");
            row.createCell(2).setCellValue("12345678910");
            row.createCell(3).setCellValue("Técnico");
            row.createCell(4).setCellValue("Análise e Desenvolvimento de Sistemas");
            row.createCell(5).setCellValue(12);

            workbook.write(out);
            xlsxBytes = out.toByteArray();
        }

        MockMultipartFile arquivo = new MockMultipartFile(
                "file",
                "teste.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes
        );

        Map<String, Object> resumo = importacaoPlanilhaService.importar(arquivo);
        assertEquals("sucesso", resumo.get("status"));
        assertEquals(1, resumo.get("inseridos"));
        assertEquals(1, resumo.get("turmasCriadas"));
        assertTrue(((List<?>) resumo.get("erros")).isEmpty());
        assertEquals(1, turmaRepository.count());
        assertEquals(1, alunoRepository.count());

        ResponseEntity<Map<String, Object>> respostaPost = presencaController.registrarFrequencia(Map.of(
                "nome", "Afonso da Silva Santos",
                "cpf", "123.456.789-10",
                "tipo", "Atraso",
                "data", "2026-11-26",
                "horario", "20:10"
        ));

        assertEquals(HttpStatus.OK, respostaPost.getStatusCode());
        assertNotNull(respostaPost.getBody());
        assertEquals("12", respostaPost.getBody().get("turma"));
        assertEquals("Análise e Desenvolvimento de Sistemas", respostaPost.getBody().get("curso"));
        assertEquals(1, registroAtrasoRepository.count());

        ResponseEntity<List<Map<String, Object>>> respostaGet = presencaController.listarFrequencias();
        assertEquals(HttpStatus.OK, respostaGet.getStatusCode());
        assertNotNull(respostaGet.getBody());
        assertEquals(1, respostaGet.getBody().size());
        assertEquals("12", respostaGet.getBody().get(0).get("turma"));
        assertNull(respostaGet.getBody().get(0).get("cpf"));
    }
}
