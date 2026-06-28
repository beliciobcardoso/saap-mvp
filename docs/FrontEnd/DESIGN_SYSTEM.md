# SAAP — Design System & Frontend Architecture

> Blueprint completo para implementação do frontend Vue.js + TanStack Query.
> Este documento é a fonte única de verdade: qualquer futuro agente pode implementar sem pesquisa adicional.

---

## 0. Bootstrap — Criar a Aplicação

### 0.1 Pré-requisitos

```bash
node --version   # >= 20.x
pnpm --version   # >= 9.x  (instalar: npm i -g pnpm)
```

### 0.2 Scaffold com Vite

Executar **dentro de** `/home/bello/Projetos/saap-mvp/` para manter frontend e backend no mesmo repositório:

```bash
# Cria o app Vue 3 + TypeScript dentro de saap-mvp/frontend/
pnpm create vite frontend -- --template vue-ts
cd frontend
pnpm install
```

Estrutura gerada pelo Vite (antes de customizar):
```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── package.json
├── public/
└── src/
    ├── main.ts
    ├── App.vue
    ├── style.css
    ├── vite-env.d.ts
    └── components/
        └── HelloWorld.vue   ← deletar
```

### 0.3 Instalar Dependências

```bash
# Runtime
pnpm add \
  @tanstack/vue-query \
  vue-router \
  pinia \
  axios \
  vee-validate \
  yup \
  @vueuse/core \
  date-fns \
  date-fns-tz \
  @phosphor-icons/vue \
  vue-sonner \
  floating-vue \
  jwt-decode

# Dev
pnpm add -D \
  tailwindcss \
  postcss \
  autoprefixer \
  @tailwindcss/forms \
  vitest \
  @vue/test-utils \
  jsdom \
  @vitejs/plugin-vue
```

### 0.4 Inicializar Tailwind CSS

```bash
pnpx tailwindcss init -p
# Gera: tailwind.config.js + postcss.config.js
```

Atualizar `tailwind.config.js` conforme seção 15 deste documento.

### 0.5 Configurar `vite.config.ts`

Substituir o conteúdo gerado pelo scaffold pelo da seção 15 deste documento (alias `@` + proxy `/api`).

### 0.6 Configurar `tsconfig.app.json`

Adicionar path alias:

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### 0.7 Criar Arquivo de Ambiente

```bash
# frontend/.env.development
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.development
echo "VITE_API_BASE_URL=https://api.saap.belloinfo.com.br" > .env.production
```

### 0.8 Limpar Boilerplate do Vite

```bash
# Deletar arquivos desnecessários gerados pelo scaffold
rm src/components/HelloWorld.vue
rm src/style.css      # será substituído pelos arquivos em src/styles/
rm src/assets/vue.svg
```

### 0.9 Scripts `package.json`

```json
{
  "scripts": {
    "dev":     "vite",
    "build":   "vue-tsc && vite build",
    "preview": "vite preview",
    "test":    "vitest",
    "test:ui": "vitest --ui",
    "typecheck": "vue-tsc --noEmit"
  }
}
```

### 0.10 Rodar em Desenvolvimento

```bash
# Com o backend Spring Boot rodando na porta 8080:
pnpm dev
# App disponível em: http://localhost:5173
```

---

## 1. Direção Estética

### 1.1 Conceito Visual: "Clínica Viva"

O SAAP serve recepcionistas, médicos e administradores em clínicas brasileiras — pessoas sob pressão real, trabalhando turnos longos, tomando decisões críticas de prioridade. A interface deve ser **rápida de ler, impossível de mal-interpretar e agradável de usar no dia a dia**.

**Não é**: Branco frio de hospital. Não é azul corporativo genérico. Não é dashboard SaaS americano.

**É**: Brasileiro, caloroso, preciso. Grid disciplinado com personalidade. Informação hierarquizada com confiança.

### 1.2 Paleta de Cores

```css
:root {
  /* === BACKGROUNDS === */
  --color-bg-base:       #F8F7F4;   /* creme quente, não branco puro */
  --color-bg-surface:    #FFFFFF;   /* cards e panels */
  --color-bg-sunken:     #EFF0ED;   /* áreas recuadas, inputs */
  --color-bg-overlay:    #1A2332E6; /* modais, 90% opacidade */

  /* === PRIMÁRIA — Azul Petróleo === */
  --color-primary-950:   #0A1628;
  --color-primary-900:   #0D1F3C;
  --color-primary-800:   #112B52;
  --color-primary-700:   #163C6E;
  --color-primary-600:   #1B4F8E;   /* ações secundárias */
  --color-primary-500:   #2563AB;   /* base primary */
  --color-primary-400:   #3B82D4;   /* hover */
  --color-primary-300:   #60A5F4;   /* disabled text on dark */
  --color-primary-200:   #BFDBFE;
  --color-primary-100:   #EFF6FF;   /* background tinted */
  --color-primary-50:    #F5F9FF;

  /* === ACENTO — Âmbar Brasileiro === */
  --color-accent-600:    #D97706;
  --color-accent-500:    #F59E0B;   /* acento principal */
  --color-accent-400:    #FBBF24;
  --color-accent-100:    #FEF3C7;
  --color-accent-50:     #FFFBEB;

  /* === SEMÂNTICAS === */
  --color-success-700:   #047857;
  --color-success-500:   #10B981;
  --color-success-100:   #D1FAE5;
  --color-success-50:    #ECFDF5;

  --color-warning-700:   #B45309;
  --color-warning-500:   #F59E0B;
  --color-warning-100:   #FEF3C7;

  --color-danger-700:    #B91C1C;
  --color-danger-600:    #DC2626;
  --color-danger-500:    #EF4444;
  --color-danger-100:    #FEE2E2;
  --color-danger-50:     #FFF5F5;

  --color-info-500:      #3B82F6;
  --color-info-100:      #DBEAFE;

  /* === NEUTROS === */
  --color-neutral-950:   #0A0E14;
  --color-neutral-900:   #111827;
  --color-neutral-800:   #1F2937;
  --color-neutral-700:   #374151;
  --color-neutral-600:   #4B5563;
  --color-neutral-500:   #6B7280;
  --color-neutral-400:   #9CA3AF;
  --color-neutral-300:   #D1D5DB;
  --color-neutral-200:   #E5E7EB;
  --color-neutral-100:   #F3F4F6;
  --color-neutral-50:    #F9FAFB;

  /* === PRIORIDADES P1–P5 (core do sistema) === */
  --color-p1-bg:         #FFF1F2;
  --color-p1-border:     #FDA4AF;
  --color-p1-text:       #9F1239;
  --color-p1-badge:      #E11D48;   /* P1 vermelho crítico */

  --color-p2-bg:         #FFF7ED;
  --color-p2-border:     #FDBA74;
  --color-p2-text:       #9A3412;
  --color-p2-badge:      #EA580C;   /* P2 laranja urgente */

  --color-p3-bg:         #FEFCE8;
  --color-p3-border:     #FDE047;
  --color-p3-text:       #713F12;
  --color-p3-badge:      #CA8A04;   /* P3 âmbar moderado */

  --color-p4-bg:         #F0FDF4;
  --color-p4-border:     #86EFAC;
  --color-p4-text:       #14532D;
  --color-p4-badge:      #16A34A;   /* P4 verde baixo */

  --color-p5-bg:         #F8FAFC;
  --color-p5-border:     #CBD5E1;
  --color-p5-text:       #475569;
  --color-p5-badge:      #64748B;   /* P5 cinza padrão */

  /* === STATUS AGENDAMENTO === */
  --status-pending:      #F59E0B;
  --status-pending-resp: #8B5CF6;
  --status-confirmed:    #3B82F6;
  --status-arrived:      #10B981;
  --status-calling:      #F97316;
  --status-in-progress:  #059669;
  --status-completed:    #6B7280;
  --status-cancelled:    #EF4444;
  --status-no-show:      #9CA3AF;
}
```

### 1.3 Tipografia

```css
/* Importar no index.html via Google Fonts */
/* @import url('https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;500&display=swap'); */

:root {
  --font-display:  'Sora', sans-serif;         /* headings, brand */
  --font-body:     'Plus Jakarta Sans', sans-serif; /* tudo mais */
  --font-mono:     'JetBrains Mono', monospace; /* IDs, códigos, tokens */

  /* Escala tipográfica */
  --text-xs:   0.75rem;    /* 12px — labels auxiliares */
  --text-sm:   0.875rem;   /* 14px — secundário */
  --text-base: 1rem;       /* 16px — corpo */
  --text-lg:   1.125rem;   /* 18px — destaque */
  --text-xl:   1.25rem;    /* 20px — subheadings */
  --text-2xl:  1.5rem;     /* 24px — títulos de seção */
  --text-3xl:  1.875rem;   /* 30px — títulos de página */
  --text-4xl:  2.25rem;    /* 36px — display */

  /* Pesos */
  --font-light:    300;
  --font-regular:  400;
  --font-medium:   500;
  --font-semibold: 600;
  --font-bold:     700;

  /* Line heights */
  --leading-tight:   1.25;
  --leading-snug:    1.375;
  --leading-normal:  1.5;
  --leading-relaxed: 1.625;

  /* Letter spacing */
  --tracking-tight:  -0.025em;
  --tracking-normal:  0em;
  --tracking-wide:    0.025em;
  --tracking-widest:  0.1em;  /* labels caps */
}
```

