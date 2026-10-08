const STORAGE_KEY = "registrosAtrasos";
// Lembre-se de configurar o seu APP_CONFIG para apontar para o Spring Boot: 'http://localhost:8080/api/frequencia/registrar'
const API_URL = window.APP_CONFIG?.API_URL?.trim() || "";

const form = document.querySelector("#formulario-registro");
const cpfInput = document.querySelector("#cpf");
const nomeInput = document.querySelector("#nome");
const tipoInput = document.querySelector("#tipo-registro");
const feedbackRegistro = document.querySelector("#feedback-registro");

// === VARIÁVEIS DE ACESSIBILIDADE ===
let modoVozAtivado = false;
const ecraBoasVindas = document.getElementById('ecra-boas-vindas');
const btnNormal = document.getElementById('btn-normal');

// Função que faz o navegador falar
function falar(texto) {
    if (modoVozAtivado && texto) {
        window.speechSynthesis.cancel(); 
        const fala = new SpeechSynthesisUtterance(texto);
        fala.lang = 'pt-BR';
        fala.rate = 1.1;     
        window.speechSynthesis.speak(fala);
    }
}

// === LÓGICA DO BANCO DE DADOS LOCAL E API ===
function getRegistros() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
    } catch (error) {
        return [];
    }
}

function formatarCpf(value) {
    const digits = value.replace(/\D/g, "").slice(0, 11);
    return digits
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

function dataLocal(date) {
    const ano = date.getFullYear();
    const mes = String(date.getMonth() + 1).padStart(2, "0");
    const dia = String(date.getDate()).padStart(2, "0");
    return `${ano}-${mes}-${dia}`;
}

async function salvarRegistro(registro) {
    if (API_URL) {
        try {
            const resposta = await fetch(API_URL, {
                body: JSON.stringify(registro),
                headers: { "Content-Type": "application/json" },
                method: "POST"
            });

            if (!resposta.ok) {
                const erro = new Error("A API recusou o registro.");
                erro.tipo = "api";
                throw erro;
            }
            
            // Se a API retornar uma mensagem personalizada para voz (como configurado no Spring Boot)
            const dados = await resposta.json();
            if (dados && dados.mensagemVoz) {
                falar(dados.mensagemVoz);
            }
            
        } catch (error) {
            if (error.tipo === "api") throw error;
            localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
            return "cache";
        }
    } else {
        localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
    }
    return "salvo";
}

function validarCpf(cpf) {
    const digitos = cpf.replace(/\D/g, "");
    if (digitos.length !== 11) return false;

    const calcularDigito = (base, pesoInicial) => {
        const soma = [...base].reduce((total, digito, indice) => total + Number(digito) * (pesoInicial - indice), 0);
        const resto = (soma * 10) % 11;
        return resto === 10 ? 0 : resto;
    };

    return calcularDigito(digitos.slice(0, 9), 10) === Number(digitos[9])
        && calcularDigito(digitos.slice(0, 10), 11) === Number(digitos[10]);
}

function exibirFeedback(mensagem, tipo) {
    feedbackRegistro.textContent = mensagem;
    feedbackRegistro.className = `feedback feedback--${tipo}`;
    feedbackRegistro.hidden = false;
    
    // A API de voz agora lê automaticamente qualquer feedback (erro ou sucesso)
    falar(mensagem); 
}

// === LÓGICA DO FORMULÁRIO ===
function autocompletarAluno(campoOrigem) {
    const registros = getRegistros();
    
    if (campoOrigem === 'cpf') {
        const cpfAtual = cpfInput.value;
        const alunoConhecido = registros.find(r => r.cpf === cpfAtual);
        if (alunoConhecido && !nomeInput.value) {
            nomeInput.value = alunoConhecido.nome;
            falar("Nome autocompletado com: " + alunoConhecido.nome);
        }
    } else if (campoOrigem === 'nome') {
        const nomeAtual = nomeInput.value.trim().toLowerCase();
        const alunoConhecido = [...registros].reverse().find(r => r.nome.toLowerCase() === nomeAtual);
        if (alunoConhecido && !cpfInput.value) {
            cpfInput.value = alunoConhecido.cpf; 
            falar("CPF autocompletado.");
        }
    }
}

nomeInput.addEventListener("blur", () => autocompletarAluno('nome'));

cpfInput.addEventListener("input", (event) => {
    // 1. Limpa imediatamente o erro visual assim que o aluno volta a digitar
    event.target.setCustomValidity(""); 
    
    // 2. Esconde a tarja vermelha de erro lá em baixo
    if (feedbackRegistro) feedbackRegistro.hidden = true; 
    
    // 3. Continua a formatar com os pontos e traço
    event.target.value = formatarCpf(event.target.value);
    
    // 4. Autocompleta se chegar aos 14 caracteres
    if (event.target.value.length === 14) {
        autocompletarAluno('cpf');
    }
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const nome = nomeInput.value.trim();
    const cpf = cpfInput.value.trim();
    const cpfDigits = cpf.replace(/\D/g, "");
    const tipo = tipoInput.value;

    if (!validarCpf(cpfDigits)) {
        cpfInput.setCustomValidity("Digite um CPF válido.");
        cpfInput.reportValidity();
        exibirFeedback("Verifique o CPF informado e tente novamente.", "erro-validacao");
        return;
    }
    cpfInput.setCustomValidity("");

    const agora = new Date();
    const registro = {
        nome,
        cpf,
        tipo, 
        data: dataLocal(agora),
        horario: agora.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })
    };

    try {
        const resultado = await salvarRegistro(registro);
        form.reset();
        
        // Se já tiver falado pela API lá na função salvarRegistro, evita falar 2x
        if (API_URL === "") {
            exibirFeedback(resultado === "cache"
                ? "Sem conexão com a API. O registro foi salvo localmente."
                : `${tipo} registrado com sucesso.`, resultado === "cache" ? "erro-rede" : "sucesso");
        }
    } catch (error) {
        exibirFeedback(error.tipo === "api"
            ? "O registro foi recusado pela API. Revise os dados."
            : "Não foi possível registrar. Tente novamente.", error.tipo === "api" ? "erro-validacao" : "erro-rede");
    }
});

