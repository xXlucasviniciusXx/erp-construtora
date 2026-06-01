import { useMemo, useRef, useState } from 'react'
import { ChevronsUpDown, Search } from 'lucide-react'
import { cn } from '@/lib/utils'

export interface ComboOption {
  value: string
  label: string
  hint?: string
}

/**
 * Combobox pesquisável (estilo CMDK) — digita e filtra em tempo real.
 * Leve, sem dependências, no padrão visual do sistema.
 */
export function Combobox({
  options, value, onChange, placeholder = 'Selecione…', disabled,
}: {
  options: ComboOption[]
  value: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  const selected = options.find((o) => o.value === value)
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return options.slice(0, 50)
    return options.filter((o) => (o.label + ' ' + (o.hint ?? '')).toLowerCase().includes(q)).slice(0, 50)
  }, [options, query])

  return (
    <div className="relative">
      <button
        type="button"
        disabled={disabled}
        onClick={() => { setOpen((v) => !v); setQuery(''); setTimeout(() => inputRef.current?.focus(), 0) }}
        className={cn(
          'flex w-full items-center justify-between rounded-md border border-gray-300 px-3 py-2 text-left text-sm',
          'dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 disabled:cursor-not-allowed disabled:opacity-50',
        )}
      >
        <span className={cn('truncate', !selected && 'text-gray-400')}>{selected ? selected.label : placeholder}</span>
        <ChevronsUpDown className="h-4 w-4 flex-shrink-0 text-gray-400" />
      </button>

      {open && !disabled && (
        <>
          <div className="fixed inset-0 z-20" onClick={() => setOpen(false)} />
          <div className="absolute z-30 mt-1 w-full overflow-hidden rounded-md border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-center gap-2 border-b border-gray-100 px-3 py-2 dark:border-gray-700">
              <Search className="h-4 w-4 text-gray-400" />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar…"
                className="w-full bg-transparent text-sm outline-none dark:text-gray-100"
              />
            </div>
            <ul className="max-h-60 overflow-y-auto py-1">
              {filtered.map((o) => (
                <li key={o.value}>
                  <button
                    type="button"
                    onClick={() => { onChange(o.value); setOpen(false) }}
                    className={cn(
                      'flex w-full flex-col px-3 py-2 text-left text-sm hover:bg-gray-50 dark:hover:bg-gray-700',
                      o.value === value && 'bg-primary/5 font-medium',
                    )}
                  >
                    <span className="truncate">{o.label}</span>
                    {o.hint && <span className="truncate text-xs text-gray-400">{o.hint}</span>}
                  </button>
                </li>
              ))}
              {filtered.length === 0 && <li className="px-3 py-3 text-center text-xs text-gray-400">Nada encontrado.</li>}
            </ul>
          </div>
        </>
      )}
    </div>
  )
}
