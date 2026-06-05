import { CONTRACT_TOKENS, type TokenDef } from '@/lib/contractTokens'

const GROUPS: TokenDef['group'][] = ['Empresa', 'Comprador', 'Imóvel', 'Pagamento', 'Distrato']

/**
 * Paleta de campos dinâmicos. Cada item tem tooltip (title) explicando a função
 * e insere o token no cursor ao clicar.
 */
export function TokenPalette({ onInsert }: { onInsert: (name: string) => void }) {
  return (
    <div className="space-y-3">
      <p className="rounded-md bg-blue-50 px-3 py-2 text-xs text-blue-700 dark:bg-blue-900/20 dark:text-blue-300">
        Clique nos campos dinâmicos abaixo para inserir informações automáticas do sistema no contrato.
      </p>
      {GROUPS.map((group) => {
        const items = CONTRACT_TOKENS.filter((t) => t.group === group)
        if (items.length === 0) return null
        return (
          <div key={group}>
            <div className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-gray-400">{group}</div>
            <div className="flex flex-wrap gap-1">
              {items.map((t) => (
                <button
                  key={t.name}
                  type="button"
                  title={t.description}
                  onClick={() => onInsert(t.name)}
                  className="rounded border border-gray-200 bg-gray-50 px-2 py-1 text-left text-[11px] text-gray-700 transition hover:border-primary hover:bg-primary hover:text-white dark:border-gray-700 dark:bg-gray-800 dark:text-gray-200"
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}
