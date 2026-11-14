package com.daniel.treino

import com.daniel.treino.ui.components.Formatadores
import com.daniel.treino.ui.components.formatarCarga
import com.daniel.treino.ui.components.formatarDataCurta
import com.daniel.treino.ui.components.formatarVolume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FormatadoresTest {

    @Test
    fun `formatarCarga formata nulo, inteiros e decimais em pt-BR`() {
        assertEquals("", formatarCarga(null))
        assertEquals("0", formatarCarga(0.0))
        assertEquals("80", formatarCarga(80.0))
        assertEquals("82,5", formatarCarga(82.5))
        assertEquals("12,25", formatarCarga(12.25))
        assertEquals("12,35", formatarCarga(12.345))
    }

    @Test
    fun `formatarVolume alterna entre kg e toneladas`() {
        assertEquals("0 kg", formatarVolume(0.0))
        assertEquals("500 kg", formatarVolume(500.0))
        assertEquals("999,5 kg", formatarVolume(999.5))
        assertEquals("1 t", formatarVolume(1000.0))
        assertEquals("1,5 t", formatarVolume(1500.0))
        assertEquals("2,4 t", formatarVolume(2350.0))
    }

    @Test
    fun `formatarDataCurta respeita flag semAno`() {
        val epochMillis = LocalDate.of(2025, 3, 15)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals("15/03/25", formatarDataCurta(epochMillis, semAno = false))
        assertEquals("15/03", formatarDataCurta(epochMillis, semAno = true))
    }

    @Test
    fun `lerInteiro e lerDecimal tratam espacos e virgula`() {
        assertEquals(12, Formatadores.lerInteiro(" 12 "))
        assertNull(Formatadores.lerInteiro("abc"))

        assertEquals(82.5, Formatadores.lerDecimal(" 82,5 ")!!, 0.0001)
        assertEquals(82.5, Formatadores.lerDecimal("82.5")!!, 0.0001)
        assertNull(Formatadores.lerDecimal(""))
    }
}
