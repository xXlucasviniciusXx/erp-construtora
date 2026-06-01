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

export interface Property {
  id: string
  development: string
  block?: string
  lot?: string
  unit?: string
  registration?: string
  address?: string
  totalArea?: number
  builtArea?: number
  saleValue?: number
  status: 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'CANCELLED'
  contractExtra?: string
  notes?: string
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
}

export interface Sale {
  id: string
  clientId: string
  clientName: string
  propertyId: string
  propertyLabel: string
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  paymentMethod?: string
  correctionIndex?: string
  interestRate?: number
  penaltyRate?: number
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
  saleDate: string
  notes?: string
  installments: Installment[]
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
  salesByPaymentMethod: Point[]
  cashFlowForecast: Point[]
  payablesPaidVsOpen: Point[]
  overdueByAging: Point[]
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
}

export interface AppUser {
  id: string
  name: string
  email: string
  role: string
  active: boolean
  createdAt: string
}
