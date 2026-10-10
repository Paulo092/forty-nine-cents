# 49 Centavos

App Android nativo de controle de gastos pessoais: registro manual rápido, captura automática
a partir das notificações dos apps de banco e exportação em CSV para o Google Sheets.

## Funcionalidades (v0.1)

- **Lançamento rápido** – valor estilo caixa eletrônico (`4000` → R$ 40,00), etiqueta, despesa/receita,
  categoria sugerida automaticamente pela descrição e data (com atalho "Hoje").
- **Captura automática** – `NotificationListenerService` lê notificações de bancos (Nubank, Itaú, Inter, C6,
  Bradesco, Santander, BB, Caixa, PicPay, Mercado Pago…) e extrai valor, estabelecimento, tipo e data/hora.
  A leitura de SMS é opcional (desligada por padrão) e só aceita remetentes com cara de banco.
- **Revisão** – o que foi capturado entra como *pendente*; o usuário confirma, troca a categoria ou descarta.
  Há um modo "Simular notificação" para testar o parser colando um texto.
- **Extrato** – navegação por mês, totais de entradas/saídas/saldo, agrupamento por dia, deslizar para excluir
  (com desfazer).
- **Apelidos** – nomes amigáveis para identificadores feios ("PAG*JOSEDASILVA" → "Feira do Zé"). Toque em
  um lançamento no Extrato ou na Revisão, ou gerencie em *Extrato → ícone de etiqueta*. Pode valer para o
  identificador exato ou para todo identificador que *contenha* um trecho ("UBER *TRIP"). O lançamento guarda
  o original (o Extrato mostra-o em letra pequena abaixo do apelido); o apelido é aplicado na exibição,
  então editar/remover vale para o histórico inteiro.
- **Exportação CSV** – do mês ou completo, compartilhada via `Intent` (Drive, Sheets, e-mail…).

## Arquitetura

MVVM + camadas simples, Kotlin, Jetpack Compose (Material 3), Coroutines/Flow e Room.

```
app/src/main/java/com/fortyninecents/
├── domain/          Modelos (Transaction, Category) e CategoryClassifier — Kotlin puro
├── data/
│   ├── local/       Room: entidade, DAO, conversores, banco
│   └── repository/  TransactionRepository (Flow para a UI)
├── notification/    NotificationParser (regex, Kotlin puro), BankApps, NotificationProcessor,
│                    BankNotificationListenerService
├── export/          CsvBuilder (Kotlin puro) e CsvExporter (FileProvider + Intent)
├── di/              AppContainer (injeção manual)
├── util/            Money (formatação/conversão em centavos), utilitários de texto
└── ui/              Tema, componentes, telas (home, transactions, review) e navegação
```

Decisões principais:

- **Valores em centavos (`Long`)** – nada de `Double` para dinheiro.
- **Capturas como PENDENTE** – um parser de regex vai errar às vezes; a revisão evita sujar o histórico.
- **Deduplicação** – índice único em `dedup_key` (hash de app + texto + minuto) porque bancos re-postam
  a mesma notificação.
- **Lógica sem Android** – parser, classificador, CSV e `Money` são Kotlin puro e cobertos por testes JVM.
- **DI manual** – suficiente para o tamanho atual; o `AppContainer` migra para Hilt sem mudar as classes.

## Como rodar

1. Abra a pasta no Android Studio (Narwhal 2025.1.3 ou mais recente) e deixe o Gradle sincronizar.
   Se o Studio sugerir atualizar versões do AGP/Kotlin/bibliotecas, pode aceitar.
2. Confira o JDK do Gradle: *Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK*
   deve ser **17 ou 21** (o "jbr-21" embutido no Studio serve). O Gradle 8.14 não roda em Java 25 e falha com
   "Incompatible Gradle JVM version".
3. Rode o app (`minSdk 26`, `targetSdk 36`).
4. Na tela principal toque em **Verificar notificações → Permitir acesso** e habilite o 49 Centavos.

> **Android 13+ com APK instalado fora da Play Store:** o sistema bloqueia o acesso a notificações
> ("configuração restrita"). Vá em *Configurações > Apps > 49 Centavos > ⋮ > Permitir configurações restritas*
> e depois conceda o acesso. Instalando pelo Android Studio isso normalmente não acontece.

Testes unitários:

```bash
./gradlew testDebugUnitTest
```

## Segurança e privacidade

- **Sem permissão de INTERNET**: os dados só saem do aparelho quando o usuário exporta o CSV. Ao adicionar
  bibliotecas, confira o *Merged Manifest* para garantir que nenhuma trouxe essa permissão.
- **Só apps conhecidos**: notificações de apps fora de `BankApps` são descartadas na entrada; o texto bruto
  nunca é salvo nem logado (só um hash para deduplicação).
- **SMS opcional**: qualquer pessoa pode enviar SMS, então a captura vem desligada e, quando ligada, só aceita
  códigos curtos (ex.: 27888) ou remetentes com nome de banco (`SmsSenderFilter`). Tudo ainda passa pela revisão.
- **CSV injection**: descrições que começam com `= + - @` ganham um `'` na frente para não virarem fórmula
  no Sheets/Excel.
- **Backup**: o banco de lançamentos é excluído do backup do Google; a transferência direta entre aparelhos
  (troca de celular) continua levando os dados. Regras em `res/xml/data_extraction_rules.xml` e `backup_rules.xml`.

## CSV

Colunas: `id, data, hora, descricao, categoria, tipo, valor, origem, identificador_original`
(`descricao` traz o apelido, quando houver). Separador vírgula, campos entre aspas,
datas `dd/MM/yyyy` e valores com vírgula decimal (`-40,00`) — formato que o Google Sheets em português
reconhece como número. Despesas saem negativas, receitas positivas. O arquivo tem BOM UTF-8 para o Excel
abrir os acentos corretamente. Textos que começariam com `=`, `+`, `-` ou `@` são prefixados com `'`
(proteção contra fórmulas maliciosas).

## Adicionando um banco

1. Inclua o package do app em `notification/BankApps.kt`.
2. Use "Simular notificação" com um texto real do banco. Se não reconhecer, adicione um caso em
   `NotificationParserTest` e ajuste os padrões em `NotificationParser`.

## Próximos passos sugeridos

- Notificação local "novo gasto capturado" com atalho para revisar.
- Edição de lançamentos existentes e busca no extrato.
- Gráficos por categoria e orçamento mensal.
- Aprender categoria a partir das correções do usuário.
- Backup/sincronização (ex.: Google Drive) e exportação direta para uma planilha via API do Sheets.
