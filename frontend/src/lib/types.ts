// Tipos espelhando os DTOs do backend (mantidos manualmente no POC).
// TODO: gerar a partir do OpenAPI (/v3/api-docs) com openapi-typescript.

export interface AuthResponse {
  token: string
  tokenType: string
  expiresInMs: number
  userId: string
  name: string
  email: string
  role: string
  permissions: string[]
  refreshToken?: string | null
}

export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface Client {
  id: string
  personType: 'PF' | 'PJ'
  name: string
  document: string
  stateRegistration?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  state?: string
  zipCode?: string
  maritalStatus?: string
  occupation?: string
  notes?: string
  status: 'ACTIVE' | 'INACTIVE'
}

export type LotStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'CANCELLED'

export interface Development {
  id: string
  name: string
  internalCode: string
  blocksCount?: number
  lotsCount?: number
  expectedValue: number
  plannedTotal: number
  receivedTotal: number
  actualBlocks: number
  actualLots: number
  address?: string
  status?: string
  dimensions?: string
}

export interface Block {
  id: string
  developmentId: string
  developmentName: string
  name: string
  internalCode: string
  registration?: string
  area?: number
  lotsCount: number
}

export interface Lot {
  id: string
  blockId: string
  blockName: string
  developmentId: string
  developmentName: string
  name: string
  internalCode: string
  registration?: string
  unit?: string
  address?: string
  totalArea?: number
  builtArea?: number
  plannedValue?: number
  saleValue?: number
  status: LotStatus
  contractExtra?: string
  notes?: string
  label: string
  reservationExpiresAt?: string | null   // ISO datetime; presente apenas quando RESERVED
}

export interface Installment {
  id: string
  saleId: string
  number: number
  amount: number
  dueDate: string
  paymentDate?: string
  status: 'OPEN' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  paymentMethod?: string
  receiptUrl?: string
  notes?: string
  daysLate: number
  penaltyAmount: number
  interestAmount: number
  updatedAmount: number
}

export interface Sale {
  id: string
  contractNumber?: string
  clientId: string
  clientName: string
  lotId: string
  propertyLabel: string
  expectedValue?: number
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  purchaseType?: string
  paymentMethod?: string
  correctionIndex?: string
  interestRate?: number
  penaltyRate?: number
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
  saleDate: string
  notes?: string
  distratoDate?: string | null
  distratoReason?: string | null
  distratoRefundAmount?: number | null
  distratoRetainedAmount?: number | null
  paidInstallments?: number
  paidAmount?: number
  openAmount?: number
  installments: Installment[]
}

/** Documento (contrato/distrato) arquivado de uma venda. */
export interface ContractDocument {
  id: string
  saleId: string
  type: 'CONTRACT' | 'DISTRATO'
  version: number
  fileName: string
  generatedAt: string
  generatedBy?: string | null
}

/** Modelo de contrato/distrato editável. */
export interface ContractTemplate {
  id: string
  kind: 'CONTRACT' | 'DISTRATO'
  name: string
  body: string
  isDefault: boolean
  active: boolean
}

export interface BankAccount {
  id: string
  name: string
  bankCode?: string
  bankName?: string
  agency?: string
  accountNumber?: string
  initialBalance: number
  active: boolean
}

export interface BankTransaction {
  id: string
  bankAccountId: string
  transactionDate: string
  description?: string
  amount: number
  type: 'CREDIT' | 'DEBIT'
  documentNumber?: string
  bankIdentifier?: string
  status: 'PENDING' | 'RECONCILED' | 'IGNORED' | 'DIVERGENT'
  notes?: string
}

export interface ManualTarget {
  targetType: 'RECEIVABLE' | 'PAYABLE' | 'INSTALLMENT'
  targetId: string
  label: string
  amount: number
  dueDate?: string
}

export interface Suggestion {
  targetType: 'RECEIVABLE' | 'PAYABLE' | 'INSTALLMENT'
  targetId: string
  label: string
  amount: number
  dueDate?: string
  score: number
  reason: string
}

export interface DashboardData {
  totalReceivableMonth: number
  totalPayableMonth: number
  expectedBalance: number
  overdueInstallments: number
  upcomingInstallments: number
  pendingBankTransactions: number
  availableProperties: number
  soldProperties: number
}

export interface Point {
  label: string
  value: number
}

export interface Dre {
  revenues: Point[]
  totalRevenue: number
  expenses: Point[]
  totalExpense: number
  result: number
  basis: 'CAIXA' | 'COMPETENCIA'
}

