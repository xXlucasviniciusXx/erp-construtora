import { type ReactNode } from 'react'
import { Lock } from 'lucide-react'
import { useLicensing } from '@/licensing/LicensingContext'
import type { ModuleCode } from '@/lib/types'

/** Protege uma rota por módulo. Se o módulo estiver desativado, mostra um
 *  aviso amigável em vez da página (gating no frontend — Fase 1). */
export function ModuleGuard({ code, children }: { code: ModuleCode; children: ReactNode }) {
  const { canAccess } = useLicensing()
  if (canAccess(code)) return <>{children}</>
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-900/40">
        <Lock className="h-6 w-6" />
      </div>
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Módulo não contratado</h2>
      <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
        Este módulo está desativado na sua licença. Fale com o administrador para habilitá-lo em
        <span className="font-medium"> Configurações → Módulos &amp; Licença</span>.
      </p>
    </div>
  )
}
