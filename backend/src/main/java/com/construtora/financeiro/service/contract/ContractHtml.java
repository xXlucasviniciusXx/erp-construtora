package com.construtora.financeiro.service.contract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * Ponte entre o editor visual (HTML do navegador) e o gerador de PDF
 * (Flying Saucer, que exige XHTML bem-formado).
 *
 * <p>O modelo é armazenado como um <b>fragmento HTML</b> (apenas o conteúdo
 * editável). Aqui:
 * <ul>
 *   <li>{@link #sanitizeFragment} limpa HTML inseguro no salvamento (Safelist);</li>
 *   <li>{@link #wrap} embrulha o fragmento no esqueleto XHTML + CSS padrão;</li>
 *   <li>{@link #toXhtml} normaliza para XML bem-formado (fecha tags, escapa);</li>
 *   <li>{@link #extractFragment} extrai o corpo de um documento legado (full-doc).</li>
 * </ul>
 */
public final class ContractHtml {

    private ContractHtml() {}

    /** CSS aplicado a todo documento gerado (inclui classes legadas dos modelos antigos). */
    private static final String CSS = """
            body { font-family: sans-serif; font-size: 12px; color: #111; line-height: 1.5; }
            h1 { font-size: 18px; text-align: center; margin: 4px 0; }
            h2 { font-size: 15px; margin: 10px 0 4px; }
            h3 { font-size: 13px; margin: 8px 0 4px; }
            p { margin: 6px 0; }
            ul, ol { margin: 6px 0 6px 18px; }
            table { width: 100%; border-collapse: collapse; margin: 8px 0; }
            th, td { border: 1px solid #999; padding: 4px; text-align: left; vertical-align: top; }
            .parcelas th { background-color: #f0f0f0; }
            .meta { text-align: center; font-size: 11px; color: #555; }
            .section { margin-top: 16px; }
            .signatures { margin-top: 60px; }
            .sig { display: inline-block; width: 45%; text-align: center; border-top: 1px solid #000; }
            """;

    /** Embrulha um fragmento HTML no esqueleto XHTML com o CSS padrão. */
    public static String wrap(String fragment) {
        return "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                + "<style>" + CSS + "</style></head><body>"
                + (fragment != null ? fragment : "")
                + "</body></html>";
    }

    /**
     * Normaliza um HTML qualquer para XHTML bem-formado (sintaxe XML), exigido
     * pelo Flying Saucer. Fecha tags vazias, escapa entidades e usa numéricas.
     */
    public static String toXhtml(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8")
                .prettyPrint(false);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + doc.html();
    }

    /** Atalho: embrulha o fragmento e devolve XHTML pronto para o PDF. */
    public static String document(String fragment) {
        return toXhtml(wrap(fragment));
    }

    /**
     * Sanitiza um fragmento vindo do editor, removendo scripts/handlers e tags
     * perigosas, preservando formatação, tabelas e os chips de token
     * ({@code span[data-token]}). Se vier um documento completo, extrai o corpo.
     */
    public static String sanitizeFragment(String html) {
        if (html == null) return "";
        String fragment = extractFragment(html);
        Safelist safelist = Safelist.relaxed()
                .addTags("u", "s", "hr", "span", "div")
                .addAttributes(":all", "style", "class")
                .addAttributes("span", "data-token")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan");
        return Jsoup.clean(fragment, safelist);
    }

    /**
     * Se {@code html} for um documento completo (contém &lt;body&gt;), devolve o
     * conteúdo interno do body; caso contrário devolve o próprio HTML (já é
     * fragmento). Mantém compatibilidade com modelos legados em XHTML completo.
     */
    public static String extractFragment(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase();
        if (lower.contains("<body")) {
            Document doc = Jsoup.parse(html);
            doc.outputSettings().prettyPrint(false);
            return doc.body().html();
        }
        return html;
    }
}
