const STORAGE_KEY = "registrosAtrasos";
const API_URL = "";

const form = document.querySelector("#formulario-registro");
const cpfInput = document.querySelector("#cpf");
const nomeInput = document.querySelector("#nome");
const tipoInput = document.querySelector("#tipo-registro");

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
        const resposta = await fetch(API_URL, {
            body: JSON.stringify(registro),
            headers: { "Content-Type": "application/json" },
            method: "POST"
        });

        if (!resposta.ok) throw new Error("Nao foi possivel salvar na API.");
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
}

// Função de Autocompletar Corrigida
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
        // Encontra o último registro com esse nome exato
        const alunoConhecido = registros.reverse().find(r => r.nome.toLowerCase() === nomeAtual);
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

    if (cpfDigits.length !== 11) {
        cpfInput.setCustomValidity("Digite um CPF válido com 11 números.");
        cpfInput.reportValidity();
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
        await salvarRegistro(registro);
        form.reset();
        alert(`${tipo} registrado com sucesso.`);
    } catch (error) {
        alert("Não foi possível registrar. Tente novamente.");
    }
});
