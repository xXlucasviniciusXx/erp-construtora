import {
  forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode,
  type SelectHTMLAttributes, type HTMLAttributes,
} from 'react'
import { Loader2, X, Inbox, ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

/* Conjunto enxuto de primitivos de UI no estilo shadcn (Tailwind puro), com suporte a tema claro/escuro. */

export const Button = forwardRef<
  HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'outline' | 'ghost' | 'danger'; loading?: boolean }
>(({ className, variant = 'primary', loading, children, disabled, ...props }, ref) => {
  const variants = {
    primary: 'text-white btn-primary shadow-sm hover:opacity-90 hover:shadow',
    outline: 'border border-gray-300 bg-white text-gray-800 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700',
    ghost: 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700',
    danger: 'bg-red-600 text-white shadow-sm hover:bg-red-700',
  }
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-all disabled:cursor-not-allowed disabled:opacity-50',
        variants[variant],
        className,
      )}
      {...props}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
})
Button.displayName = 'Button'

const inputClasses =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 ' +
  'dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-500'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input ref={ref} className={cn(inputClasses, className)} {...props} />
  ),
)
Input.displayName = 'Input'

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <select ref={ref} className={cn(inputClasses, 'bg-white dark:bg-gray-800', className)} {...props}>
      {children}
    </select>
  ),
)
Select.displayName = 'Select'

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block space-y-1">
      <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{label}</span>
      {children}
    </label>
  )
}

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <div className={cn('rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800', className)}>
      {children}
    </div>
  )
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-5 flex items-start justify-between gap-4">
      <div>
        <h1 className="text-xl font-semibold tracking-tight text-gray-900 dark:text-gray-50">{title}</h1>
        {subtitle && <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Badge({ children, color = 'gray', dot }: { children: ReactNode; color?: string; dot?: boolean }) {
  const colors: Record<string, string> = {
    gray: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-200',
    green: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
    red: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
    yellow: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300',
    blue: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
  }
  const dotColors: Record<string, string> = {
    gray: 'bg-gray-400', green: 'bg-green-500', red: 'bg-red-500', yellow: 'bg-yellow-500', blue: 'bg-blue-500',
  }
  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium', colors[color])}>
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full', dotColors[color])} />}
      {children}
    </span>
  )
}

export function Table({ headers, children }: { headers: string[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500 dark:bg-gray-900/50 dark:text-gray-400">
          <tr>
            {headers.map((h) => (
              <th key={h} className="px-4 py-3 font-medium">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 dark:divide-gray-700/60">{children}</tbody>
      </table>
    </div>
  )
}

/** Linha de tabela com hover correto em ambos os temas (substitui hover:bg-gray-50 manual). */
export function Tr({ className, children, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr className={cn('row-hover', className)} {...props}>
      {children}
    </tr>
  )
}

export function Modal({
  open, onClose, title, children, size = 'lg',
}: { open: boolean; onClose: () => void; title: string; children: ReactNode; size?: 'sm' | 'lg' | 'xl' }) {
  if (!open) return null
  const widths = { sm: 'max-w-sm', lg: 'max-w-lg', xl: 'max-w-2xl' }
  return (
    <div className="animate-fade-in fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm" onClick={onClose}>
      <div
        className={cn(
          'animate-scale-in max-h-[90vh] w-full overflow-y-auto rounded-xl bg-white shadow-2xl dark:bg-gray-800 dark:text-gray-100',
          widths[size],
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-gray-100 bg-white px-6 py-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-gray-400 transition hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-700"
            aria-label="Fechar"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn('h-5 w-5 animate-spin text-primary', className)} />
}

/** Fallback de carregamento de página (usado no Suspense das rotas lazy). */
export function PageFallback() {
  return (
    <div className="flex h-64 items-center justify-center">
      <div className="flex flex-col items-center gap-3 text-gray-400">
        <Spinner className="h-7 w-7" />
        <span className="text-sm">Carregando…</span>
      </div>
    </div>
  )
}

/** Bloco de skeleton de carregamento. */
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('skeleton h-4 w-full', className)} />
}

/** Skeleton em formato de tabela, para listagens em carregamento. */
export function TableSkeleton({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
      <div className="border-b border-gray-100 bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-900/50">
        <Skeleton className="h-3 w-32" />
      </div>
      <div className="divide-y divide-gray-100 dark:divide-gray-700/60">
        {Array.from({ length: rows }).map((_, r) => (
          <div key={r} className="flex items-center gap-4 px-4 py-3">
            {Array.from({ length: cols }).map((_, c) => (
              <Skeleton key={c} className={cn('h-4', c === 0 ? 'w-1/4' : 'flex-1')} />
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

/** Controles de paginação server-side (anterior/próxima + contagem). */
export function Pagination({
  page, totalPages, totalElements, onChange,
}: { page: number; totalPages: number; totalElements: number; onChange: (page: number) => void }) {
  if (totalElements === 0) return null
  const btn = 'inline-flex items-center gap-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40 dark:border-gray-600 dark:hover:bg-gray-700'
  return (
    <div className="mt-3 flex items-center justify-between gap-3 text-sm text-gray-500 dark:text-gray-400">
      <span>
        {totalElements} registro{totalElements === 1 ? '' : 's'}
        {totalPages > 1 && <> · página {page + 1} de {totalPages}</>}
      </span>
      {totalPages > 1 && (
        <div className="flex gap-2">
          <button className={btn} disabled={page <= 0} onClick={() => onChange(page - 1)}>
            <ChevronLeft className="h-4 w-4" /> Anterior
          </button>
          <button className={btn} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
            Próxima <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  )
}

/** Estado vazio com ícone, mensagem e ação opcional. */
export function EmptyState({
  icon: Icon = Inbox, title, description, action,
}: { icon?: typeof Inbox; title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center dark:border-gray-700 dark:bg-gray-800/50">
      <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700">
        <Icon className="h-6 w-6 text-gray-400" />
      </div>
      <p className="text-sm font-medium text-gray-700 dark:text-gray-200">{title}</p>
      {description && <p className="mt-1 max-w-sm text-xs text-gray-500 dark:text-gray-400">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