### 1.4 Espaçamento e Grid

```css
:root {
  /* Escala de espaçamento (base 4px) */
  --space-1:   0.25rem;   /* 4px */
  --space-2:   0.5rem;    /* 8px */
  --space-3:   0.75rem;   /* 12px */
  --space-4:   1rem;      /* 16px */
  --space-5:   1.25rem;   /* 20px */
  --space-6:   1.5rem;    /* 24px */
  --space-8:   2rem;      /* 32px */
  --space-10:  2.5rem;    /* 40px */
  --space-12:  3rem;      /* 48px */
  --space-16:  4rem;      /* 64px */
  --space-20:  5rem;      /* 80px */
  --space-24:  6rem;      /* 96px */

  /* Layout */
  --sidebar-width:       240px;
  --sidebar-collapsed:   64px;
  --topbar-height:       64px;
  --content-max-width:   1400px;
  --content-padding:     var(--space-6);

  /* Border radius */
  --radius-sm:   0.25rem;  /* 4px — inputs, badges */
  --radius-md:   0.5rem;   /* 8px — cards pequenos */
  --radius-lg:   0.75rem;  /* 12px — cards principais */
  --radius-xl:   1rem;     /* 16px — modais */
  --radius-2xl:  1.5rem;   /* 24px — containers grandes */
  --radius-full: 9999px;   /* pills, avatars */

  /* Shadows */
  --shadow-xs:  0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-sm:  0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
  --shadow-md:  0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg:  0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  --shadow-xl:  0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
  --shadow-inner: inset 0 2px 4px 0 rgb(0 0 0 / 0.05);

  /* Transitions */
  --duration-fast:    150ms;
  --duration-normal:  250ms;
  --duration-slow:    350ms;
  --ease-out:         cubic-bezier(0.0, 0.0, 0.2, 1);
  --ease-in-out:      cubic-bezier(0.4, 0.0, 0.2, 1);
  --ease-spring:      cubic-bezier(0.175, 0.885, 0.32, 1.275);

  /* Z-index scale */
  --z-below:    -1;
  --z-base:      0;
  --z-raised:    10;
  --z-dropdown:  100;
  --z-sticky:    200;
  --z-overlay:   300;
  --z-modal:     400;
  --z-toast:     500;
  --z-tooltip:   600;
}
```

---

## 2. Stack Tecnológica

### 2.1 Dependências Principais

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "@tanstack/vue-query": "^5.28.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "vee-validate": "^4.12.0",
    "yup": "^1.3.0",
    "@vueuse/core": "^10.9.0",
    "date-fns": "^3.6.0",
    "date-fns-tz": "^3.1.0",
    "@phosphor-icons/vue": "^2.1.7",
    "vue-sonner": "^1.0.3",
    "floating-vue": "^5.2.2"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.2.0",
    "typescript": "^5.4.0",
    "tailwindcss": "^3.4.0",
    "postcss": "^8.4.0",
    "autoprefixer": "^10.4.0",
    "@tailwindcss/forms": "^0.5.7",
    "vitest": "^1.4.0",
    "@vue/test-utils": "^2.4.0"
  }
}
```

### 2.2 Estrutura de Diretórios

```
src/
├── main.ts                     # Bootstrap: Vue app + plugins
├── App.vue                     # Root component
├── router/
│   └── index.ts                # Vue Router com guards de autenticação
├── stores/
│   ├── auth.ts                 # Pinia: JWT, user, role
│   └── ui.ts                   # Pinia: sidebar collapsed, toasts globais
├── api/
│   ├── client.ts               # Axios instance com interceptors JWT
│   ├── auth.ts                 # Funções de API: auth endpoints
│   ├── patients.ts             # Funções de API: patients
│   ├── professionals.ts        # Funções de API: professionals
│   ├── services.ts             # Funções de API: services
│   ├── appointments.ts         # Funções de API: appointments
│   ├── users.ts                # Funções de API: users
│   ├── audit.ts                # Funções de API: audit logs
│   └── types.ts                # TypeScript interfaces de todos os DTOs
├── composables/
│   ├── queries/                # TanStack Query hooks (useQuery)
│   │   ├── usePatients.ts
│   │   ├── useProfessionals.ts
│   │   ├── useServices.ts
│   │   ├── useAppointments.ts
│   │   ├── useUsers.ts
│   │   └── useAuditLogs.ts
│   ├── mutations/              # TanStack Query hooks (useMutation)
│   │   ├── usePatientMutations.ts
│   │   ├── useProfessionalMutations.ts
│   │   ├── useServiceMutations.ts
│   │   ├── useAppointmentMutations.ts
│   │   └── useUserMutations.ts
│   ├── useAuth.ts              # Auth helpers: login, logout, role checks
│   ├── usePermissions.ts       # Checagem de permissão por role
│   └── useQueue.ts             # Lógica de fila com polling
├── layouts/
│   ├── AppLayout.vue           # Layout principal: sidebar + topbar
│   ├── AuthLayout.vue          # Layout de login
│   └── QueueLayout.vue         # Layout full-screen para tela de fila
├── pages/
│   ├── LoginPage.vue
│   ├── DashboardPage.vue
│   ├── AppointmentsPage.vue
│   ├── AppointmentDetailPage.vue
│   ├── PatientsPage.vue
│   ├── ProfessionalsPage.vue
│   ├── ServicesPage.vue
│   ├── UsersPage.vue
│   ├── AuditLogsPage.vue
│   ├── QueuePage.vue           # Tela de fila em tempo real
│   └── PublicConfirmPage.vue   # Confirmação via token de email
├── components/
│   ├── ui/                     # Componentes base (design system atoms)
│   │   ├── AppButton.vue
│   │   ├── AppInput.vue
│   │   ├── AppSelect.vue
│   │   ├── AppBadge.vue
│   │   ├── AppCard.vue
│   │   ├── AppModal.vue
│   │   ├── AppTable.vue
│   │   ├── AppDropdown.vue
│   │   ├── AppTooltip.vue
│   │   ├── AppSkeleton.vue
│   │   ├── AppSpinner.vue
│   │   ├── AppEmptyState.vue
│   │   └── AppErrorBoundary.vue
│   ├── shared/                 # Componentes compartilhados (molecules)
│   │   ├── PriorityBadge.vue
│   │   ├── StatusBadge.vue
│   │   ├── UserAvatar.vue
│   │   ├── DateTimeDisplay.vue
│   │   ├── ConfirmDialog.vue
│   │   ├── FilterBar.vue
│   │   └── SearchInput.vue
│   ├── layout/                 # Componentes de layout
│   │   ├── AppSidebar.vue
│   │   ├── AppTopbar.vue
│   │   └── AppBreadcrumb.vue
│   ├── appointments/           # Feature: agendamentos
│   │   ├── AppointmentCard.vue
│   │   ├── AppointmentList.vue
│   │   ├── BookAppointmentForm.vue
│   │   ├── AppointmentFilters.vue
│   │   ├── AppointmentActions.vue
│   │   └── CheckInForm.vue
│   ├── patients/               # Feature: pacientes
│   │   ├── PatientCard.vue
│   │   ├── PatientForm.vue
│   │   └── PatientSearch.vue
│   ├── professionals/          # Feature: profissionais
│   │   ├── ProfessionalCard.vue
│   │   └── ProfessionalForm.vue
│   ├── queue/                  # Feature: fila de atendimento
│   │   ├── QueueCard.vue
│   │   ├── QueueStats.vue
│   │   └── CallNextButton.vue
│   └── dashboard/              # Feature: dashboard
│       ├── StatCard.vue
│       ├── TodaySchedule.vue
│       └── QueuePreview.vue
└── styles/
    ├── tokens.css              # CSS custom properties (acima)
    ├── typography.css          # Classes tipográficas globais
    ├── animations.css          # Keyframes globais
    └── global.css              # Reset + base styles
```

---

## 3. Tipos TypeScript (DTOs)

```typescript
// src/api/types.ts
// Espelha exatamente os DTOs da API Spring Boot

// ─── ENUMS ─────────────────────────────────────────────

