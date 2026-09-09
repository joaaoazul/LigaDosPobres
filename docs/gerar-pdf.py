# -*- coding: utf-8 -*-
"""Gera os PDF de docs/pdf/ a partir dos markdown de docs/.

    python docs/gerar-pdf.py

Nao precisa de nada instalado alem do Python e de um Chrome ou Edge, que faz a
impressao em modo headless. O HTML intermedio fica em docs/pdf/ e pode ser
aberto no browser para ver o resultado sem gerar o PDF.

O conversor de markdown cobre exactamente o que estes tres documentos usam:
titulos, tabelas, blocos de codigo, citacoes, listas, regras horizontais,
negrito, italico, codigo em linha e ligacoes. Nao e um markdown completo, e
nao vale a pena que seja.
"""
import html
import io
import os
import re
import subprocess
import sys
import time

CSS = u"""
@font-face { font-family:"Outfit"; src:url("FONTDIR/outfit.woff2") format("woff2");
             font-weight:100 900; font-display:swap; }

@page { size:A4; margin:19mm 17mm 20mm; }

:root { --sinal:#d8410b; --tinta:#16181c; --media:#5a626c; --fraca:#8b939d; --regua:#dcdfe4; }

* { box-sizing:border-box; }
body { font-family:"Outfit",system-ui,sans-serif; font-size:10.2pt; line-height:1.55;
       color:var(--tinta); margin:0; }
.numero, td.n { font-variant-numeric:tabular-nums; }

h1,h2,h3,h4 { font-family:"Outfit",sans-serif; font-weight:700;
              line-height:1.2; margin:0; }
h1 { font-size:23pt; letter-spacing:-.01em; border-bottom:2.5pt solid var(--sinal);
     padding-bottom:5mm; margin-bottom:7mm; }
h2 { font-size:14pt; text-transform:uppercase; letter-spacing:.045em;
     margin:9mm 0 3mm; padding-bottom:1.6mm; border-bottom:.6pt solid var(--regua);
     break-after:avoid; }
h3 { font-size:11.2pt; margin:6mm 0 2mm; break-after:avoid; }
h4 { font-size:10pt; margin:4mm 0 1.5mm; color:var(--media); break-after:avoid; }

p { margin:0 0 3mm; }
ul,ol { margin:0 0 3mm; padding-left:6mm; }
li { margin-bottom:1.2mm; }
strong { font-weight:700; }
a { color:var(--sinal); text-decoration:none; }

code { font-family:ui-monospace,"Cascadia Mono",Consolas,monospace; font-size:8.8pt;
       background:#f2f3f5; border:.4pt solid var(--regua); border-radius:2pt;
       padding:.3mm 1.1mm; }
pre { background:#f7f8f9; border:.5pt solid var(--regua); border-left:2pt solid var(--sinal);
      border-radius:2pt; padding:3mm 4mm; margin:0 0 4mm; overflow-wrap:anywhere;
      white-space:pre-wrap; break-inside:avoid; }
pre code { background:none; border:none; padding:0; font-size:8.6pt; }

table { border-collapse:collapse; width:100%; margin:0 0 4mm; font-size:9.4pt;
        break-inside:avoid; }
th,td { border-bottom:.5pt solid var(--regua); padding:1.8mm 2.4mm; text-align:left;
        vertical-align:top; }
th { font-family:"Outfit",sans-serif; font-size:8.4pt; font-weight:700;
     text-transform:uppercase; letter-spacing:.05em; color:var(--media);
     border-bottom:1pt solid var(--fraca); }

blockquote { margin:0 0 4mm; padding:2.5mm 4mm; background:#fdf6f2;
             border-left:2pt solid var(--sinal); break-inside:avoid; }
blockquote p:last-child { margin-bottom:0; }

hr { border:none; border-top:.6pt solid var(--regua); margin:7mm 0; }

.rodape { margin-top:10mm; padding-top:3mm; border-top:.6pt solid var(--regua);
          color:var(--fraca); font-size:8.4pt; }
"""


def em_linha(t):
    t = html.escape(t, quote=False)
    t = re.sub(r'`([^`]+)`', lambda m: u'<code>%s</code>' % m.group(1), t)
    t = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', t)
    # Italico so com asteriscos. Underscores ficam de fora de proposito: os
    # documentos estao cheios de PODE_CRIAR_LIGAS e POSTGRES_DB, e trata-los
    # como italico partia esses nomes ao meio.
    t = re.sub(r'\*(?=\S)([^*]+?)(?<=\S)\*', r'<em>\1</em>', t)
    t = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', t)
    return t


def celulas(linha):
    return [c.strip() for c in linha.strip().strip('|').split('|')]


