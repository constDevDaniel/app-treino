# Treino (`com.daniel.treino`)

Aplicativo Android nativo para acompanhamento de treinos de musculação, controle de séries e cargas, agenda semanal, cronômetro de descanso integrado à **NowBar** (One UI) e relatórios de evolução.

## Funcionalidades

- **Hoje**: Visão rápida da semana, treino agendado do dia, retomada de treino em andamento e início rápido.
- **Treinos**: Criação e edição de fichas de treino com ícones personalizados, catálogo de exercícios com mesclagem automática ao renomear e importação de histórico via planilha Excel (`.xlsx` / `.csv`).
- **Sessão de Treino**: Preenchimento automático com base na última sessão, barra de descanso com contagem regressiva, notificação ao vivo na **NowBar** e alerta sonoro/vibração.
- **Agenda**: Definição de treino fixo ou descanso para cada dia da semana.
- **Relatórios e Histórico**: Gráficos de evolução de carga por exercício e volume semanal (Vico Charts), recordes pessoais e histórico completo de sessões.
- **Backup**: Exportação e restauração completa em JSON com suporte a backup automático via Storage Access Framework / Google Drive.

## Stack Tecnológica

- **Linguagem**: Kotlin 2.1.0 (JDK 17)
- **UI**: Jetpack Compose (Material 3, Navigation Compose, tipografia Barlow / Barlow Condensed)
- ** Persistência**: Room 2.6.1 (SQLite + KSP + Coroutines Flow)
- **Gráficos**: Vico Charts (`compose-m3` 2.0.0)
- **SDK**: `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`