export type AppointmentStatus =
  | 'PENDING'
  | 'PENDING_RESPONSE'
  | 'CONFIRMED'
  | 'ARRIVED'
  | 'CALLING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export type PriorityLevel = 'P1' | 'P2' | 'P3' | 'P4' | 'P5'

export type WaitlistStatus = 'WAITING' | 'OFFERED' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED'

export type UserRole = 'ADMIN' | 'RECEPTIONIST' | 'PROFESSIONAL' | 'ASSISTANT' | 'PATIENT'

export type ProfessionalRole = 'PROFESSIONAL' | 'ASSISTANT'

export type PaymentMethod = 'PIX' | 'DINHEIRO' | 'CARTAO' | 'CHEQUE'

// ─── ENTIDADES (Response DTOs) ─────────────────────────

export interface PatientResponse {
  id: string             // UUID
  name: string
  cpf: string
  susNumber: string | null
  email: string | null
  phone: string
  birthDate: string      // ISO date "YYYY-MM-DD"
  active: boolean
}

export interface ProfessionalResponse {
  id: string
  name: string
  email: string
  phone: string
  registrationNumber: string
  role: ProfessionalRole
  active: boolean
}

export interface ServiceResponse {
  id: string
  name: string
  description: string | null
  durationMinutes: number
  price: number
  active: boolean
}

export interface UserResponse {
  id: string
  email: string
  role: UserRole
  active: boolean
}

export interface AppointmentResponse {
  id: string
  patient: PatientResponse
  professional: ProfessionalResponse
  service: ServiceResponse
  dateTime: string                    // ISO datetime
  status: AppointmentStatus
  paymentMethod: PaymentMethod
  declaredPriority: PriorityLevel | null
  verifiedPriority: PriorityLevel | null
  priorityScore: number | null        // calc: level * 10^12 + checkInTimestamp
  cancellationReason: string | null
  followUpRequired: boolean
  followUpSent: boolean
  checkInAt: string | null
  checkInNotes: string | null
  verifiedBy: string | null           // UUID do profissional
  createdAt: string
  updatedAt: string
}

export interface WaitlistEntryResponse {
  id: string
  patient: PatientResponse
  professional: ProfessionalResponse
  service: ServiceResponse
  preferredDate: string
  status: WaitlistStatus
  position: number                    // posição na fila FIFO
  active: boolean
  offerExpiresAt: string | null
  createdAt: string
}

export interface AuditLogResponse {
  id: string
  timestamp: string
  userId: string
  action: string
  appointmentId: string | null
  recursoId: string
  recursoTipo: string
  ipAddress: string
}

// ─── TOKENS AUTH ───────────────────────────────────────

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthTokenResponse {
  token: string
  type: 'Bearer'
  expiresIn: number  // milliseconds
}

// ─── REQUEST DTOs ──────────────────────────────────────

export interface PatientRequest {
  name: string
  cpf: string
  susNumber?: string
  email?: string
  phone: string
  birthDate: string  // "YYYY-MM-DD"
}

export interface ProfessionalRequest {
  name: string
  email: string
  phone: string
  registrationNumber: string
  role: ProfessionalRole
}

export interface ServiceRequest {
  name: string
  description?: string
  durationMinutes: number
  price: number
}

export interface UserRequest {
  email: string
  password: string
  role: UserRole
}

export interface BookAppointmentRequest {
  patientId: string
  professionalId: string
  serviceId: string
  dateTime: string         // ISO datetime "YYYY-MM-DDTHH:mm:ss"
  paymentMethod: PaymentMethod
  declaredPriority?: PriorityLevel
}

export interface CancelAppointmentRequest {
  reason: string
}

export interface CheckInRequest {
  verifiedLevel: PriorityLevel
  verifiedBy: string      // UUID do profissional
  notes?: string
}

// ─── FILTROS DE QUERY ──────────────────────────────────

export interface AppointmentFilters {
  professionalId?: string
  patientId?: string
  startDate?: string
  endDate?: string
  status?: AppointmentStatus
}

export interface AuditLogFilters {
  userId?: string
  action?: string
  recursoTipo?: string
  startDate?: string
  endDate?: string
}

// ─── PAGINAÇÃO (futuro) ────────────────────────────────

export interface PageRequest {
  page?: number    // 0-based
  size?: number    // default 20
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // current page
  size: number
}
```

---

## 4. Camada de API (Axios)

### 4.1 Cliente HTTP

```typescript
// src/api/client.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15_000,
})

// Injeta token JWT em todas as requisições
apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

// Trata 401 (token expirado)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

// Helper para extrair mensagem de erro da API
export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message
      || error.response?.data?.error
      || error.message
      || 'Erro desconhecido'
  }
  return 'Erro inesperado'
}
```

### 4.2 Funções de API por Recurso

```typescript
// src/api/auth.ts
import { apiClient } from './client'
import type { LoginRequest, AuthTokenResponse } from './types'

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthTokenResponse>('/auth/login', data).then(r => r.data),
}

// src/api/patients.ts
import { apiClient } from './client'
import type { PatientResponse, PatientRequest } from './types'

export const patientsApi = {
  list: () =>
    apiClient.get<PatientResponse[]>('/patients').then(r => r.data),
  get: (id: string) =>
    apiClient.get<PatientResponse>(`/patients/${id}`).then(r => r.data),
  create: (data: PatientRequest) =>
    apiClient.post<PatientResponse>('/patients', data).then(r => r.data),
  update: (id: string, data: PatientRequest) =>
    apiClient.put<PatientResponse>(`/patients/${id}`, data).then(r => r.data),
  deactivate: (id: string) =>
    apiClient.delete(`/patients/${id}`),
}

// src/api/appointments.ts
import { apiClient } from './client'
import type {
  AppointmentResponse, BookAppointmentRequest,
  CancelAppointmentRequest, CheckInRequest, AppointmentFilters
} from './types'

export const appointmentsApi = {
  list: (filters?: AppointmentFilters) =>
    apiClient.get<AppointmentResponse[]>('/appointments', { params: filters }).then(r => r.data),
  get: (id: string) =>
    apiClient.get<AppointmentResponse>(`/appointments/${id}`).then(r => r.data),
  book: (data: BookAppointmentRequest) =>
    apiClient.post<AppointmentResponse>('/appointments', data).then(r => r.data),
  confirm: (id: string) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/confirm`).then(r => r.data),
  cancel: (id: string, data: CancelAppointmentRequest) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/cancel`, data).then(r => r.data),
  checkIn: (id: string, data: CheckInRequest) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/check-in`, data).then(r => r.data),
  start: (id: string) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/start`).then(r => r.data),
  callNext: (id: string) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/call-next`).then(r => r.data),
  complete: (id: string) =>
    apiClient.put<AppointmentResponse>(`/appointments/${id}/complete`).then(r => r.data),
}

// src/api/audit.ts
import { apiClient } from './client'
import type { AuditLogResponse, AuditLogFilters } from './types'

export const auditApi = {
  list: (filters?: AuditLogFilters) =>
    apiClient.get<AuditLogResponse[]>('/audit-logs', { params: filters }).then(r => r.data),
}
```

---

## 5. TanStack Query — Query Keys e Composables

### 5.1 Query Keys (centralizados)

```typescript
// src/composables/queries/queryKeys.ts
// Query keys únicas, type-safe e hierárquicas

export const queryKeys = {
  // Patients
  patients: {
    all: ['patients'] as const,
    lists: () => [...queryKeys.patients.all, 'list'] as const,
    list: () => [...queryKeys.patients.lists()] as const,
    details: () => [...queryKeys.patients.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.patients.details(), id] as const,
  },

  // Professionals
  professionals: {
    all: ['professionals'] as const,
    list: () => [...queryKeys.professionals.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.professionals.all, 'detail', id] as const,
  },

  // Services
  services: {
    all: ['services'] as const,
    list: () => [...queryKeys.services.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.services.all, 'detail', id] as const,
  },

  // Users
  users: {
    all: ['users'] as const,
    list: () => [...queryKeys.users.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.users.all, 'detail', id] as const,
  },

  // Appointments
  appointments: {
    all: ['appointments'] as const,
    lists: () => [...queryKeys.appointments.all, 'list'] as const,
    list: (filters?: AppointmentFilters) =>
      [...queryKeys.appointments.lists(), filters] as const,
    details: () => [...queryKeys.appointments.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.appointments.details(), id] as const,
    queue: (date: string) => [...queryKeys.appointments.all, 'queue', date] as const,
  },

  // Audit
  audit: {
    all: ['audit-logs'] as const,
    list: (filters?: AuditLogFilters) =>
      [...queryKeys.audit.all, 'list', filters] as const,
  },
}
```

### 5.2 Query Composables

