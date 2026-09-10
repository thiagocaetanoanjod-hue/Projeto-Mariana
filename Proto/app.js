const STORAGE_KEY = "registrosAtrasos";
const API_URL = window.APP_CONFIG?.API_URL?.trim() || "";

const form = document.querySelector("#formulario-registro");
const cpfInput = document.querySelector("#cpf");
const nomeInput = document.querySelector("#nome");
const tipoInput = document.querySelector("#tipo-registro");
const feedbackRegistro = document.querySelector("#feedback-registro");

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
        } catch (error) {
            if (error.tipo === "api") throw error;
            localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
            return "cache";
        }
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
    return "salvo";
}

function validarCpf(cpf) {
    const digitos = cpf.replace(/\D/g, "");
    if (digitos.length !== 11 || /^(\d)\1{10}$/.test(digitos)) return false;

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
}

function autocompletarAluno(campoOrigem) {
    const registros = getRegistros();
    
    if (campoOrigem === 'cpf') {
        const cpfAtual = cpfInput.value;
        const alunoConhecido = registros.find(r => r.cpf === cpfAtual);
        if (alunoConhecido && !nomeInput.value) {
            nomeInput.value = alunoConhecido.nome;
        }
    } else if (campoOrigem === 'nome') {
        const nomeAtual = nomeInput.value.trim().toLowerCase();
        const alunoConhecido = [...registros].reverse().find(r => r.nome.toLowerCase() === nomeAtual);
        if (alunoConhecido && !cpfInput.value) {
            cpfInput.value = alunoConhecido.cpf; // O CPF já virá formatado do histórico
        }
    }
}

nomeInput.addEventListener("blur", () => autocompletarAluno('nome'));

cpfInput.addEventListener("input", (event) => {
    event.target.value = formatarCpf(event.target.value);
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
        exibirFeedback(resultado === "cache"
            ? "Sem conexão com a API. O registro foi salvo localmente e deverá ser sincronizado depois."
            : `${tipo} registrado com sucesso.`, resultado === "cache" ? "erro-rede" : "sucesso");
    } catch (error) {
        exibirFeedback(error.tipo === "api"
            ? "O registro foi recusado pela API. Revise os dados e tente novamente."
            : "Não foi possível registrar. Tente novamente.", error.tipo === "api" ? "erro-validacao" : "erro-rede");
    }
});
