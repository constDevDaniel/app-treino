package com.daniel.treino.data.planilha

import android.util.Xml
import com.daniel.treino.data.backup.BackupInvalidoException
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TreeMap
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong

enum class Coluna(val obrigatoria: Boolean, val corresponde: (String) -> Boolean) {
    DATA(true, { n -> n.startsWith("data") || n == "dia" }),
    EXERCICIO(true, { n -> n.contains("exerc") }),
    REPETICOES(false, { n -> n.startsWith("rep") || n == "qtd repeticoes" || n.contains("repeticoes") }),
    CARGA(false, { n -> n.startsWith("carga") || n.startsWith("peso") || n == "kg" || n.contains("carga") }),
    TREINO(false, { n -> n.contains("treino") })
}

data class LinhaPlanilha(
    val numero: Int,
    val data: LocalDate,
    val exercicio: String,
    val repeticoes: Int?,
    val carga: Double?,
    val treino: String?
)

data class SessaoPlanilha(
    val data: LocalDate,
    val treinoInformado: String?,
    val linhas: List<LinhaPlanilha>
) {
    fun exercicios(): List<String> =
        linhas.map { it.exercicio }.distinctBy { normalizarTexto(it) }
}

data class ResultadoPlanilha(
    val sessoes: List<SessaoPlanilha>,
    val avisos: List<String>
)