```typescript
// src/composables/queries/usePatients.ts
import { useQuery } from '@tanstack/vue-query'
import { patientsApi } from '@/api/patients'
import { queryKeys } from './queryKeys'

export function usePatients() {
  return useQuery({
    queryKey: queryKeys.patients.list(),
    queryFn: patientsApi.list,
    staleTime: 5 * 60 * 1000,  // 5 min
  })
}

export function usePatient(id: Ref<string>) {
  return useQuery({
    queryKey: computed(() => queryKeys.patients.detail(id.value)),
    queryFn: () => patientsApi.get(id.value),
    enabled: computed(() => !!id.value),
  })
}

// src/composables/queries/useAppointments.ts
import { useQuery } from '@tanstack/vue-query'
import { appointmentsApi } from '@/api/appointments'
import { queryKeys } from './queryKeys'
import type { AppointmentFilters } from '@/api/types'

export function useAppointments(filters?: Ref<AppointmentFilters>) {
  return useQuery({
    queryKey: computed(() => queryKeys.appointments.list(filters?.value)),
    queryFn: () => appointmentsApi.list(filters?.value),
    staleTime: 30 * 1000,  // 30s (agendamentos mudam com frequência)
  })
}

export function useAppointment(id: Ref<string>) {
  return useQuery({
    queryKey: computed(() => queryKeys.appointments.detail(id.value)),
    queryFn: () => appointmentsApi.get(id.value),
    enabled: computed(() => !!id.value),
  })
}

// Fila do dia com polling automático
export function useTodayQueue(professionalId: Ref<string | undefined>) {
  const today = new Date().toISOString().split('T')[0]

  return useQuery({
    queryKey: computed(() => queryKeys.appointments.queue(today)),
    queryFn: () => appointmentsApi.list({
      professionalId: professionalId.value,
      startDate: `${today}T00:00:00`,
      endDate: `${today}T23:59:59`,
    }),
    refetchInterval: 10_000,  // Polling a cada 10s para fila em tempo real
    staleTime: 0,
  })
}
```

### 5.3 Mutation Composables

```typescript
// src/composables/mutations/useAppointmentMutations.ts
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { toast } from 'vue-sonner'
import { appointmentsApi } from '@/api/appointments'
import { queryKeys } from '../queries/queryKeys'
import { getApiErrorMessage } from '@/api/client'
import type {
  BookAppointmentRequest, CancelAppointmentRequest, CheckInRequest, AppointmentResponse
} from '@/api/types'

export function useBookAppointment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: BookAppointmentRequest) => appointmentsApi.book(data),
    onSuccess: (appointment) => {
      // Invalida lista de agendamentos
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Agendamento criado', {
        description: `${appointment.patient.name} — ${appointment.service.name}`,
      })
    },
    onError: (error) => {
      toast.error('Erro ao agendar', { description: getApiErrorMessage(error) })
    },
  })
}

export function useConfirmAppointment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => appointmentsApi.confirm(id),
    onSuccess: (appointment) => {
      // Atualiza o agendamento específico em cache
      queryClient.setQueryData(
        queryKeys.appointments.detail(appointment.id),
        appointment
      )
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Agendamento confirmado')
    },
    onError: (error) => {
      toast.error('Erro ao confirmar', { description: getApiErrorMessage(error) })
    },
  })
}

export function useCancelAppointment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CancelAppointmentRequest }) =>
      appointmentsApi.cancel(id, data),
    onSuccess: (appointment) => {
      queryClient.setQueryData(
        queryKeys.appointments.detail(appointment.id),
        appointment
      )
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Agendamento cancelado')
    },
    onError: (error) => {
      toast.error('Erro ao cancelar', { description: getApiErrorMessage(error) })
    },
  })
}

export function useCheckIn() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CheckInRequest }) =>
      appointmentsApi.checkIn(id, data),
    onSuccess: (appointment) => {
      queryClient.setQueryData(
        queryKeys.appointments.detail(appointment.id),
        appointment
      )
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Check-in realizado', {
        description: `Prioridade verificada: ${appointment.verifiedPriority}`,
      })
    },
    onError: (error) => {
      toast.error('Erro no check-in', { description: getApiErrorMessage(error) })
    },
  })
}

export function useCallNext() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => appointmentsApi.callNext(id),
    onSuccess: (appointment) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success(`Chamando: ${appointment.patient.name}`, {
        description: `Prioridade ${appointment.verifiedPriority}`,
        duration: 8000,
      })
    },
    onError: (error) => {
      toast.error('Erro ao chamar próximo', { description: getApiErrorMessage(error) })
    },
  })
}

export function useStartAppointment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => appointmentsApi.start(id),
    onSuccess: (appointment) => {
      queryClient.setQueryData(
        queryKeys.appointments.detail(appointment.id),
        appointment
      )
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Atendimento iniciado')
    },
    onError: (error) => {
      toast.error('Erro ao iniciar atendimento', { description: getApiErrorMessage(error) })
    },
  })
}

export function useCompleteAppointment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => appointmentsApi.complete(id),
    onSuccess: (appointment) => {
      queryClient.setQueryData(
        queryKeys.appointments.detail(appointment.id),
        appointment
      )
      queryClient.invalidateQueries({ queryKey: queryKeys.appointments.lists() })
      toast.success('Atendimento finalizado')
    },
    onError: (error) => {
      toast.error('Erro ao finalizar', { description: getApiErrorMessage(error) })
    },
  })
}
```

---

## 6. Store de Autenticação (Pinia)

```typescript
// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { jwtDecode } from 'jwt-decode'
import type { UserRole } from '@/api/types'

interface JwtPayload {
  iss: string
  sub: string       // email
  role: UserRole
  exp: number       // timestamp em ms
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('saap_token'))
  const userEmail = ref<string | null>(null)
  const userRole = ref<UserRole | null>(null)

  // Restaura estado do token salvo
  if (token.value) {
    try {
      const decoded = jwtDecode<JwtPayload>(token.value)
      userEmail.value = decoded.sub
      userRole.value = decoded.role
    } catch {
      token.value = null
      localStorage.removeItem('saap_token')
    }
  }

  const isAuthenticated = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('saap_token', newToken)
    const decoded = jwtDecode<JwtPayload>(newToken)
    userEmail.value = decoded.sub
    userRole.value = decoded.role
  }

  function logout() {
    token.value = null
    userEmail.value = null
    userRole.value = null
    localStorage.removeItem('saap_token')
  }

  return { token, userEmail, userRole, isAuthenticated, setToken, logout }
})

// src/composables/usePermissions.ts
// Checagem de permissão por role — espelha @PreAuthorize do backend

export function usePermissions() {
  const authStore = useAuthStore()
  const role = computed(() => authStore.userRole)

  return {
    // Gestão de pacientes
    canManagePatients: computed(() =>
      ['ADMIN', 'RECEPTIONIST'].includes(role.value ?? '')),
    
    // Agendar
    canBook: computed(() =>
      ['ADMIN', 'RECEPTIONIST'].includes(role.value ?? '')),
    
    // Ações de profissional
    canStartAppointment: computed(() =>
      ['ADMIN', 'PROFESSIONAL'].includes(role.value ?? '')),
    canCallNext: computed(() =>
      ['ADMIN', 'PROFESSIONAL'].includes(role.value ?? '')),
    canComplete: computed(() =>
      ['ADMIN', 'PROFESSIONAL'].includes(role.value ?? '')),
    
    // Administração
    canManageUsers: computed(() => role.value === 'ADMIN'),
    canManageProfessionals: computed(() => role.value === 'ADMIN'),
    canManageServices: computed(() => role.value === 'ADMIN'),
    canViewAuditLogs: computed(() => role.value === 'ADMIN'),
    
    // Confirmar agendamento
    canConfirm: computed(() =>
      ['ADMIN', 'RECEPTIONIST'].includes(role.value ?? '')),

    isAdmin: computed(() => role.value === 'ADMIN'),
    isReceptionist: computed(() => role.value === 'RECEPTIONIST'),
    isProfessional: computed(() => role.value === 'PROFESSIONAL'),
  }
}
```

---