def converter(md):
    saida, i, linhas = [], 0, md.split('\n')
    while i < len(linhas):
        l = linhas[i]

        if l.startswith('```'):
            i += 1
            corpo = []
            while i < len(linhas) and not linhas[i].startswith('```'):
                corpo.append(html.escape(linhas[i], quote=False))
                i += 1
            saida.append(u'<pre><code>%s</code></pre>' % u'\n'.join(corpo))
            i += 1
            continue

        if re.match(r'^\s*$', l):
            i += 1
            continue

        if re.match(r'^---+\s*$', l):
            saida.append(u'<hr>')
            i += 1
            continue

        m = re.match(r'^(#{1,4})\s+(.*)$', l)
        if m:
            n = len(m.group(1))
            saida.append(u'<h%d>%s</h%d>' % (n, em_linha(m.group(2)), n))
            i += 1
            continue

        # tabela: cabecalho, separador, linhas
        if l.strip().startswith('|') and i + 1 < len(linhas) \
                and re.match(r'^\s*\|[\s:|-]+\|\s*$', linhas[i + 1]):
            cab = celulas(l)
            i += 2
            corpo = []
            while i < len(linhas) and linhas[i].strip().startswith('|'):
                corpo.append(celulas(linhas[i]))
                i += 1
            t = [u'<table><thead><tr>']
            t += [u'<th>%s</th>' % em_linha(c) for c in cab]
            t.append(u'</tr></thead><tbody>')
            for linha in corpo:
                t.append(u'<tr>' + u''.join(u'<td>%s</td>' % em_linha(c) for c in linha) + u'</tr>')
            t.append(u'</tbody></table>')
            saida.append(u''.join(t))
            continue

        if l.startswith('>'):
            corpo = []
            while i < len(linhas) and linhas[i].startswith('>'):
                corpo.append(linhas[i].lstrip('>').strip())
                i += 1
            paras = u' '.join(corpo).split('  ')
            saida.append(u'<blockquote>%s</blockquote>'
                         % u''.join(u'<p>%s</p>' % em_linha(p) for p in paras if p))
            continue

        m = re.match(r'^(\s*)([-*]|\d+\.)\s+(.*)$', l)
        if m:
            ordenada = bool(re.match(r'^\d+\.$', m.group(2)))
            itens = []
            while i < len(linhas):
                mm = re.match(r'^(\s*)([-*]|\d+\.)\s+(.*)$', linhas[i])
                if not mm:
                    # continuacao indentada do item anterior
                    if itens and linhas[i].startswith('  ') and linhas[i].strip():
                        itens[-1] += u' ' + linhas[i].strip()
                        i += 1
                        continue
                    break
                itens.append(mm.group(3))
                i += 1
            # A formatacao e aplicada ao item ja junto, nunca linha a linha:
            # um **negrito** que atravesse a quebra de linha ficava por
            # converter e os asteriscos apareciam no documento final.
            tag = u'ol' if ordenada else u'ul'
            saida.append(u'<%s>%s</%s>'
                         % (tag, u''.join(u'<li>%s</li>' % em_linha(x) for x in itens), tag))
            continue

        # paragrafo
        corpo = []
        while i < len(linhas) and linhas[i].strip() and not re.match(
                r'^(#{1,4}\s|```|>|\s*([-*]|\d+\.)\s|---+\s*$)', linhas[i]) \
                and not linhas[i].strip().startswith('|'):
            corpo.append(linhas[i].strip())
            i += 1
        if corpo:
            saida.append(u'<p>%s</p>' % em_linha(u' '.join(corpo)))
        else:
            i += 1

    return u'\n'.join(saida)


DOCUMENTOS = [
    (u'MANUAL', u'Manual do Quota', u'Quota \u00b7 Manual de utiliza\u00e7\u00e3o'),
    (u'MANUAL_TREINADOR', u'Manual do treinador \u00b7 Quota', u'Quota \u00b7 Manual do treinador'),
    (u'DESENVOLVIMENTO', u'Quota \u00b7 Manual t\u00e9cnico', u'Quota \u00b7 Manual t\u00e9cnico'),
    (u'API', u'Quota \u00b7 Refer\u00eancia da API', u'Quota \u00b7 Refer\u00eancia da API'),
]

BROWSERS = [
    r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Google\Chrome\Application\chrome.exe',
    '/usr/bin/microsoft-edge',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
]


def procurar_browser():
    for caminho in BROWSERS:
        if os.path.exists(caminho):
            return caminho
    return None


def esperar_por(caminho, segundos=30):
    fim = time.time() + segundos
    while time.time() < fim:
        # Um PDF acabado termina sempre em %%EOF; enquanto o browser esta a
        # escrever, o ficheiro ja existe mas ainda esta a meio.
        try:
            with io.open(caminho, 'rb') as f:
                f.seek(-8, 2)
                if b'%%EOF' in f.read():
                    return True
        except (IOError, OSError, ValueError):
            pass
        time.sleep(0.3)
    return False


def escrever_html(raiz, nome, titulo, rodape):
    fontes = 'file:///' + os.path.join(raiz, 'src', 'main', 'resources',
                                       'static', 'fontes').replace('\\', '/')
    md = io.open(os.path.join(raiz, 'docs', nome + '.md'), encoding='utf-8').read()
    doc = (u'<!doctype html><html lang="pt"><head><meta charset="utf-8">'
           u'<title>%s</title><style>%s</style></head><body>%s'
           u'<div class="rodape">%s</div></body></html>'
           % (html.escape(titulo), CSS.replace('FONTDIR', fontes),
              converter(md), html.escape(rodape)))
    destino = os.path.join(raiz, 'docs', 'pdf', nome + '.html')
    io.open(destino, 'w', encoding='utf-8').write(doc)
    return destino


def main():
    raiz = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    pasta = os.path.join(raiz, 'docs', 'pdf')
    if not os.path.isdir(pasta):
        os.makedirs(pasta)
    browser = procurar_browser()
    if browser is None:
        sys.exit(u'Nao encontrei o Chrome nem o Edge. Acrescenta o caminho a BROWSERS.')
    for nome, titulo, rodape in DOCUMENTOS:
        origem = escrever_html(raiz, nome, titulo, rodape)
        pdf = os.path.join(pasta, nome + '.pdf')
        subprocess.check_call([
            browser, '--headless', '--disable-gpu', '--no-pdf-header-footer',
            '--virtual-time-budget=6000', '--print-to-pdf=' + pdf,
            'file:///' + origem.replace('\\', '/'),
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        # O Edge devolve o controlo antes de acabar de escrever o ficheiro, por
        # isso nao basta o processo terminar: e preciso esperar pelo PDF.
        if not esperar_por(pdf):
            sys.exit(u'O browser nao escreveu %s.' % pdf)
        print(u'%s.pdf' % nome)


if __name__ == '__main__':
    main()
