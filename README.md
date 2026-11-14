# Treino (`com.daniel.treino`)

[![Gerar e Publicar APK](https://github.com/constDevDaniel/app-treino/actions/workflows/build-apk.yml/badge.svg)](https://github.com/constDevDaniel/app-treino/actions/workflows/build-apk.yml)
[![Baixar APK](https://img.shields.io/badge/Download-treino.apk-6C5CE7?style=for-the-badge&logo=android&logoColor=white)](https://github.com/constDevDaniel/app-treino/releases/latest/download/treino.apk)

Aplicativo Android nativo para acompanhamento de treinos de musculação, controle de séries e cargas, agenda semanal, cronômetro de descanso integrado à **NowBar** (One UI) e relatórios de evolução.

## Download do APK

Sempre que uma nova atualização é enviada para a branch `main`, o GitHub Actions compila automaticamente o aplicativo e atualiza a página de Releases:

- **Download direto do APK mais recente:** [treino.apk](https://github.com/constDevDaniel/app-treino/releases/latest/download/treino.apk)
- **Página de Releases:** [Releases no GitHub](https://github.com/constDevDaniel/app-treino/releases)

## Funcionalidades

- **Hoje**: Visão rápida da semana, treino agendado do dia, retomada de treino em andamento e início rápido.
- **Treinos**: Criação e edição de fichas de treino com ícones personalizados, catálogo de exercícios com mesclagem automática ao renomear e importação de histórico via planilha Excel (`.xlsx` / `.csv`).
- **Sessão de Treino**: Preenchimento automático com base na última sessão, barra de descanso com contagem regressiva, notificação ao vivo na **NowBar** e alerta sonoro/vibração.
- **Agenda**: Definição de treino fixo ou descanso para cada dia da semana.
- **Relatórios e Histórico**: Gráficos de evolução de carga por exercício e volume semanal (Vico Charts), recordes pessoais e histórico completo de sessões.
- **Backup**: Exportação e restauração completa em JSON com suporte a backup automático via Storage Access Framework / Google Drive.

## Stack Tecnológica

- **Linguagem**: Kotlin 2.4.21 (JDK 17)
- **Build**: AGP 9.5.1, Gradle 9.9.1
- **UI**: Jetpack Compose (Material 3, Navigation Compose, tipografia Barlow / Barlow Condensed)
- **Persistência**: Room 2.8.5 (SQLite + KSP + Coroutines Flow)
- **Gráficos**: Vico Charts (`compose-m3` 3.3.1)
- **SDK**: `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`