## 7. Roteamento

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // ─── AUTH ──────────────────────────────────────────
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { layout: 'auth', requiresAuth: false },
  },

  // ─── ROTAS PÚBLICAS (tokens de email) ──────────────
  {
    path: '/public/confirm',
    name: 'PublicConfirm',
    component: () => import('@/pages/PublicConfirmPage.vue'),
    meta: { layout: 'none', requiresAuth: false },
  },

  // ─── APP (autenticado) ─────────────────────────────
  {
    path: '/',
    redirect: '/dashboard',
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/DashboardPage.vue'),
    meta: { requiresAuth: true, title: 'Dashboard' },
  },
  {
    path: '/appointments',
    name: 'Appointments',
    component: () => import('@/pages/AppointmentsPage.vue'),
    meta: { requiresAuth: true, title: 'Agendamentos' },
  },
  {
    path: '/appointments/:id',
    name: 'AppointmentDetail',
    component: () => import('@/pages/AppointmentDetailPage.vue'),
    meta: { requiresAuth: true, title: 'Detalhes do Agendamento' },
  },
  {
    path: '/patients',
    name: 'Patients',
    component: () => import('@/pages/PatientsPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'RECEPTIONIST'], title: 'Pacientes' },
  },
  {
    path: '/professionals',
    name: 'Professionals',
    component: () => import('@/pages/ProfessionalsPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], title: 'Profissionais' },
  },
  {
    path: '/services',
    name: 'Services',
    component: () => import('@/pages/ServicesPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], title: 'Serviços' },
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/pages/UsersPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], title: 'Usuários' },
  },
  {
    path: '/audit-logs',
    name: 'AuditLogs',
    component: () => import('@/pages/AuditLogsPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], title: 'Auditoria' },
  },
  {
    path: '/queue',
    name: 'Queue',
    component: () => import('@/pages/QueuePage.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN', 'PROFESSIONAL'],
      layout: 'queue',
      title: 'Fila de Atendimento',
    },
  },

  // ─── 404 ───────────────────────────────────────────
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// Guard global
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && authStore.userRole) {
    const allowed = to.meta.roles as string[]
    if (!allowed.includes(authStore.userRole)) {
      return { name: 'Dashboard' }  // redirect silencioso, sem 403 page
    }
  }

  if (to.name === 'Login' && authStore.isAuthenticated) {
    return { name: 'Dashboard' }
  }
})

export default router
```

---

## 8. Componentes Base (Design System Atoms)

### 8.1 AppButton

Variantes: `primary`, `secondary`, `ghost`, `danger`, `success`
Tamanhos: `xs`, `sm`, `md`, `lg`
States: `loading`, `disabled`

```vue
<!-- src/components/ui/AppButton.vue -->
<script setup lang="ts">
import { PhCircleNotch } from '@phosphor-icons/vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'success' | 'warning'
  size?: 'xs' | 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
  fullWidth?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  type: 'button',
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'btn',
      `btn--${variant}`,
      `btn--${size}`,
      { 'btn--loading': loading, 'btn--full': fullWidth },
    ]"
  >
    <PhCircleNotch v-if="loading" class="btn__spinner" :size="16" weight="bold" />
    <slot />
  </button>
</template>

<style scoped>
/* Estilos completos em tailwind — implementar com @apply ou classes inline */
.btn {
  @apply inline-flex items-center justify-center gap-2 font-medium
         rounded-[var(--radius-sm)] border transition-all
         focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed select-none;
  font-family: var(--font-body);
}
.btn--xs    { @apply text-xs px-2.5 py-1.5; }
.btn--sm    { @apply text-sm px-3 py-2; }
.btn--md    { @apply text-sm px-4 py-2.5; }
.btn--lg    { @apply text-base px-6 py-3; }
.btn--full  { @apply w-full; }

.btn--primary   { @apply bg-[var(--color-primary-500)] text-white border-transparent
                         hover:bg-[var(--color-primary-400)] focus-visible:ring-[var(--color-primary-500)]; }
.btn--secondary { @apply bg-white text-[var(--color-neutral-700)] border-[var(--color-neutral-300)]
                         hover:bg-[var(--color-neutral-50)]; }
.btn--ghost     { @apply bg-transparent text-[var(--color-neutral-600)] border-transparent
                         hover:bg-[var(--color-neutral-100)]; }
.btn--danger    { @apply bg-[var(--color-danger-600)] text-white border-transparent
                         hover:bg-[var(--color-danger-700)]; }
.btn--success   { @apply bg-[var(--color-success-500)] text-white border-transparent
                         hover:bg-[var(--color-success-700)]; }
.btn--warning   { @apply bg-[var(--color-accent-500)] text-white border-transparent
                         hover:bg-[var(--color-accent-600)]; }

.btn__spinner { @apply animate-spin; }
</style>
```

### 8.2 PriorityBadge

Componente central do sistema — exibido em TODA a UI.

```vue
<!-- src/components/shared/PriorityBadge.vue -->
<script setup lang="ts">
import type { PriorityLevel } from '@/api/types'

interface Props {
  level: PriorityLevel | null | undefined
  size?: 'sm' | 'md' | 'lg'
  pulse?: boolean  // P1 pulsa (urgência visual)
}

const props = withDefaults(defineProps<Props>(), { size: 'md' })

const labelMap: Record<PriorityLevel, string> = {
  P1: 'P1 — Crítico',
  P2: 'P2 — Urgente',
  P3: 'P3 — Moderado',
  P4: 'P4 — Baixo',
  P5: 'P5 — Padrão',
}

const colorMap: Record<PriorityLevel, string> = {
  P1: 'priority-p1',
  P2: 'priority-p2',
  P3: 'priority-p3',
  P4: 'priority-p4',
  P5: 'priority-p5',
}
</script>

<template>
  <span
    v-if="level"
    :class="[
      'priority-badge',
      `priority-badge--${size}`,
      colorMap[level],
      { 'priority-badge--pulse': pulse && level === 'P1' },
    ]"
  >
    {{ size === 'lg' ? labelMap[level] : level }}
  </span>
  <span v-else class="priority-badge priority-badge--unknown">—</span>
</template>

<style scoped>
.priority-badge {
  @apply inline-flex items-center font-semibold rounded-full tracking-wide;
  font-family: var(--font-mono);
}
.priority-badge--sm { @apply text-xs px-2 py-0.5; }
.priority-badge--md { @apply text-xs px-2.5 py-1; }
.priority-badge--lg { @apply text-sm px-3 py-1.5; }

.priority-p1 { background: var(--color-p1-bg); color: var(--color-p1-text); border: 1px solid var(--color-p1-border); }
.priority-p2 { background: var(--color-p2-bg); color: var(--color-p2-text); border: 1px solid var(--color-p2-border); }
.priority-p3 { background: var(--color-p3-bg); color: var(--color-p3-text); border: 1px solid var(--color-p3-border); }
.priority-p4 { background: var(--color-p4-bg); color: var(--color-p4-text); border: 1px solid var(--color-p4-border); }
.priority-p5 { background: var(--color-p5-bg); color: var(--color-p5-text); border: 1px solid var(--color-p5-border); }
.priority-badge--unknown { @apply text-gray-400 text-xs; }

.priority-badge--pulse {
  animation: pulse-p1 1.5s ease-in-out infinite;
}

