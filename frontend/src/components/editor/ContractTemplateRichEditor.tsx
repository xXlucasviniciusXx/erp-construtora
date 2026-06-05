import { forwardRef, useImperativeHandle, useState, type ReactNode } from 'react'
import { useEditor, EditorContent, type Editor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextStyle from '@tiptap/extension-text-style'
import TextAlign from '@tiptap/extension-text-align'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableHeader from '@tiptap/extension-table-header'
import TableCell from '@tiptap/extension-table-cell'
import {
  Bold, Italic, Underline as UnderlineIcon, AlignLeft, AlignCenter, AlignRight, AlignJustify,
  List, ListOrdered, Table as TableIcon, Code, Eye,
} from 'lucide-react'
import { FontSize } from './FontSize'
import { TokenNode } from './TokenNode'
import { TokenPalette } from './TokenPalette'
import { tokensToSpans, spansToTokens } from './tokenHtml'
import { cn } from '@/lib/utils'

/** Handle imperativo: lê o conteúdo atual do editor (fonte da verdade no salvar). */
export interface RichEditorHandle {
  getContent: () => string
}

interface Props {
  value: string
  onPreview?: () => void
}

const FONT_SIZES = [
  { label: 'Pequena', value: '10px' },
  { label: 'Normal', value: '' },
  { label: 'Média', value: '14px' },
  { label: 'Grande', value: '18px' },
]

export const ContractTemplateRichEditor = forwardRef<RichEditorHandle, Props>(
  function ContractTemplateRichEditor({ value, onPreview }, ref) {
  const [showCode, setShowCode] = useState(false)
  const [rawHtml, setRawHtml] = useState('')

  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextStyle,
      FontSize,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Table.configure({ resizable: false }),
      TableRow,
      TableHeader,
      TableCell,
      TokenNode,
    ],
    content: tokensToSpans(value),
    editorProps: {
      attributes: { class: 'contract-prose focus:outline-none' },
    },
  })

  // O salvamento lê o conteúdo aqui (não via onChange), evitando corridas de
  // montagem (StrictMode/Suspense). No modo código, usa o texto da textarea.
  useImperativeHandle(ref, () => ({
    getContent: () => {
      if (showCode) return rawHtml
      return editor ? spansToTokens(editor.getHTML()) : value
    },
  }), [editor, showCode, rawHtml, value])

  if (!editor) return <div className="h-72 animate-pulse rounded-md bg-gray-100 dark:bg-gray-800" />

  function toggleCode() {
    if (!showCode) {
      setRawHtml(spansToTokens(editor!.getHTML()))
      setShowCode(true)
    } else {
      // Aplica o HTML editado de volta ao editor visual
      editor!.commands.setContent(tokensToSpans(rawHtml))
      setShowCode(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1fr_220px]">
      <div className="rounded-md border border-gray-300 dark:border-gray-600">
        <Toolbar editor={editor} showCode={showCode} onToggleCode={toggleCode} onPreview={onPreview} />
        {showCode ? (
          <textarea
            className="min-h-[300px] w-full rounded-b-md border-0 bg-gray-900 px-3 py-2 font-mono text-xs text-gray-100"
            value={rawHtml}
            onChange={(e) => setRawHtml(e.target.value)}
            spellCheck={false}
          />
        ) : (
          <EditorContent editor={editor} className="contract-editor" />
        )}
      </div>

      <div className="rounded-md border border-gray-200 p-2 dark:border-gray-700">
        <TokenPalette onInsert={(name) => editor.chain().focus().insertToken(name).run()} />
      </div>
    </div>
  )
})

