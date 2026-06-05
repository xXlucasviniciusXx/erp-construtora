import { clsx, type ClassValue } from 'clsx'

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs)
}

export function formatCurrency(value?: number | null): string {
  if (value == null) return '—'
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const [y, m, d] = value.split('T')[0].split('-')
  return `${d}/${m}/${y}`
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return formatDate(value)
  // Fixa o fuso em America/Sao_Paulo: o backend roda em UTC (Render) e serializa
  // o instante com offset; aqui convertemos para a hora do Brasil de forma estável,
  // independente do fuso do navegador. (Datas só-dia continuam em formatDate.)
  return date.toLocaleString('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
