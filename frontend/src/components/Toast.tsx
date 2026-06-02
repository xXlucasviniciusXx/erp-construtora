import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react'
import { cn } from '@/lib/utils'

type ToastKind = 'success' | 'error' | 'info'
interface Toast {
  id: number
  kind: ToastKind
  message: string
}

interface ToastApi {
  success: (message: string) => void
  error: (message: string) => void
  info: (message: string) => void
}

const ToastContext = createContext<ToastApi | null>(null)

let counter = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const remove = useCallback((id: number) => {
    setToasts((list) => list.filter((t) => t.id !== id))
  }, [])

  const push = useCallback(
    (kind: ToastKind, message: string) => {
      const id = ++counter
      setToasts((list) => [...list, { id, kind, message }])
      window.setTimeout(() => remove(id), kind === 'error' ? 6000 : 3500)
    },
    [remove],
  )

  const api: ToastApi = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[100] flex w-full max-w-sm flex-col gap-2">
        {toasts.map((t) => (
          <ToastCard key={t.id} toast={t} onClose={() => remove(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

const CONFIG: Record<ToastKind, { icon: typeof CheckCircle2; ring: string; iconColor: string }> = {
  success: { icon: CheckCircle2, ring: 'border-l-green-500', iconColor: 'text-green-500' },
  error: { icon: AlertCircle, ring: 'border-l-red-500', iconColor: 'text-red-500' },
  info: { icon: Info, ring: 'border-l-blue-500', iconColor: 'text-blue-500' },
}

function ToastCard({ toast, onClose }: { toast: Toast; onClose: () => void }) {
  const { icon: Icon, ring, iconColor } = CONFIG[toast.kind]
  return (
    <div
      className={cn(
        'animate-toast-in pointer-events-auto flex items-start gap-3 rounded-lg border border-l-4 bg-white p-3 shadow-lg',
        'dark:border-gray-700 dark:bg-gray-800',
        ring,
      )}
      role="status"
    >
      <Icon className={cn('mt-0.5 h-5 w-5 flex-shrink-0', iconColor)} />
      <p className="flex-1 text-sm text-gray-700 dark:text-gray-200">{toast.message}</p>
      <button
        onClick={onClose}
        className="flex-shrink-0 rounded p-0.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-700"
        aria-label="Fechar"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  )
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast deve ser usado dentro de <ToastProvider>')
  return ctx
}
