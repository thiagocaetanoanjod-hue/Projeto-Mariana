const STORAGE_KEY = "registrosAtrasos";
const API_URL = "";

const form = document.querySelector("#formulario-registro");
const cpfInput = document.querySelector("#cpf");

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

        if (!resposta.ok) {
            throw new Error("Nao foi possivel salvar na API.");
        }
    }

    localStorage.setItem(STORAGE_KEY, JSON.stringify([...getRegistros(), registro]));
}

cpfInput.addEventListener("input", (event) => {
    event.target.value = formatarCpf(event.target.value);
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const nome = document.querySelector("#nome").value.trim();
    const cpf = cpfInput.value.trim();
    const cpfDigits = cpf.replace(/\D/g, "");

    if (cpfDigits.length !== 11) {
        cpfInput.setCustomValidity("Digite um CPF com 11 numeros.");
        cpfInput.reportValidity();
        return;
    }

    cpfInput.setCustomValidity("");
    const agora = new Date();
    const registro = {
        nome,
        cpf,
        data: dataLocal(agora),
        horario: agora.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })
    };

    try {
        await salvarRegistro(registro);
        form.reset();
        alert("Atraso registrado com sucesso.");
    } catch (error) {
        alert("Nao foi possivel registrar o atraso. Tente novamente.");
    }
});