export interface DashboardAnalytics {
  totalSold: number
  totalReceived: number
  totalOpen: number
  totalOverdue: number
  delinquentClients: number
  activeClients: number
  inactiveClients: number
  lotsSold: number
  lotsAvailable: number
  receivedByMonth: Point[]
  toReceiveByMonth: Point[]
  overdueByMonth: Point[]
  delinquencyByDevelopment: Point[]
  salesByMonth: Point[]
  salesByPurchaseType: Point[]
  cashFlowForecast: Point[]
  payablesPaidVsOpen: Point[]
  receivablesReceivedVsOpen: Point[]
  overdueByAging: Point[]
  expensesByDevelopment: Point[]
  profitByDevelopment: Point[]
  expensesByCategory: Point[]
  expensesByCostCenter: Point[]
}

export interface InstallmentDetail {
  id: string
  saleId: string
  number: number
  amount: number
  dueDate: string
  paymentDate?: string
  status: 'OPEN' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  clientId: string
  clientName: string
  clientDocument: string
  clientPhone?: string
  development?: string
  propertyLabel?: string
  daysLate: number
  penaltyAmount: number
  interestAmount: number
  updatedAmount: number
}

export interface Supplier {
  id: string
  name: string
  document?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  state?: string
  category?: string
  notes?: string
  active: boolean
}

export interface CostCenter {
  id: string
  name: string
  description?: string
  grupo?: string
  active: boolean
}

export interface Category {
  id: string
  grupo: string
  name: string
  active: boolean
}

export interface SystemSettings {
  systemName: string
  logoUrl?: string
  primaryColor?: string
  secondaryColor?: string
  theme?: string
  companyName?: string
  companyDocument?: string
  companyAddress?: string
  companyPhone?: string
  companyEmail?: string
  footerText?: string
  // SMTP (a senha nunca volta do servidor; mailPasswordSet indica se já está definida)
  mailEnabled?: boolean
  mailHost?: string
  mailPort?: number
  mailUsername?: string
  mailPassword?: string
  mailFrom?: string
  mailReminderDays?: number
  mailPasswordSet?: boolean
}

export interface EmailNotification {
  id: string
  recipient: string
  subject: string
  body: string
  eventType: string
  status: 'PENDING' | 'SENT' | 'FAILED'
  error?: string
  createdAt: string
  sentAt?: string
}

export interface AppUser {
  id: string
  name: string
  email: string
  role: string
  active: boolean
  createdAt: string
}

// ---- Licenciamento (Fase 1: módulos / feature flags + licença) ----

export type ModuleCode =
  | 'DASHBOARD'
  | 'CLIENTES'
  | 'EMPREENDIMENTOS'
  | 'VENDAS'
  | 'CONTAS_PAGAR'
  | 'CONTAS_RECEBER'
  | 'FORNECEDORES'
  | 'CONCILIACAO'
  | 'DRE'
  | 'RELATORIOS'
  | 'NOTIFICACOES'
  | 'PORTAL_CLIENTE'
  | 'APP_MOBILE'

export interface Module {
  id: string
  code: ModuleCode | string
  name: string
  description?: string
  active: boolean
  sortOrder: number
}

export interface License {
  plan: string
  status: string // ATIVA | EXPIRADA | SUSPENSA | CANCELADA
  startDate?: string
  endDate?: string
  periodMonths: number
  maxUsers?: number
  notes?: string
  customer?: string
  graceDays: number
  daysToExpire?: number | null
  expired: boolean
  readOnly: boolean
  blocked: boolean
  hasKey: boolean
}

export interface LicensingInfo {
  modules: Module[]
  license: License
}

// ---- Perfis de acesso (papéis) e catálogo de permissões ----

export interface Permission {
  code: string
  description?: string
  module: string // ex.: CLIENTES, SISTEMA
  action: string // VIEW | EDIT | MANAGE
}

export interface Role {
  id: string
  name: string
  description?: string
  permissions: string[]
  system: boolean // ADMIN é protegido
  userCount: number
}

export interface Payable {
  id: string
  supplier?: string
  categoryId?: string
  categoryName?: string
  categoryGroup?: string
  description?: string
  amount: number
  dueDate: string
  paymentDate?: string
  status: 'OPEN' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  paymentMethod?: string
  costCenterId?: string
  costCenterName?: string
  developmentId?: string
  developmentName?: string
  notes?: string
}

// ---- Listas configuráveis ----
export interface NamedItem {
  id: string
  name: string
  active: boolean
  sortOrder: number
  sgsCode?: number | null   // só índices de correção
}

/** Cotação oficial (BCB) de um índice de correção. */
export interface IndexQuote {
  id: string
  name: string
  sgsCode?: number | null
  accumulated12m?: number | null
  lastValue?: number | null
  lastRef?: string | null
  available: boolean
}
