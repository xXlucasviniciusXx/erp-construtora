import { useState } from 'react'
import { MoreVertical } from 'lucide-react'
import { cn } from '@/lib/utils'

export interface MenuItem {
  label: string
  onClick: () => void
  danger?: boolean
  disabled?: boolean
}

/** Menu de ações contextual (ícone ⋮), alinhado à direita. */
export function ActionsMenu({ items }: { items: MenuItem[] }) {
  const [open, setOpen] = useState(false)

  return (
    <div className="relative inline-block text-left">
      <button
        type="button"
        title="Ações"
        aria-label="Ações"
        onClick={() => setOpen((v) => !v)}
        className="rounded-md p-1.5 text-gray-500 transition hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200"
      >
        <MoreVertical className="h-4 w-4" />
      </button>
      {open && (
        <>
          {/* overlay para fechar ao clicar fora */}
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="animate-scale-in absolute right-0 z-20 mt-1 w-48 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg dark:border-gray-700 dark:bg-gray-800">
            {items.map((it, idx) => (
              <button
                key={idx}
                type="button"
                disabled={it.disabled}
                onClick={() => { setOpen(false); it.onClick() }}
                className={cn(
                  'block w-full px-3 py-2 text-left text-sm transition disabled:cursor-not-allowed disabled:opacity-40',
                  it.danger
                    ? 'text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30'
                    : 'text-gray-700 hover:bg-gray-50 dark:text-gray-200 dark:hover:bg-gray-700',
                )}
              >
                {it.label}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