fun normalizarTexto(s: String): String {
    val semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return semAcento
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

object LeitorPlanilha {
    private val EPOCH_EXCEL: LocalDate = LocalDate.of(1899, 12, 30)
    private val FORMATOS_DATA: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yy"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("d.M.yyyy")
    )

    fun ler(inputStream: InputStream): ResultadoPlanilha {
        return analisar(lerTabela(inputStream))
    }

    fun lerTabela(inputStream: InputStream): List<List<String?>> {
        val bytes = inputStream.readBytes()
        if (bytes.size > 4 &&
            bytes[0] == 0xD0.toByte() &&
            bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() &&
            bytes[3] == 0xE0.toByte()
        ) {
            throw BackupInvalidoException(
                "Arquivo .xls (Excel antigo) não é suportado. No Excel, use Salvar como → Pasta de Trabalho do Excel (.xlsx)."
            )
        }

        return if (bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            lerXlsx(bytes)
        } else {
            lerCsv(bytes)
        }
    }

    private fun lerCsv(bytes: ByteArray): List<List<String?>> {
        val texto = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        val primeiraLinha = texto.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val separador = listOf(';', '\t', ',').maxBy { sep ->
            primeiraLinha.count { it == sep }
        }

        val resultado = ArrayList<List<String?>>()
        var linhaAtual = ArrayList<String?>()
        val celula = StringBuilder()
        var entreAspas = false
        var i = 0

        fun adicionarCelula() {
            val valor = celula.toString().trim().takeIf { it.isNotEmpty() }
            linhaAtual.add(valor)
            celula.setLength(0)
        }

        while (i < texto.length) {
            val c = texto[i]
            if (entreAspas && c == '"') {
                val proximo = texto.getOrNull(i + 1)
                if (proximo == '"') {
                    celula.append('"')
                    i += 2
                    continue
                }
            }
            if (c == '"') {
                entreAspas = !entreAspas
                i++
            } else {
                when {
                    !entreAspas && c == separador -> adicionarCelula()
                    !entreAspas && (c == '\n' || c == '\r') -> {
                        if (c == '\r' && texto.getOrNull(i + 1) == '\n') {
                            i++
                        }
                        adicionarCelula()
                        if (linhaAtual.any { it != null }) {
                            resultado.add(linhaAtual)
                        }
                        linhaAtual = ArrayList()
                    }
                    else -> celula.append(c)
                }
                i++
            }
        }
        adicionarCelula()
        if (linhaAtual.any { it != null }) {
            resultado.add(linhaAtual)
        }
        return resultado
    }

    private fun lerXlsx(bytes: ByteArray): List<List<String?>> {
        val arquivos = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val entradas = generateSequence { zip.nextEntry }
            for (entrada in entradas) {
                val nome = entrada.name
                if (nome.endsWith(".xml") || nome.endsWith(".rels")) {
                    arquivos[nome] = zip.readBytes()
                }
            }
        }

        val sharedStrings: List<String> = arquivos["xl/sharedStrings.xml"]?.let { ssBytes ->
            val lista = ArrayList<String>()
            val parser = novoParserXml(ssBytes)
            var dentroDeT = false
            var textoSi: StringBuilder? = null
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "si" -> textoSi = StringBuilder()
                        "t" -> dentroDeT = true
                        "rPh" -> dentroDeT = false
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "t" -> dentroDeT = false
                        "si" -> {
                            lista.add(textoSi.toString())
                            textoSi = null
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (dentroDeT && textoSi != null) {
                            textoSi.append(parser.text)
                        }
                    }
                }
            }
            lista
        } ?: emptyList()

        var caminhoAba: String? = null
        val workbookBytes = arquivos["xl/workbook.xml"]
        val relsBytes = arquivos["xl/_rels/workbook.xml.rels"]
        if (workbookBytes != null && relsBytes != null) {
            val wbParser = novoParserXml(workbookBytes)
            var relId: String? = null
            while (relId == null && wbParser.next() != XmlPullParser.END_DOCUMENT) {
                if (wbParser.eventType == XmlPullParser.START_TAG && wbParser.name == "sheet") {
                    relId = wbParser.getAttributeValue(null, "r:id")
                }
            }
            if (relId != null) {
                val relParser = novoParserXml(relsBytes)
                while (relParser.next() != XmlPullParser.END_DOCUMENT) {
                    if (relParser.eventType == XmlPullParser.START_TAG &&
                        relParser.name == "Relationship" &&
                        relParser.getAttributeValue(null, "Id") == relId
                    ) {
                        val target = relParser.getAttributeValue(null, "Target")
                        if (target != null) {
                            caminhoAba = if (target.startsWith("/")) {
                                target.removePrefix("/")
                            } else {
                                "xl/" + target.removePrefix("./")
                            }
                            break
                        }
                    }
                }
            }
        }

        val caminhoFinal = caminhoAba ?: "xl/worksheets/sheet1.xml"
        val sheetBytes = arquivos[caminhoFinal]
            ?: arquivos.keys.filter { it.startsWith("xl/worksheets/sheet") }.sorted().firstOrNull()?.let { arquivos[it] }
            ?: throw IllegalArgumentException("Não encontrei nenhuma aba na planilha.")

        val linhasPorNumero = TreeMap<Int, MutableMap<Int, String?>>()
        val parser = novoParserXml(sheetBytes)
        var lendoValor = false
        var proxColuna = 0
        var numLinha = 0
        var colAtual = 0
        var valorCelula: StringBuilder? = null
        var tipoCelula: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        val r = parser.getAttributeValue(null, "r")?.toIntOrNull()
                        numLinha = r ?: (numLinha + 1)
                        proxColuna = 0
                    }
                    "c" -> {
                        val ref = parser.getAttributeValue(null, "r")
                        if (ref != null) {
                            var col = 0
                            for (ch in ref) {
                                if (!ch.isLetter()) break
                                col = col * 26 + (ch.uppercaseChar() - '@')
                            }
                            proxColuna = col - 1
                        }
                        colAtual = proxColuna
                        proxColuna = colAtual + 1
                        tipoCelula = parser.getAttributeValue(null, "t")
                        valorCelula = StringBuilder()
                    }
                    "v", "t" -> {
                        lendoValor = valorCelula != null
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> lendoValor = false
                    "c" -> {
                        val bruto = valorCelula?.toString()
                        val resolvido = when (tipoCelula) {
                            "s" -> bruto?.trim()?.toIntOrNull()?.let { sharedStrings.getOrNull(it) }
                            "b" -> if (bruto == "1") "VERDADEIRO" else "FALSO"
                            else -> bruto
                        }?.takeIf { it.isNotBlank() }

                        linhasPorNumero.getOrPut(numLinha) { LinkedHashMap() }[colAtual] = resolvido
                        valorCelula = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (lendoValor && valorCelula != null) {
                        valorCelula.append(parser.text)
                    }
                }
            }
        }

        return linhasPorNumero.values.map { mapaColunas ->
            val maxCol = mapaColunas.keys.maxOrNull() ?: -1
            (0..maxCol).map { mapaColunas[it] }
        }
    }

    private fun novoParserXml(bytes: ByteArray): XmlPullParser {
        return Xml.newPullParser().apply {
            setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false)
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }
    }

    fun analisar(tabela: List<List<String?>>): ResultadoPlanilha {
        val indiceCabecalho = (0 until minOf(tabela.size, 10)).firstOrNull { idx ->
            val normalizadas = tabela[idx].map { normalizarTexto(it ?: "") }
            Coluna.entries.filter { it.obrigatoria }.all { col ->
                normalizadas.any { col.corresponde(it) }
            }
        } ?: return ResultadoPlanilha(
            sessoes = emptyList(),
            avisos = listOf("Não encontrei as colunas de Data e Exercício no cabeçalho.")
        )

        val cabecalhoNorm = tabela[indiceCabecalho].map { normalizarTexto(it ?: "") }
        val mapaColunas = LinkedHashMap<Coluna, Int>()
        for (col in Coluna.entries) {
            val idx = cabecalhoNorm.indexOfFirst { col.corresponde(it) }
            if (idx >= 0) {
                mapaColunas[col] = idx
            }
        }

        val avisos = ArrayList<String>()
        val linhasValidas = ArrayList<LinhaPlanilha>()

        fun lerCelula(linha: List<String?>, coluna: Coluna): String? {
            val idx = mapaColunas[coluna] ?: return null
            return linha.getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }
        }

        val dadosBrutos = tabela.drop(indiceCabecalho + 1)
        for ((offset, linha) in dadosBrutos.withIndex()) {
            val numeroLinha = indiceCabecalho + offset + 2
            if (!linha.all { it.isNullOrBlank() }) {
                val exercicioRaw = lerCelula(linha, Coluna.EXERCICIO)
                val dataRaw = lerCelula(linha, Coluna.DATA)

                if (exercicioRaw != null || dataRaw != null) {
                    if (exercicioRaw == null) {
                        avisos.add("Linha $numeroLinha ignorada: exercício em branco.")
                    } else if (dataRaw == null) {
                        avisos.add("Linha $numeroLinha ignorada: data em branco.")
                    } else {
                        val data = analisarData(dataRaw)
                        if (data == null) {
                            avisos.add("Linha $numeroLinha ignorada: data \"$dataRaw\" não reconhecida.")
                        } else {
                            val repsRaw = lerCelula(linha, Coluna.REPETICOES)
                            val repeticoes = repsRaw?.let { s ->
                                s.replace(',', '.').toDoubleOrNull()?.toInt()
                                    ?: Regex("\\d+").find(s)?.value?.toIntOrNull()
                            }

                            val cargaRaw = lerCelula(linha, Coluna.CARGA)
                            val carga = cargaRaw?.let { analisarCarga(it) }

                            linhasValidas.add(
                                LinhaPlanilha(
                                    numero = numeroLinha,
                                    data = data,
                                    exercicio = exercicioRaw.trim().replace(Regex("\\s+"), " "),
                                    repeticoes = repeticoes,
                                    carga = carga,
                                    treino = lerCelula(linha, Coluna.TREINO)
                                )
                            )
                        }
                    }
                }
            }
        }

        val porChave = LinkedHashMap<Pair<LocalDate, String?>, MutableList<LinhaPlanilha>>()
        for (item in linhasValidas) {
            val chave = item.data to item.treino?.let { normalizarTexto(it) }
            porChave.getOrPut(chave) { ArrayList() }.add(item)
        }

        val sessoes = porChave.map { (chave, itens) ->
            val data = chave.first
            val treinoInformado = itens.firstNotNullOfOrNull { it.treino }
            SessaoPlanilha(
                data = data,
                treinoInformado = treinoInformado,
                linhas = itens
            )
        }.sortedWith(compareBy { it.data })

        return ResultadoPlanilha(sessoes = sessoes, avisos = avisos)
    }

    private fun analisarData(valor: String): LocalDate? {
        val limpo = valor.trim().substringBefore(' ').substringBefore('T')
        val numero = limpo.replace(',', '.').toDoubleOrNull()
        if (numero != null && numero in 30000.0..80000.0) {
            return EPOCH_EXCEL.plusDays(numero.toLong())
        }
        for (fmt in FORMATOS_DATA) {
            val parsed = runCatching { LocalDate.parse(limpo, fmt) }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun analisarCarga(valor: String): Double? {
        val limpo = valor.lowercase(Locale.ROOT)
            .replace("kg", "")
            .trim()
        if (limpo.isEmpty()) return null

        val normalizado = if (',' in limpo && '.' in limpo) {
            if (limpo.lastIndexOf(',') > limpo.lastIndexOf('.')) {
                limpo.replace(".", "").replace(',', '.')
            } else {
                limpo.replace(",", "")
            }
        } else {
            limpo.replace(',', '.')
        }

        val d = normalizado.toDoubleOrNull() ?: return null
        if (d < 0.0) return null
        return (d * 100.0).roundToLong() / 100.0
    }
}
