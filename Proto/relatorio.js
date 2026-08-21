const STORAGE_KEY = "registrosAtrasos";

const totalRegistros = document.querySelector("#total-registros");
const registrosHoje = document.querySelector("#registros-hoje");
const percentualHoje = document.querySelector("#percentual-hoje");
const listaRegistros = document.querySelector("#lista-registros");
const estadoVazio = document.querySelector("#estado-vazio");
const seletorAluno = document.querySelector("#seletor-aluno");
const alunoSelecionado = document.querySelector("#aluno-selecionado");
const cpfSelecionado = document.querySelector("#cpf-selecionado");

function carregarRegistros() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
    } catch (error) {
        return [];
    }
}

function escaparHtml(value) {
    return String(value).replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    }[character]));
}

function dataAtual() {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, "0");
    const dia = String(hoje.getDate()).padStart(2, "0");
    return `${ano}-${mes}-${dia}`;
}

function formatarData(data) {
    const [ano, mes, dia] = data.split("-");
    return `${dia}/${mes}/${ano}`;
}

function atualizarRelatorio() {
    const registros = carregarRegistros();
    const hoje = registros.filter((registro) => registro.data === dataAtual()).length;
    const percentual = registros.length ? Math.round((hoje / registros.length) * 100) : 0;

    totalRegistros.textContent = registros.length;
    registrosHoje.textContent = hoje;
    percentualHoje.textContent = `${percentual}%`;

    const alunos = [...new Map(registros.map((registro) => [registro.cpf, {
        cpf: registro.cpf,
        nome: registro.nome
    }])).values()];
    const alunoAtual = seletorAluno.value || alunos[0]?.cpf || "";

    seletorAluno.innerHTML = alunos.length
        ? alunos.map((aluno) => `<option value="${escaparHtml(aluno.cpf)}">${escaparHtml(aluno.nome)} - ${escaparHtml(aluno.cpf)}</option>`).join("")
        : `<option value="">Nenhum aluno registrado</option>`;
    seletorAluno.value = alunos.some((aluno) => aluno.cpf === alunoAtual) ? alunoAtual : alunos[0]?.cpf || "";

    const aluno = alunos.find((item) => item.cpf === seletorAluno.value);
    const registrosDoAluno = registros.filter((registro) => registro.cpf === seletorAluno.value);

    alunoSelecionado.textContent = aluno?.nome || "Nenhum aluno selecionado";
    cpfSelecionado.textContent = aluno?.cpf || "Registre um aluno para consultar o histórico.";
    listaRegistros.innerHTML = "";
    estadoVazio.hidden = registrosDoAluno.length > 0;

    [...registrosDoAluno].reverse().forEach((registro) => {
        const linha = document.createElement("tr");
        linha.innerHTML = `
            <td>${formatarData(registro.data)}</td>
            <td>${escaparHtml(registro.horario)}</td>
        `;
        listaRegistros.appendChild(linha);
    });
}

seletorAluno.addEventListener("change", atualizarRelatorio);

document.querySelector("#btn-imprimir").addEventListener("click", () => {
    atualizarRelatorio();
    window.print();
});

atualizarRelatorio();
