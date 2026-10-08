package com.senai.mariana.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/frequencia")
@CrossOrigin(origins = "*") // Permite que o seu index.html acesse a API sem bloqueios de segurança
public class PresencaController {

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrarFrequencia(@RequestBody Map<String, String> dados) {
        String ra = dados.get("ra");
        
        // Em breve, vamos substituir isso pela busca real no banco de dados
        boolean sucesso = true; 

        Map<String, Object> resposta = new HashMap<>();
        
        if (sucesso) {
            resposta.put("status", "sucesso");
            // A mensagem formatada especificamente para o Text-to-Speech ler bem:
            resposta.put("mensagemVoz", "Presença registrada com sucesso para o aluno com R A " + ra);
            return ResponseEntity.ok(resposta);
        } else {
            resposta.put("status", "erro");
            resposta.put("mensagemVoz", "Ocorreu um erro ao registrar. Por favor, procure o docente.");
            return ResponseEntity.badRequest().body(resposta);
        }
    }
}