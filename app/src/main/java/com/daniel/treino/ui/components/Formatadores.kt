package com.daniel.treino.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Reconstructed from S1/v.java

object Formatadores {
    val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    private val numero: DecimalFormat = DecimalFormat("0.##", DecimalFormatSymbols(ptBr))
    private val inteiro: DecimalFormat = DecimalFormat("#,##0.#", DecimalFormatSymbols(ptBr))
    private val dataCurta: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", ptBr)
    private val diaMes: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", ptBr)

    fun formatarPeso(kg: Double?): String =
        if (kg == null) "" else numero.format(kg)

    fun formatarVolume(kg: Double): String =
        if (kg >= 1000.0) {
            "${numero.format(kg / 1000.0)} t"
        } else {
            "${inteiro.format(kg)} kg"
        }

    fun formatarData(epochMillis: Long, semAno: Boolean = false): String {
        val data = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return data.format(if (semAno) diaMes else dataCurta)
    }

    fun nomeDiaSemana(diaSemana: Int, estilo: TextStyle = TextStyle.SHORT): String {
        val bruto = DayOfWeek.of(diaSemana)
            .getDisplayName(estilo, ptBr)
            .trimEnd('.')
        return bruto.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(ptBr) else it.toString()
        }
    }

    fun lerInteiro(texto: String): Int? =
        texto.trim().toIntOrNull()

    fun lerDecimal(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()
}

val PtBr: Locale get() = Formatadores.ptBr

fun formatarCarga(kg: Double?): String = Formatadores.formatarPeso(kg)

fun formatarVolume(kg: Double): String = Formatadores.formatarVolume(kg)

fun formatarDataCurta(epochMillis: Long, semAno: Boolean = false): String =
    Formatadores.formatarData(epochMillis, semAno)
