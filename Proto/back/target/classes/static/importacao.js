const API_BASE = window.APP_CONFIG?.API_BASE_URL?.trim() || "/api";

const formImportacao = document.querySelector("#form-importacao");
const arquivoInput = document.querySelector("#arquivo-planilha");
const btnImportar = document.querySelector("#btn-importar");
const feedbackImportacao = document.querySelector("#feedback-importacao");

const resumoImportacao = document.querySelector("#resumo-importacao");
const resumoLinhas = document.querySelector("#resumo-linhas");
const resumoInseridos = document.querySelector("#resumo-inseridos");
const resumoAtualizados = document.querySelector("#resumo-atualizados");
const resumoTurmas = document.querySelector("#resumo-turmas");
const boxErros = document.querySelector("#box-erros-importacao");
const listaErros = document.querySelector("#lista-erros-importacao");

const filtroBusca = document.querySelector("#filtro-busca");
const filtroTurma = document.querySelector("#filtro-turma");
const totalAlunosExibidos = document.querySelector("#total-alunos-exibidos");
const listaAlunos = document.querySelector("#lista-alunos");
const estadoVazioAlunos = document.querySelector("#estado-vazio-alunos");

let alunosCarregados = [];

function escaparHtml(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (caractere) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    }[caractere]));
}

function exibirFeedback(mensagem, tipo) {
    feedbackImportacao.textContent = mensagem;
    feedbackImportacao.className = `feedback feedback--${tipo}`;
    feedbackImportacao.hidden = false;
}

function renderizarErros(erros) {
    if (!Array.isArray(erros) || erros.length === 0) {
        boxErros.hidden = true;
        listaErros.innerHTML = "";
        return;
    }

    listaErros.innerHTML = erros
        .map((erro) => `<li>Linha ${escaparHtml(erro.linha)}: ${escaparHtml(erro.motivo)}</li>`)
        .join("");
    boxErros.hidden = false;
}

function renderizarTurmas(turmas) {
    const turmaAtual = filtroTurma.value;
    const codigos = [...new Set((Array.isArray(turmas) ? turmas : [])
        .map((t) => t.codigoTurma)
        .filter(Boolean))];

    filtroTurma.innerHTML = `<option value="">Todas as turmas</option>` +
        codigos.map((codigo) => `<option value="${escaparHtml(codigo)}">${escaparHtml(codigo)}</option>`).join("");

    if (codigos.includes(turmaAtual)) {
        filtroTurma.value = turmaAtual;
    }
}

function renderizarAlunos() {
    const termo = filtroBusca.value.trim().toLowerCase();
    const turma = filtroTurma.value;

    const filtrados = alunosCarregados.filter((aluno) => {
        const bateTurma = !turma || aluno.turma === turma;
        const bateBusca = !termo ||
            (aluno.nome || "").toLowerCase().includes(termo) ||
            (aluno.matricula || "").toLowerCase().includes(termo);
        return bateTurma && bateBusca;
    });

    totalAlunosExibidos.textContent = `${filtrados.length} ${filtrados.length === 1 ? "aluno" : "alunos"}`;
    estadoVazioAlunos.hidden = filtrados.length > 0;

    listaAlunos.innerHTML = filtrados.map((aluno) => `
        <tr>
            <td>${escaparHtml(aluno.matricula)}</td>
            <td>${escaparHtml(aluno.nome)}</td>
            <td>${escaparHtml(aluno.turma)}</td>
            <td>${escaparHtml(aluno.curso)}</td>
            <td>${escaparHtml(aluno.tipoCurso)}</td>
        </tr>
    `).join("");
}

async function carregarDados() {
    try {
        const [respTurmas, respAlunos] = await Promise.all([
            fetch(`${API_BASE}/turmas`),
            fetch(`${API_BASE}/alunos`)
        ]);

        if (!respTurmas.ok || !respAlunos.ok) {
            throw new Error("Servidor indisponível para consultar alunos.");
        }

        const [turmas, alunos] = await Promise.all([
            respTurmas.json(),
            respAlunos.json()
        ]);

        renderizarTurmas(turmas);
        alunosCarregados = Array.isArray(alunos) ? alunos : [];
        renderizarAlunos();
    } catch (error) {
        alunosCarregados = [];
        renderizarAlunos();
        exibirFeedback("Não foi possível carregar os alunos cadastrados. Verifique se o backend Spring Boot está em execução.", "erro-rede");
    }
}

formImportacao.addEventListener("submit", async (event) => {
    event.preventDefault();
    const arquivo = arquivoInput.files[0];

    if (!arquivo) {
        exibirFeedback("Selecione uma planilha (.xlsx, .xls ou .csv) antes de importar.", "erro-validacao");
        return;
    }

    const extensaoValida = /\.(xlsx|xls|csv)$/i.test(arquivo.name);
    if (!extensaoValida) {
        exibirFeedback("Formato inválido. Envie um arquivo .xlsx, .xls ou .csv.", "erro-validacao");
        return;
    }

    const formData = new FormData();
    formData.append("file", arquivo);

    btnImportar.disabled = true;
    btnImportar.textContent = "Importando...";
    feedbackImportacao.hidden = true;

    try {
        const resposta = await fetch(`${API_BASE}/importacao/alunos`, {
            method: "POST",
            body: formData
        });

        const dados = await resposta.json().catch(() => ({}));
        if (!resposta.ok) {
            exibirFeedback(dados.mensagem || "Não foi possível processar a planilha.", "erro-validacao");
            return;
        }

        resumoLinhas.textContent = dados.linhasLidas ?? 0;
        resumoInseridos.textContent = dados.inseridos ?? 0;
        resumoAtualizados.textContent = dados.atualizados ?? 0;
        resumoTurmas.textContent = dados.turmasCriadas ?? 0;
        resumoImportacao.hidden = false;

        renderizarErros(dados.erros);
        exibirFeedback("Importação concluída com sucesso.", "sucesso");
        formImportacao.reset();
        await carregarDados();
    } catch (error) {
        exibirFeedback("Falha de conexão ao enviar a planilha para o servidor.", "erro-rede");
    } finally {
        btnImportar.disabled = false;
        btnImportar.textContent = "Importar Planilha";
    }
});

filtroBusca.addEventListener("input", renderizarAlunos);
filtroTurma.addEventListener("change", renderizarAlunos);

carregarDados();
