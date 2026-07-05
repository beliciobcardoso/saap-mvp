# PRD - Fluxo de Atendimento Médico

## 1. Visão Geral
Este documento descreve a jornada completa do profissional de saúde (médico) dentro do sistema SAAP, desde a configuração da sua disponibilidade até a finalização de consultas e gestão de emergências. O foco central é automatizar a fila de espera e reduzir a fricção na tomada de decisão do médico.

## 2. Jornada do Médico (Passo a Passo)
1. **Configuração de Agenda:** O médico cria e define sua agenda de disponibilidade no sistema.
2. **Início do Dia:** O médico inicia seu dia de trabalho e **seleciona o consultório** onde irá atender.
3. **Visão Geral do Plantão:** Acesso à tela principal, onde visualiza a agenda do dia e a **sala de espera**.
4. **Chamada de Paciente:** O médico chama o paciente da sala de espera através da ação **"Chamar Próximo"**.
5. **Confirmação e Contexto:** O médico confirma a entrada do paciente no consultório. O sistema exibe todas as informações do paciente de forma visível na tela.
6. **Prontuário:** 
    - Médico abre o prontuário.
    - Médico preenche as informações clínicas.
7. **Encerramento da Consulta:** Médico finaliza a consulta do paciente atual.
8. **Finalização do Turno:** Ao final do expediente (ou período), o médico finaliza o atendimento do dia.

## 3. Regras de Fila e Prioridade
O sistema assume a responsabilidade de ordenar e entregar os pacientes, retirando do médico a carga de escolher quem será o próximo.
- A lista de espera é visualizada pelo médico de forma **ordenada por prioridade** (ex: P1 > P2 > P3).
- **Ação única:** O médico clica exclusivamente no botão **"Chamar Próximo"**.
- **Entrega sistêmica:** O sistema entrega automaticamente o paciente de maior prioridade elegível naquele momento. O médico **não escolhe** manualmente quem chamar.

## 4. Fluxo de Emergências e Encaixes
Mesmo que a lista da sala de espera esteja vazia (ou em andamento normal), emergências podem surgir e o médico deve estar habilitado para atendê-las.
1. **Comunicação:** A recepcionista informa o médico sobre o caso de urgência/emergência.
2. **Aceite:** O médico aceita o encaixe.
3. **Fila e Visualização:** O paciente é encaixado no final da fila da sala de espera.
4. **Sinalização Visual:** O paciente urgente recebe uma tag **"EMERG"** acompanhada de **fundo vermelho** para destaque visual absoluto.
5. **Atendimento:** O médico clica em "Chamar Próximo" e o paciente urgente é atendido assim que chegar a sua vez na fila.