@keyframes pulse-p1 {
  0%, 100% { box-shadow: 0 0 0 0 rgba(225, 29, 72, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(225, 29, 72, 0); }
}
</style>
```

### 8.3 StatusBadge

```vue
<!-- src/components/shared/StatusBadge.vue -->
<script setup lang="ts">
import type { AppointmentStatus } from '@/api/types'

const props = defineProps<{ status: AppointmentStatus }>()

const config: Record<AppointmentStatus, { label: string; class: string }> = {
  PENDING:          { label: 'Aguardando',    class: 'status--pending' },
  PENDING_RESPONSE: { label: 'Respondendo',   class: 'status--pending-response' },
  CONFIRMED:        { label: 'Confirmado',    class: 'status--confirmed' },
  ARRIVED:          { label: 'Chegou',        class: 'status--arrived' },
  CALLING:          { label: 'Chamando',      class: 'status--calling' },
  IN_PROGRESS:      { label: 'Em atendimento',class: 'status--in-progress' },
  COMPLETED:        { label: 'Finalizado',    class: 'status--completed' },
  CANCELLED:        { label: 'Cancelado',     class: 'status--cancelled' },
  NO_SHOW:          { label: 'Não compareceu',class: 'status--no-show' },
}
</script>

<template>
  <span :class="['status-badge', config[status].class]">
    {{ config[status].label }}
  </span>
</template>

<style scoped>
.status-badge {
  @apply inline-flex items-center text-xs font-medium px-2.5 py-0.5 rounded-full;
}
.status--pending          { color: #92400E; background: #FEF3C7; }
.status--pending-response { color: #5B21B6; background: #EDE9FE; }
.status--confirmed        { color: #1E40AF; background: #DBEAFE; }
.status--arrived          { color: #065F46; background: #D1FAE5; }
.status--calling          { color: #9A3412; background: #FED7AA; animation: blink 1s step-end infinite; }
.status--in-progress      { color: #065F46; background: #A7F3D0; }
.status--completed        { color: #374151; background: #F3F4F6; }
.status--cancelled        { color: #991B1B; background: #FEE2E2; }
.status--no-show          { color: #6B7280; background: #F9FAFB; }

@keyframes blink {
  50% { opacity: 0.6; }
}
</style>
```

---

## 9. Layout Principal

### 9.1 AppLayout.vue

```
┌─────────────────────────────────────────────────────────┐
│  TOPBAR  [logo] [breadcrumb]          [user] [logout]  │ ← 64px fixo
├──────────┬──────────────────────────────────────────────┤
│          │                                              │
│ SIDEBAR  │              MAIN CONTENT                   │
│ 240px    │              (scroll)                       │
│          │                                              │
│ [nav]    │                                              │
│          │                                              │
└──────────┴──────────────────────────────────────────────┘
```

**Sidebar — itens de navegação por role:**

| Ícone | Rota | Label | Roles |
|-------|------|-------|-------|
| `PhGridFour` | /dashboard | Dashboard | todos |
| `PhCalendarBlank` | /appointments | Agendamentos | todos |
| `PhQueue` | /queue | Fila | ADMIN, PROFESSIONAL |
| `PhUserCircle` | /patients | Pacientes | ADMIN, RECEPTIONIST |
| `PhStethoscope` | /professionals | Profissionais | ADMIN |
| `PhFirstAid` | /services | Serviços | ADMIN |
| `PhUsers` | /users | Usuários | ADMIN |
| `PhClipboardText` | /audit-logs | Auditoria | ADMIN |

### 9.2 Comportamento do Layout

- Sidebar retrátil (ícone + tooltip quando collapsed)
- Estado `collapsed` persiste no Pinia `ui.ts`
- Topbar exibe breadcrumb baseado em `route.meta.title`
- Avatar do usuário exibe email truncado + role badge

---

## 10. Páginas — Especificações

### 10.1 LoginPage

**Layout**: AuthLayout (centralizado, sem sidebar)
**Visual**: Tela dividida — lado esquerdo com branding + imagem, direito com form

```
┌────────────────────┬────────────────────────────────────┐
│                    │                                    │
│   BRAND PANEL      │        LOGIN FORM                  │
│   (azul petróleo)  │                                    │
│                    │  ┌────────────────────────────┐   │
│   [SAAP logo]      │  │  Bem-vindo de volta        │   │
│                    │  │  email input               │   │
│   "Gestão clínica  │  │  senha input + show/hide   │   │
│   inteligente"     │  │  [Entrar]                  │   │
│                    │  └────────────────────────────┘   │
│   (ilustração SVG  │                                    │
│   de agenda/saúde) │  versão da API (rodapé)           │
└────────────────────┴────────────────────────────────────┘
```

**TanStack Query**: useMutation para POST /auth/login
**Validação**: vee-validate + yup (email válido, senha min 6)
**Após login**: redirect para `?redirect` param ou `/dashboard`

---

### 10.2 DashboardPage

**Dados necessários**:
- `useAppointments({ startDate: hoje, endDate: hoje })` — agendamentos do dia
- `useProfessionals()` — para filtro
- Stats calculados localmente via `computed()`

**Layout**:
```
┌──────────────────────────────────────────────────────────┐
│  Bom dia, [nome]!  —  [data de hoje]                     │
├────────────┬────────────┬────────────┬───────────────────┤
│ Agendados  │ Confirmados│ Em atend.  │ Finalizados       │ ← StatCards
│    12      │     8      │    2       │    5              │
├────────────┴────────────┴────────────┴───────────────────┤
│  AGENDAMENTOS DE HOJE                                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 08:00  João Silva  P2  Dr. Carlos  ARRIVED  [→]  │   │
│  │ 09:00  Maria Lima  P3  Dr. Ana     CONFIRMED [→] │   │
│  │ 10:30  Pedro Costa P1  Dr. Carlos  PENDING  [→]  │   │
│  └──────────────────────────────────────────────────┘   │
│  [Ver todos os agendamentos]                             │
└──────────────────────────────────────────────────────────┘
```

**StatCard**: prop `{ label, value, icon, color, trend? }`

---

### 10.3 AppointmentsPage

**Dados**: `useAppointments(filters)`

**Layout**:
```
┌──────────────────────────────────────────────────────────┐
│  Agendamentos           [+ Novo Agendamento]              │
├──────────────────────────────────────────────────────────┤
│  FILTROS: [Data início] [Data fim] [Profissional ▼] [Status ▼] [Limpar]│
├──────────────────────────────────────────────────────────┤
│  TABLE                                                   │
│  # | Paciente | Profissional | Serviço | Data/Hora | Prioridade | Status | Ações │
│  ...                                                     │
├──────────────────────────────────────────────────────────┤
│  Total: 24 agendamentos                                  │
└──────────────────────────────────────────────────────────┘
```

**AppointmentActions** — botões contextuais por status:
- `PENDING` → `[Confirmar]` (RECEPTIONIST/ADMIN)
- `CONFIRMED` → `[Check-in]` (RECEPTIONIST/ADMIN)
- `ARRIVED` → `[Chamar]` (PROFESSIONAL/ADMIN) `[Iniciar]` (PROFESSIONAL/ADMIN)
- `IN_PROGRESS` → `[Finalizar]` (PROFESSIONAL/ADMIN)
- Qualquer (exceto terminais) → `[Cancelar]`

**Modal Novo Agendamento** — BookAppointmentForm:
- Selecionar paciente (select + busca)
- Selecionar profissional
- Selecionar serviço
- Data e hora (datetime-local)
- Método de pagamento
- Prioridade declarada (opcional, default P5)

---

### 10.4 AppointmentDetailPage

**Dados**: `useAppointment(id)`

**Layout**:
```
┌──────────────────────────────────────────────────────────┐
│  ← Agendamentos  /  [Paciente Name]                      │ ← breadcrumb
├─────────────────────────┬────────────────────────────────┤
│  CARD: PACIENTE         │  CARD: DETALHES                │
│  Nome, CPF, tel, email  │  Profissional, Serviço,        │
│  [Ver perfil →]         │  Data/hora, Pagamento          │
├─────────────────────────┴────────────────────────────────┤
│  TIMELINE DE STATUS                                      │
│  ● PENDING → ● CONFIRMED → ● ARRIVED → ● IN_PROGRESS    │
├──────────────────────────────────────────────────────────┤
│  PRIORIDADE                                              │
│  Declarada: [P3 badge]   Verificada: [P2 badge]          │
│  Score: 200000000...     Check-in: 10:15 por Dr. Carlos  │
│  Notas: "Grávida, gestação avançada"                     │
├──────────────────────────────────────────────────────────┤
│  AÇÕES DISPONÍVEIS (contextuais por status)              │
│  [Confirmar] [Check-in] [Cancelar]                       │
└──────────────────────────────────────────────────────────┘
```

---

### 10.5 QueuePage

**Layout especial**: QueueLayout (full-screen, sem sidebar, fundo escuro)
**Dados**: `useTodayQueue(professionalId)` com polling 10s

```
┌──────────────────────────────────────────────────────────┐
│  FILA DE ATENDIMENTO — [DATA]  [●] ao vivo  Dr. Carlos   │ ← topbar mínima
├──────────────────────────────────────────────────────────┤
│                                                          │
│   PRÓXIMO NA FILA                                        │
│   ┌──────────────────────────────────────────────────┐   │
│   │  [P1]  MARIA OLIVEIRA             ARRIVED        │   │ ← card destacado
│   │  Consulta Geral — 09:00           Score: xxx     │   │
│   │  Chegou há 23 min                                │   │
│   │  [                   CHAMAR PACIENTE           ] │   │
│   └──────────────────────────────────────────────────┘   │
│                                                          │
│   EM ATENDIMENTO                                         │
│   ┌──────────────────────────────────────────────────┐   │
│   │  [P3]  JOÃO SILVA          IN_PROGRESS           │   │
│   │  Revisão — Iniciou 10:05   23 min               │   │
│   │  [Finalizar]                                     │   │
│   └──────────────────────────────────────────────────┘   │
│                                                          │
│   AGUARDANDO (5)                                         │
│   ┌─ P2 Ana Santos ──────── Chegou 09:45 ──────────┐   │
│   ┌─ P3 Carlos Mota ──────── Chegou 10:00 ──────────┐   │
│   ┌─ P4 Fernanda ────────── Chegou 10:10 ──────────┐   │
│   ┌─ P5 Roberto ─────────── Chegou 10:30 ──────────┐   │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Regra de exibição da fila**:
- Ordena por `priorityScore ASC` (score menor = maior prioridade)
- P1 recebe borda e animação de pulso
- Mostra `CALLING` com animação de atenção
- Auto-refresh a cada 10s com `refetchInterval`
- Indicador "ao vivo" (dot verde animado)

---

### 10.6 PatientsPage

**Dados**: `usePatients()`

**Layout**:
```
┌──────────────────────────────────────────────────────────┐
│  Pacientes                  [+ Novo Paciente]            │
├──────────────────────────────────────────────────────────┤
│  🔍 Buscar paciente...                                   │ ← filtro local
├──────────────────────────────────────────────────────────┤
│  TABLE                                                   │
│  Nome | CPF | Telefone | Email | Data de Nasc. | Status | Ações │
│  ...                                                     │
└──────────────────────────────────────────────────────────┘
```

**Modal PatientForm** — criar/editar:
- name (required)
- cpf (mask: 999.999.999-99, validação check-digit)
- susNumber (mask: 15 dígitos, opcional)
- email (optional, valid email)
- phone (required, mask: (99) 9 9999-9999)
- birthDate (date picker, max = today)

**Desativar**: ConfirmDialog antes de DELETE

---

### 10.7 AuditLogsPage

**Dados**: `useAuditLogs(filters)` — apenas ADMIN

```
┌──────────────────────────────────────────────────────────┐
│  Logs de Auditoria                                       │
├──────────────────────────────────────────────────────────┤
│  FILTROS: [Usuário] [Ação ▼] [Recurso ▼] [Data início] [Data fim] │
├──────────────────────────────────────────────────────────┤
│  TABLE (somente leitura)                                 │
│  Timestamp | Usuário | Ação | Recurso | IP Address       │
│  ...                                                     │
└──────────────────────────────────────────────────────────┘
```

**Ações auditadas com labels PT-BR**:

```typescript
const actionLabels: Record<string, string> = {
  LOGIN_USUARIO:                    'Login',
  LOGOUT_USUARIO:                   'Logout',
  CADASTRO_PACIENTE:                'Cadastro de Paciente',
  ATUALIZACAO_PACIENTE:             'Atualização de Paciente',
  DESATIVACAO_PACIENTE:             'Desativação de Paciente',
  CADASTRO_PROFISSIONAL:            'Cadastro de Profissional',
  ATUALIZACAO_PROFISSIONAL:         'Atualização de Profissional',
  DESATIVACAO_PROFISSIONAL:         'Desativação de Profissional',
  CADASTRO_USUARIO:                 'Cadastro de Usuário',
  ATUALIZACAO_USUARIO:              'Atualização de Usuário',
  DESATIVACAO_USUARIO:              'Desativação de Usuário',
  CADASTRO_SERVICO:                 'Cadastro de Serviço',
  ATUALIZACAO_SERVICO:              'Atualização de Serviço',
  DESATIVACAO_SERVICO:              'Desativação de Serviço',
  CHECK_IN_VALIDACAO_PRIORIDADE:    'Check-in / Validação de Prioridade',
  CHAMADA_PROXIMO_PACIENTE:         'Chamada do Próximo Paciente',
  CONFIRMACAO_AGENDAMENTO:          'Confirmação de Agendamento',
  CANCELAMENTO_AGENDAMENTO:         'Cancelamento de Agendamento',
  CONFIRMACAO_AGENDAMENTO_POR_EMAIL:'Confirmação via Email',
  CANCELAMENTO_AGENDAMENTO_POR_EMAIL:'Cancelamento via Email',
}
```

---

### 10.8 PublicConfirmPage

**Rota**: `/public/confirm?token=...&action=confirm|cancel`
**Sem autenticação** — layout mínimo

**Fluxo**:
1. Lê `?token` e `?action` da URL
2. Faz POST para `/api/v1/appointments/public/confirm` ou `/cancel`
3. Exibe resultado: sucesso ou erro amigável
4. Link para fechar a página

```
┌──────────────────────────────────────────────────────────┐
│                    [SAAP Logo]                           │
│                                                          │
│           ✅ Agendamento Confirmado!                     │
│                                                          │
│  Sua consulta foi confirmada com sucesso.               │
│  Você receberá um lembrete próximo à data.              │
│                                                          │
│  Data: 28/06/2026 às 14:00                              │
│  Profissional: Dr. Carlos Silva                         │
│  Serviço: Consulta Geral                                │
│                                                          │
│  Caso precise cancelar, clique aqui:                    │
│  [Cancelar Agendamento]                                 │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 11. Formulários — Validação com VeeValidate + Yup

### 11.1 Schema de Validação: Paciente

```typescript
// src/composables/forms/usePatientForm.ts
import { useForm } from 'vee-validate'
import * as yup from 'yup'

const schema = yup.object({
  name: yup.string().required('Nome é obrigatório').min(3, 'Mínimo 3 caracteres'),
  cpf: yup.string()
    .required('CPF é obrigatório')
    .matches(/^\d{3}\.\d{3}\.\d{3}-\d{2}$/, 'CPF inválido')
    .test('cpf-valid', 'CPF inválido', (value) => validateCpf(value ?? '')),
  susNumber: yup.string()
    .nullable()
    .transform(v => v === '' ? null : v)
    .matches(/^\d{15}$/, 'Cartão SUS deve ter 15 dígitos'),
  email: yup.string().nullable().email('Email inválido'),
  phone: yup.string().required('Telefone é obrigatório'),
  birthDate: yup.string()
    .required('Data de nascimento é obrigatória')
    .test('past-date', 'Data deve ser no passado', (value) => {
      if (!value) return false
      return new Date(value) < new Date()
    }),
})
```

### 11.2 Schema: Agendamento

```typescript
const bookSchema = yup.object({
  patientId:      yup.string().uuid().required('Paciente é obrigatório'),
  professionalId: yup.string().uuid().required('Profissional é obrigatório'),
  serviceId:      yup.string().uuid().required('Serviço é obrigatório'),
  dateTime:       yup.string()
    .required('Data e hora são obrigatórios')
    .test('future', 'Data deve ser futura', (v) => {
      if (!v) return false
      return new Date(v) > new Date()
    }),
  paymentMethod:    yup.mixed<PaymentMethod>().required('Forma de pagamento é obrigatória'),
  declaredPriority: yup.mixed<PriorityLevel>().nullable(),
})
```

---

## 12. Utilitários de Formatação

```typescript
// src/lib/formatters.ts
import { format, parseISO, formatDistanceToNow } from 'date-fns'
import { ptBR } from 'date-fns/locale'

export function formatDate(iso: string): string {
  return format(parseISO(iso), 'dd/MM/yyyy', { locale: ptBR })
}

export function formatDateTime(iso: string): string {
  return format(parseISO(iso), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })
}

export function formatTime(iso: string): string {
  return format(parseISO(iso), 'HH:mm', { locale: ptBR })
}

export function formatDateTimeRelative(iso: string): string {
  return formatDistanceToNow(parseISO(iso), { locale: ptBR, addSuffix: true })
}

export function formatCpf(cpf: string): string {
  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
}

export function formatPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  if (digits.length === 11) {
    return digits.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
  }
  return digits.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3')
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)
}

export function formatPaymentMethod(method: PaymentMethod): string {
  const map: Record<PaymentMethod, string> = {
    PIX: 'Pix',
    DINHEIRO: 'Dinheiro',
    CARTAO: 'Cartão',
    CHEQUE: 'Cheque',
  }
  return map[method]
}

// Validação de CPF (algoritmo brasileiro)
export function validateCpf(cpf: string): boolean {
  const digits = cpf.replace(/\D/g, '')
  if (digits.length !== 11 || /^(\d)\1{10}$/.test(digits)) return false

  const calc = (d: string, len: number) => {
    let sum = 0
    for (let i = 0; i < len; i++) {
      sum += parseInt(d[i]) * (len + 1 - i)
    }
    const rem = (sum * 10) % 11
    return rem >= 10 ? 0 : rem
  }

  return (
    calc(digits, 9) === parseInt(digits[9]) &&
    calc(digits, 10) === parseInt(digits[10])
  )
}
```

---

## 13. Configuração do TanStack Query

```typescript
// src/main.ts
import { createApp } from 'vue'
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import router from '@/router'
import App from './App.vue'
import './styles/tokens.css'
import './styles/global.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,          // 1 min padrão
      gcTime: 5 * 60 * 1000,         // 5 min garbage collection
      retry: (failureCount, error) => {
        // Não retenta erros de autorização
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          return false
        }
        if (axios.isAxiosError(error) && error.response?.status === 403) {
          return false
        }
        return failureCount < 2
      },
      refetchOnWindowFocus: true,
    },
    mutations: {
      retry: false,  // mutações nunca são retiradas automaticamente
    },
  },
})

createApp(App)
  .use(createPinia())
  .use(router)
  .use(VueQueryPlugin, { queryClient })
  .mount('#app')
```

---

## 14. Animações e Micro-interações

```css
/* src/styles/animations.css */

/* Entrada de cards (staggered) */
@keyframes slide-up {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Entrada de modal */
@keyframes modal-in {
  from { opacity: 0; transform: scale(0.95) translateY(-8px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}

/* Overlay de modal */
@keyframes overlay-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* Pulse do dot "ao vivo" */
@keyframes live-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.7; }
}

/* Badge P1 urgente */
@keyframes p1-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(225, 29, 72, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(225, 29, 72, 0); }
}

/* Shake para erro de form */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}

/* Skeleton loading */
@keyframes shimmer {
  from { background-position: -200% 0; }
  to   { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-neutral-100) 25%,
    var(--color-neutral-200) 50%,
    var(--color-neutral-100) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

/* Transição de rota */
.page-enter-active,
.page-leave-active {
  transition: opacity var(--duration-normal) var(--ease-out),
              transform var(--duration-normal) var(--ease-out);
}
.page-enter-from { opacity: 0; transform: translateY(8px); }
.page-leave-to   { opacity: 0; transform: translateY(-8px); }
```

---

## 15. Configuração do Ambiente

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.saap.belloinfo.com.br
```

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['Sora', 'sans-serif'],
        body: ['Plus Jakarta Sans', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
```

---

## 16. Estados de UI

### 16.1 Loading States

Todo `useQuery` expõe `isLoading`, `isFetching`, `isError`. Padrão de uso:

```vue
<!-- Skeleton para tabelas -->
<template v-if="isLoading">
  <AppSkeleton v-for="n in 5" :key="n" class="h-12 mb-2" />
</template>

<!-- Dados -->
<template v-else-if="data">
  <!-- conteúdo -->
</template>

<!-- Erro -->
<template v-else-if="isError">
  <AppErrorBoundary :error="error" @retry="refetch" />
</template>

<!-- Vazio -->
<template v-else>
  <AppEmptyState
    icon="PhCalendarBlank"
    title="Nenhum agendamento"
    description="Crie o primeiro agendamento clicando no botão acima."
  />
</template>
```

### 16.2 Toasts (vue-sonner)

```typescript
import { toast } from 'vue-sonner'

// Sucesso
toast.success('Título', { description: 'Mensagem de apoio' })

// Erro
toast.error('Erro', { description: getApiErrorMessage(error) })

// Informação longa (queue)
toast.success(`Chamando: ${patient.name}`, {
  description: `Prioridade ${priority} — Sala 1`,
  duration: 8000,
  action: { label: 'Ver fila', onClick: () => router.push('/queue') },
})
```

### 16.3 ConfirmDialog

Antes de toda ação destrutiva (cancelar, desativar):

```vue
<!-- Uso: -->
<ConfirmDialog
  v-model:open="showConfirm"
  title="Cancelar agendamento?"
  description="Esta ação não pode ser desfeita."
  confirm-label="Sim, cancelar"
  :loading="cancelMutation.isPending.value"
  @confirm="handleCancel"
/>
```

---

## 17. Regras Críticas de Implementação

### 17.1 Transições de Status Permitidas

Respeitar no frontend (validação visual, não de segurança):

| Status Atual | Ação Disponível | Novo Status |
|---|---|---|
| PENDING | Confirmar | PENDING_RESPONSE |
| PENDING / PENDING_RESPONSE | Cancelar | CANCELLED |
| CONFIRMED | Check-in | ARRIVED |
| CONFIRMED | Cancelar | CANCELLED |
| ARRIVED | Chamar próximo | CALLING |
| ARRIVED | Cancelar | CANCELLED |
| CALLING | Iniciar | IN_PROGRESS |
| IN_PROGRESS | Finalizar | COMPLETED |
| IN_PROGRESS | Cancelar | CANCELLED |

### 17.2 Checagem de Conflito de Horário

O backend retorna `409 Conflict` se houver sobreposição. O frontend deve:
1. Capturar `error.response.status === 409`
2. Exibir `toast.error('Conflito de horário', { description: 'Profissional já tem agendamento neste horário' })`
3. NÃO fechar o modal — permitir alterar o horário

### 17.3 Polling da Fila

- `QueuePage` usa `refetchInterval: 10_000` (10s)
- `useTodayQueue` retorna status `ARRIVED`, `CALLING`, `IN_PROGRESS`
- Ordenação local: `[...data].sort((a, b) => (a.priorityScore ?? 0) - (b.priorityScore ?? 0))`
- Score menor = maior prioridade (P1 + timestamp antigo = score mínimo)

### 17.4 Token de Ação Pública

Endpoints `/api/v1/appointments/public/*` recebem `?token=...` via query param.
O frontend da `PublicConfirmPage` extrai via `useRoute().query.token`.
Nunca armazena em localStorage — token de uso único.

### 17.5 CPF Mask + Validação

Usar `@vueuse/core` `useMaskedInput` ou biblioteca `vue-the-mask`:
```
Máscara: ###.###.###-##
Enviar para API: apenas dígitos (remover máscara antes do submit)
```

---

## 18. Checklist de Implementação

### Fase 1 — Fundação
- [ ] Scaffold Vite + Vue 3 + TypeScript
- [ ] Instalar e configurar Tailwind CSS + tokens CSS
- [ ] Configurar TanStack Query com QueryClient
- [ ] Configurar Pinia + authStore
- [ ] Configurar Vue Router com guards
- [ ] Implementar Axios client com interceptors
- [ ] Implementar todas as funções `api/*.ts`
- [ ] Definir todos os tipos em `api/types.ts`

### Fase 2 — Autenticação
- [ ] LoginPage (layout split, form vee-validate)
- [ ] Composable `useAuth` com login/logout
- [ ] Guard de rota por role

### Fase 3 — Componentes Base
- [ ] AppButton (todas variantes)
- [ ] AppInput + AppSelect + form fields
- [ ] AppCard + AppModal
- [ ] AppTable com skeleton
- [ ] PriorityBadge (P1-P5)
- [ ] StatusBadge (todos AppointmentStatus)
- [ ] AppEmptyState + AppErrorBoundary
- [ ] ConfirmDialog
- [ ] AppSidebar + AppTopbar (AppLayout)

### Fase 4 — Dashboard
- [ ] StatCards com contagens do dia
- [ ] TodaySchedule com lista rápida
- [ ] Links para ações rápidas

### Fase 5 — Agendamentos (core)
- [ ] AppointmentsPage com filtros
- [ ] BookAppointmentForm (modal)
- [ ] AppointmentDetailPage
- [ ] AppointmentActions (contextuais por status)
- [ ] CheckInForm (modal)
- [ ] CancelForm (modal com reason)

### Fase 6 — Fila
- [ ] QueuePage (polling 10s)
- [ ] QueueCard com P1-P5 visual
- [ ] CallNextButton
- [ ] QueueStats (aguardando/em atendimento)

### Fase 7 — CRUDs Administrativos
- [ ] PatientsPage + PatientForm
- [ ] ProfessionalsPage + ProfessionalForm
- [ ] ServicesPage + ServiceForm
- [ ] UsersPage + UserForm

### Fase 8 — Auditoria
- [ ] AuditLogsPage com filtros
- [ ] Action labels em PT-BR

### Fase 9 — Público
- [ ] PublicConfirmPage (token de email)
- [ ] Estados: loading / sucesso / erro / token expirado

### Fase 10 — Polimento
- [ ] Transições de página (Vue Router)
- [ ] Toasts com vue-sonner
- [ ] Responsividade (sidebar collapse mobile)
- [ ] Empty states em todas as tabelas
- [ ] Error boundaries em todas as queries

---

## 19. Variáveis de Ambiente Necessárias

| Variável | Descrição | Exemplo |
|---|---|---|
| `VITE_API_BASE_URL` | URL base da API Spring Boot | `http://localhost:8080` |

A API Spring Boot usa:
- `JWT_SECRET` — segredo HMAC-SHA256 para tokens
- `spring.datasource.*` — conexão PostgreSQL
- Esses são configurados no backend e não têm impacto direto no frontend.

---

## 20. Notas de Integração Crítica

1. **Header Authorization**: `Authorization: Bearer <token>` — interceptor do Axios injeta automaticamente
2. **Content-Type**: `application/json` — padrão do Axios client
3. **Datas**: Backend aceita ISO 8601 (`"2026-06-28T14:00:00"`), sem timezone — são tratadas como horário local da clínica
4. **UUIDs**: Todos os IDs são UUID v4. Usar `string` no TypeScript, não tentar parsear
5. **Soft-delete**: Entidades desativadas têm `active: false`. O backend já filtra — o frontend não precisa filtrar
6. **Paginação**: Não implementada no MVP. Todas as listas retornam `[]` completo — adicionar paginação client-side se necessário
7. **CORS**: Backend deve estar configurado para aceitar origem `http://localhost:5173` em dev
8. **Conflito 409**: Apenas em `POST /appointments` — tratar separado de outros erros
9. **403 vs 401**: 401 = token expirado (redirect para login), 403 = sem permissão (silencioso, não redireciona)
10. **Token de ação pública**: Passado como query param `?token=...`, não como Authorization header
