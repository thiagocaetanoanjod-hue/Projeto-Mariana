# Projeto-Mariana
Um sistema de registro de alunos atrasados.

## Configuração da API

Para compartilhar registros entre dispositivos, defina `API_URL` em `Proto/config.js` com a URL do endpoint de registros. O endpoint deve aceitar `POST` com um registro JSON e responder a `GET` com uma lista JSON de registros. Sem URL configurada, o sistema opera somente com o cache local do navegador.
