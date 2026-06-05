/**
 * Conversões entre o fragmento armazenado (com tokens em texto `{{nome}}`) e o
 * HTML do editor (com tokens como `<span data-token="nome">`), garantindo um
 * round-trip estável entre o banco e o TipTap.
 */

const TOKEN_RE = /\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g

/** Antes de carregar no editor: troca `{{nome}}` por spans que o TokenNode entende. */
export function tokensToSpans(fragment: string): string {
  if (!fragment) return ''
  return fragment.replace(TOKEN_RE, (_m, name) => `<span data-token="${name}"></span>`)
}

/** Ao salvar: troca os spans de token de volta para o texto `{{nome}}`. */
export function spansToTokens(html: string): string {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
  const root = doc.body.firstElementChild
  if (!root) return html
  root.querySelectorAll('[data-token]').forEach((el) => {
    const name = el.getAttribute('data-token') ?? ''
    el.replaceWith(doc.createTextNode(`{{${name}}}`))
  })
  return root.innerHTML
}
