## 1. Banco de Dados e Migrações

- [x] 1.1 Criar o script de migração Flyway V1__criar_tabelas_core.sql em src/main/resources/db/migration/ para as tabelas usuario, paciente, profissional e servico.

## 2. Modelos de Domínio Puro

- [x] 2.1 Criar enums UserRole e ProfessionalRole em br.com.belloinfo.saap_mvp.domain.valueobject.
- [x] 2.2 Criar a classe de domínio User em br.com.belloinfo.saap_mvp.domain.model.
- [x] 2.3 Criar a classe de domínio Patient em br.com.belloinfo.saap_mvp.domain.model.
- [x] 2.4 Criar a classe de domínio Professional em br.com.belloinfo.saap_mvp.domain.model.
- [x] 2.5 Criar a classe de domínio Service em br.com.belloinfo.saap_mvp.domain.model.

## 3. Interfaces de Repositório de Domínio (Ports)

- [x] 3.1 Criar a interface UserRepository em br.com.belloinfo.saap_mvp.domain.repository.
- [x] 3.2 Criar a interface PatientRepository em br.com.belloinfo.saap_mvp.domain.repository.
- [x] 3.3 Criar a interface ProfessionalRepository em br.com.belloinfo.saap_mvp.domain.repository.
- [x] 3.4 Criar a interface ServiceRepository em br.com.belloinfo.saap_mvp.domain.repository.

## 4. Camada de Persistência (JPA Entities & Mappers)

- [x] 4.1 Criar a entidade JPA UserEntity em br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.
- [x] 4.2 Criar a entidade JPA PatientEntity em br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.
- [x] 4.3 Criar a entidade JPA ProfessionalEntity em br.com.belloinfo.saap_mvp.infrastructure.persistence.entity com tratamento de exclusão lógica (soft delete).
- [x] 4.4 Criar a entidade JPA ServiceEntity em br.com.belloinfo.saap_mvp.infrastructure.persistence.entity com tratamento de exclusão lógica (soft delete).
- [x] 4.5 Criar as interfaces de mapeamento do MapStruct para converter entre Models e Entities.

## 5. Implementação dos Repositórios JPA (Adapters)

- [x] 5.1 Criar as interfaces do Spring Data JPA (JpaUserRepository, JpaPatientRepository, JpaProfessionalRepository, JpaServiceRepository).
- [x] 5.2 Implementar os adaptadores de repositório que estendem as interfaces de domínio (Ports) e encapsulam os repositories JPA.

## 6. Testes e Validação

- [x] 6.1 Criar testes unitários para a validação das classes de domínio puro.
- [x] 6.2 Criar testes de integração com Testcontainers PostgreSQL para validar a persistência e o comportamento de soft delete dos repositories.

## 7. Ajustes: CPF e SUS no Paciente

- [x] 7.1 Alterar o script de migração Flyway V1__criar_tabelas_core.sql para incluir as colunas cpf e sus_number na tabela paciente.
- [x] 7.2 Adicionar os campos cpf e susNumber na classe de domínio Patient.
- [x] 7.3 Adicionar as colunas cpf e susNumber na entidade JPA PatientEntity.
- [x] 7.4 Atualizar testes unitários e de integração para contemplar os novos campos.
