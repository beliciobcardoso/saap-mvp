# PRD - Fluxo da Recepcionista

## 1. Visão Geral
Este documento descreve as funcionalidades e responsabilidades do perfil de Recepcionista no sistema SAAP. O foco deste perfil é a triagem inicial, gestão administrativa de agendas, recepção e fluxo de pacientes, garantindo que os médicos tenham a fila organizada para o atendimento.

## 2. Autenticação e Controle de Acesso
- **Acesso ao Sistema:** Entrada realizada via usuário e senha.
- **Privacidade e LGPD:** A recepcionista **não tem acesso** aos prontuários clínicos dos pacientes. O acesso é estritamente limitado aos dados cadastrais e administrativos necessários para o atendimento na recepção (nome, contato, agendamentos).

## 3. Dashboard e Monitoramento da Clínica
A recepcionista possui uma visão panorâmica (dashboard) em tempo real do funcionamento da clínica:
- **Painel de Médicos:** Visualização de quais médicos estão escalados/agendados para atender no dia.
- **Status do Fluxo Médico:** Acompanhamento em tempo real das ações do médico, incluindo:
  - Médico iniciou o atendimento do dia (plantão).
  - Médico chamou o próximo da fila.
  - Consulta iniciada.
  - Consulta finalizada.
  - Médico encerrou o atendimento do dia.

## 4. Gestão de Agendas (Médicos e Consultas)
- **Criação de Agenda:** Pode criar a grade/agenda de disponibilidade dos médicos.
- **Alteração de Agenda:** Pode editar a agenda do médico (ex: em caso de falta ou imprevisto). *Regra de Negócio:* Esta alteração só é permitida **antes** de o médico iniciar seu atendimento no sistema.
- **Agendamento de Pacientes:** Criação de novos agendamentos vinculando pacientes aos horários disponíveis na agenda médica.

## 5. Gestão de Pacientes e Recepção
- **Cadastro:** Cadastrar novos pacientes no sistema.
- **Busca e Listagem:** Listar e pesquisar pacientes na base de dados.
- **Visualização e Atualização:** Visualizar perfil do paciente (somente dados permitidos) e atualizar informações cadastrais.
- **Check-in:** Ação fundamental da recepção. Ao chegar na clínica, a recepcionista realiza o "check-in" do paciente, transferindo-o do status de 'agendado' para 'aguardando' (colocando-o na sala de espera sistêmica que o médico visualiza).
