# ⚖️ Bolso+Leve

**Bolso+Leve** é um aplicativo Android nativo moderno e completo para acompanhamento de tratamento de perda de peso e controle de medicação semanal (como Mounjaro, Ozempic, Wegovy, entre outros).

O app une o acompanhamento clínico, evolução da curva de peso, gestão de doses e controle financeiro de investimento em um só lugar, com visual moderno em Material Design 3 e Material You.

---

## 📱 Funcionalidades Principais

- **Painel Inicial Inteligente**:
  - Resumo de peso atual, peso inicial, meta e percentual de progresso.
  - Contagem regressiva da próxima aplicação semanal com confirmação rápida da dose.
  - Próxima pesagem recomendada com guia de boas práticas.
  - Próxima consulta médica com atalho para sincronizar na agenda do dispositivo.
  - Registro rápido de pesagem atual ou pesagens retroativas com data.
  - Mini-gráfico da curva recente de evolução de peso com opção de editar/excluir registros.

- **Medicina & Consultas**:
  - Gestão de medicação ativa e dosagem (mg).
  - Histórico de doses tomadas em aba colapsável com contagem de aplicações.
  - Registro de compra de caixas de medicação com cálculo de custo e geração automática/retroativa de doses.
  - Dados do médico/clínica (CRM, especialidade, endereço com atalho para o Google Maps, telefone com discador rápido e notas médicas).
  - Registro de consultas médicas retroativas integradas ao financeiro.

- **Estatísticas & Finanças**:
  - Gráfico evolutivo completo com projeção e linha de meta.
  - Gráfico de barras de variação semanal líquida (perda vs. ganho).
  - Métricas de taxa de perda semanal e estimativa de data de chegada ao objetivo.
  - Estatísticas financeiras completas: total investido em medicações e consultas, custo por quilo eliminado (R$/kg).

- **Opções & Backup**:
  - Exportação completa de backup em formato JSON estruturado (compartilhamento ou salvamento em arquivo).
  - Restauração de backup JSON.
  - Importação de pesagens via arquivo CSV no formato `Data,peso` (ex: `26/06/2026,109`).
  - Redefinição completa de dados.

- **Sobre o Criador**:
  - Desenvolvido por Rafael Lannes ([rafael-lannes.github.io](https://rafael-lannes.github.io)).

---

## 🛠️ Stack Tecnológica

- **Linguagem**: Kotlin 2.0
- **UI Toolkit**: Jetpack Compose + Material Design 3 (Material You)
- **Arquitetura**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Injeção de Dependências**: Koin
- **Persistência Local**: Room Database + DataStore Preferences
- **Tarefas em Segundo Plano & Notificações**: WorkManager + Notification Channels
- **Gráficos**: Custom Canvas Jetpack Compose

---

## 🚀 Como Executar o Projeto

1. Clone o repositório:
   ```bash
   git clone https://github.com/rafael-lannes/Bolso-leve.git
   ```
2. Abra o projeto no **Android Studio**.
3. Sincronize as dependências do Gradle.
4. Execute em um emulador ou dispositivo físico com Android 8.0+ (API 26+).

---

## 📄 Licença

Distribuído sob a licença MIT. Consulte `LICENSE` para mais informações.