function Toolbar({ editor, showCode, onToggleCode, onPreview }:
  { editor: Editor; showCode: boolean; onToggleCode: () => void; onPreview?: () => void }) {
  const Btn = ({ on, active, title, children }: { on: () => void; active?: boolean; title: string; children: ReactNode }) => (
    <button
      type="button"
      title={title}
      onMouseDown={(e) => { e.preventDefault(); on() }}
      disabled={showCode}
      className={cn(
        'rounded p-1.5 text-gray-600 transition hover:bg-gray-100 disabled:opacity-40 dark:text-gray-300 dark:hover:bg-gray-700',
        active && 'bg-gray-200 text-primary dark:bg-gray-700',
      )}
    >
      {children}
    </button>
  )
  const sep = <span className="mx-0.5 h-5 w-px bg-gray-200 dark:bg-gray-700" />

  return (
    <div className="flex flex-wrap items-center gap-0.5 border-b border-gray-200 bg-gray-50 px-1.5 py-1 dark:border-gray-700 dark:bg-gray-800">
      <select
        className="mr-1 rounded border border-gray-200 bg-white px-1 py-1 text-xs dark:border-gray-600 dark:bg-gray-800"
        disabled={showCode}
        value={editor.isActive('heading', { level: 1 }) ? 'h1' : editor.isActive('heading', { level: 2 }) ? 'h2' : editor.isActive('heading', { level: 3 }) ? 'h3' : 'p'}
        onChange={(e) => {
          const v = e.target.value
          if (v === 'p') editor.chain().focus().setParagraph().run()
          else editor.chain().focus().toggleHeading({ level: Number(v[1]) as 1 | 2 | 3 }).run()
        }}
      >
        <option value="p">Texto</option>
        <option value="h1">Título 1</option>
        <option value="h2">Título 2</option>
        <option value="h3">Título 3</option>
      </select>

      <select
        className="mr-1 rounded border border-gray-200 bg-white px-1 py-1 text-xs dark:border-gray-600 dark:bg-gray-800"
        disabled={showCode}
        value={(editor.getAttributes('textStyle').fontSize as string) || ''}
        onChange={(e) => {
          const v = e.target.value
          if (v) editor.chain().focus().setFontSize(v).run()
          else editor.chain().focus().unsetFontSize().run()
        }}
        title="Tamanho da fonte"
      >
        {FONT_SIZES.map((f) => <option key={f.label} value={f.value}>{f.label}</option>)}
      </select>

      {sep}
      <Btn title="Negrito" active={editor.isActive('bold')} on={() => editor.chain().focus().toggleBold().run()}><Bold className="h-4 w-4" /></Btn>
      <Btn title="Itálico" active={editor.isActive('italic')} on={() => editor.chain().focus().toggleItalic().run()}><Italic className="h-4 w-4" /></Btn>
      <Btn title="Sublinhado" active={editor.isActive('underline')} on={() => editor.chain().focus().toggleUnderline().run()}><UnderlineIcon className="h-4 w-4" /></Btn>
      {sep}
      <Btn title="Alinhar à esquerda" active={editor.isActive({ textAlign: 'left' })} on={() => editor.chain().focus().setTextAlign('left').run()}><AlignLeft className="h-4 w-4" /></Btn>
      <Btn title="Centralizar" active={editor.isActive({ textAlign: 'center' })} on={() => editor.chain().focus().setTextAlign('center').run()}><AlignCenter className="h-4 w-4" /></Btn>
      <Btn title="Alinhar à direita" active={editor.isActive({ textAlign: 'right' })} on={() => editor.chain().focus().setTextAlign('right').run()}><AlignRight className="h-4 w-4" /></Btn>
      <Btn title="Justificar" active={editor.isActive({ textAlign: 'justify' })} on={() => editor.chain().focus().setTextAlign('justify').run()}><AlignJustify className="h-4 w-4" /></Btn>
      {sep}
      <Btn title="Lista com marcadores" active={editor.isActive('bulletList')} on={() => editor.chain().focus().toggleBulletList().run()}><List className="h-4 w-4" /></Btn>
      <Btn title="Lista numerada" active={editor.isActive('orderedList')} on={() => editor.chain().focus().toggleOrderedList().run()}><ListOrdered className="h-4 w-4" /></Btn>
      <Btn title="Inserir tabela" on={() => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()}><TableIcon className="h-4 w-4" /></Btn>

      <div className="ml-auto flex items-center gap-0.5">
        {onPreview && (
          <button type="button" disabled={showCode} title="Pré-visualizar com dados de exemplo"
            onClick={onPreview}
            className="flex items-center gap-1 rounded px-2 py-1 text-xs text-gray-600 hover:bg-gray-100 disabled:opacity-40 dark:text-gray-300 dark:hover:bg-gray-700">
            <Eye className="h-4 w-4" /> Pré-visualizar
          </button>
        )}
        <button type="button" title="Ver/editar código HTML (avançado)"
          onClick={onToggleCode}
          className={cn('flex items-center gap-1 rounded px-2 py-1 text-xs hover:bg-gray-100 dark:hover:bg-gray-700',
            showCode ? 'bg-gray-200 text-primary dark:bg-gray-700' : 'text-gray-600 dark:text-gray-300')}>
          <Code className="h-4 w-4" /> {showCode ? 'Voltar ao editor' : 'Ver código HTML'}
        </button>
      </div>
    </div>
  )
}
