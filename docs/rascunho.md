# Rascunho de Análise Orientada a Objetos para o Projeto SAAP

### **Por que essa estrutura é "OO"?**

* **Abstração e Especialização**: Ao utilizarmos o `ProfessionalRole`, estamos aplicando o conceito de especialização. Embora todos no sistema possam ser "Usuários" ou "Profissionais", o comportamento do objeto muda conforme seu papel: um `PRACTITIONER` possui uma agenda vinculada, enquanto um `RECEPTIONIST` possui métodos para gerenciar agendas de terceiros.
* **Encapsulamento de Regras de Negócio**: A lógica de transição de estados do agendamento (ex: de `PENDING` para `CONFIRMED`) fica encapsulada na entidade `Appointment`. Isso evita que o status seja alterado de forma inconsistente por agentes externos, garantindo a integridade dos dados.
* **Polimorfismo de Relacionamento**: A classe `Service` é tratada de forma polimórfica: ela pode ser associada a diferentes tipos de profissionais (médicos, enfermeiros, assistentes) através de uma relação N:N, permitindo que o sistema cresça sem precisar alterar a estrutura base de agendamento.
* **Identificação de Entidades vs. Objetos de Valor**: Identificamos claramente que `Patient` e `Professional` são **Entidades** (possuem identidade única e ciclo de vida), enquanto o `AppointmentStatus` funciona como um objeto de valor que define o estado do sistema em um dado momento.
* **Baixo Acoplamento via Clean Architecture**: Ao definir o schema do Prisma baseado em nossas classes de domínio, garantimos que a lógica de negócio ("O que o sistema faz") esteja separada da tecnologia de persistência ("Como o sistema armazena"), facilitando a manutenção futura.

---

### **Reflexão para o Projeto**
Essa estrutura resolve os problemas de "conflito de horários" e "falta de controle" mencionados na visão do problema, pois utiliza objetos inteligentes para validar as regras de negócio antes mesmo de chegarem ao banco de dados.