// === LÓGICA DE ACESSIBILIDADE (BOAS VINDAS E NAVEGAÇÃO) ===

// 1. Interação com a tela de Boas-vindas
document.addEventListener('keydown', function(event) {
    // Só ativa a voz se a tela inicial estiver visível e a tecla for Espaço
    if (ecraBoasVindas && ecraBoasVindas.style.display !== 'none' && event.code === 'Space') {
        event.preventDefault(); 
        modoVozAtivado = true;
        
        ecraBoasVindas.style.display = 'none'; // Esconde a tela de boas vindas
        
        falar("Modo de acessibilidade ativado. Utilize a tecla TAB para navegar pelos campos e Enter para selecionar.");
        
        // Foca no primeiro campo (CPF) após a instrução
        setTimeout(() => {
            if(cpfInput) cpfInput.focus();
        }, 5000); 
    }
});

if (btnNormal) {
    btnNormal.addEventListener('click', () => {
        ecraBoasVindas.style.display = 'none'; // Apenas esconde a tela, voz continua desligada
    });
}

// 2. Leitura dos campos ao usar a tecla TAB (Focus)
// Adiciona o evento para todos os inputs, selects e buttons que encontrar na tela
const elementosNavegaveis = document.querySelectorAll('input, button, select');

elementosNavegaveis.forEach(elemento => {
    elemento.addEventListener('focus', () => {
        // Pega o aria-label, se não tiver pega o placeholder, se não pega o texto dentro
        let textoParaLer = elemento.getAttribute('aria-label') || elemento.placeholder || elemento.innerText || elemento.name;
        
        // Verifica o id para dar mensagens mais específicas
        if (elemento.id === 'tipo-registro') {
            textoParaLer = "Selecione o tipo de registro. Use as setas para cima e para baixo para mudar.";
        }
        
        if (elemento.tagName === 'INPUT') {
            falar("Campo " + textoParaLer + ". Pode começar a escrever.");
        } else {
            falar(textoParaLer);
        }
    });
});