/* Configuração dinâmica da API do MarianoSRA */
const isLocal = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
const isSpringPort = window.location.port === "8080";
const API_BASE_URL = (isLocal && isSpringPort)
    ? "http://localhost:8080/api"
    : "/api";

window.APP_CONFIG = {
    API_BASE_URL,
    // Se estiver rodando no Spring Boot local (porta 8080), usa URL completa.
    // Na Vercel ou na nuvem (HTTPS), usa endpoint relativo "/api/frequencia/registrar".
    API_URL: `${API_BASE_URL}/frequencia/registrar`
};

