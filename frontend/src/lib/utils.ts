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
