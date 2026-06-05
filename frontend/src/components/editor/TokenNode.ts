import { Node, mergeAttributes } from '@tiptap/core'
import { tokenDescription, tokenLabel } from '@/lib/contractTokens'

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    token: {
      /** Insere um campo dinâmico {{nome}} no cursor. */
      insertToken: (name: string) => ReturnType
    }
  }
}

/**
 * Campo dinâmico do contrato, renderizado como um "chip" inline e atômico
 * (selecionável/apagável como uma unidade). Persistido no HTML como
 * `<span data-token="nome">{{nome}}</span>` — depois normalizado para o texto
 * `{{nome}}` ao salvar (ver tokenSerialize).
 */
export const TokenNode = Node.create({
  name: 'token',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      name: {
        default: '',
        parseHTML: (el) => (el as HTMLElement).getAttribute('data-token') ?? '',
        renderHTML: (attrs) => ({ 'data-token': attrs.name }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-token]' }]
  },

  renderHTML({ node, HTMLAttributes }) {
    const name = node.attrs.name as string
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        class: 'token-chip',
        title: tokenDescription(name),
      }),
      `{{${name}}}`,
    ]
  },

  renderText({ node }) {
    return `{{${node.attrs.name}}}`
  },

  addCommands() {
    return {
      insertToken:
        (name: string) =>
        ({ commands }) =>
          commands.insertContent({ type: this.name, attrs: { name } }),
    }
  },
})

/** Rótulo amigável de um token (para a paleta). */
export { tokenLabel }
