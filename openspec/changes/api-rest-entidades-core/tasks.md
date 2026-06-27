## 1. Validador Customizado de CPF

- [x] 1.1 Criar a anotação `@CPF` no pacote `infrastructure.web.validation`.
- [x] 1.2 Criar a classe `CpfValidator` que implementa `ConstraintValidator<CPF, String>` para validação matemática de dígitos verificadores de CPF.

## 2. Camada de Aplicação (Use Cases)

- [x] 2.1 Criar os Use Cases de CRUD para Usuário (`CreateUserUseCase`, `FindUserByIdUseCase`, `ListActiveUsersUseCase`, `UpdateUserUseCase`, `DeactivateUserUseCase`).
- [x] 2.2 Criar os Use Cases de CRUD para Paciente (`CreatePatientUseCase`, `FindPatientByIdUseCase`, `ListActivePatientsUseCase`, `UpdatePatientUseCase`, `DeactivatePatientUseCase`).
- [x] 2.3 Criar os Use Cases de CRUD para Profissional (`CreateProfessionalUseCase`, `FindProfessionalByIdUseCase`, `ListActiveProfessionalsUseCase`, `UpdateProfessionalUseCase`, `DeactivateProfessionalUseCase`).
- [x] 2.4 Criar os Use Cases de CRUD para Serviço (`CreateServiceUseCase`, `FindServiceByIdUseCase`, `ListActiveServicesUseCase`, `UpdateServiceUseCase`, `DeactivateServiceUseCase`).

## 3. Camada Web (DTOs e Mappers)

- [x] 3.1 Criar DTOs de Request e Response para todas as 4 entidades (ex: `UserRequest`, `UserResponse`, `PatientRequest`, etc.), contendo anotações de validação do Jakarta e `@CPF` para paciente.
- [x] 3.2 Criar a interface de mapeamento MapStruct `WebMapper` no pacote `infrastructure.web.mapper` para converter entre DTOs e modelos de domínio puro.

## 4. Camada de Apresentação (REST Controllers)

- [x] 4.1 Criar `UserController` exposto em `/api/users`.
- [x] 4.2 Criar `PatientController` exposto em `/api/patients`.
- [x] 4.3 Criar `ProfessionalController` exposto em `/api/professionals`.
- [x] 4.4 Criar `ServiceController` exposto em `/api/services`.

## 5. Tratamento de Exceções Global

- [x] 5.1 Criar a classe de payload `ErrorResponse` no pacote `infrastructure.web.exception`.
- [x] 5.2 Criar a classe `GlobalExceptionHandler` anotada com `@RestControllerAdvice` tratando validações (`MethodArgumentNotValidException`), duplicidades (`DataIntegrityViolationException`) e not found.

## 6. Testes e Validação

- [x] 6.1 Criar testes unitários para a classe `CpfValidator`.
- [x] 6.2 Criar testes de integração usando `@WebMvcTest` ou `@SpringBootTest` com `MockMvc` para as rotas HTTP das 4 entidades, validando fluxos de sucesso, erros de validação e conflito de duplicidade.
