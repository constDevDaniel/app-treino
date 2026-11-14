package com.daniel.treino

import com.daniel.treino.data.planilha.LeitorPlanilha
import com.daniel.treino.data.planilha.normalizarTexto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class LeitorPlanilhaTest {

    @Test
    fun `lerCsv com ponto e virgula agrupa sessoes por data e treino`() {
        val csv = """
            Data;Treino;Exercício;Repetições;Carga
            10/03/2025;Treino A;Supino Reto;10;80,5 kg
            10/03/2025;Treino A;Supino Reto;8;85 kg
            12/03/2025;Treino B;Agachamento Livre;12;100
        """.trimIndent()

        val resultado = LeitorPlanilha.ler(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        assertTrue(resultado.avisos.isEmpty())
        assertEquals(2, resultado.sessoes.size)

        val sessao1 = resultado.sessoes[0]
        assertEquals(LocalDate.of(2025, 3, 10), sessao1.data)
        assertEquals("Treino A", sessao1.treinoInformado)
        assertEquals(2, sessao1.linhas.size)
        assertEquals("Supino Reto", sessao1.linhas[0].exercicio)
        assertEquals(10, sessao1.linhas[0].repeticoes)
        assertEquals(80.5, sessao1.linhas[0].carga!!, 0.001)
        assertEquals(85.0, sessao1.linhas[1].carga!!, 0.001)

        val sessao2 = resultado.sessoes[1]
        assertEquals(LocalDate.of(2025, 3, 12), sessao2.data)
        assertEquals("Treino B", sessao2.treinoInformado)
        assertEquals(1, sessao2.linhas.size)
        assertEquals(100.0, sessao2.linhas[0].carga!!, 0.001)
    }

    @Test
    fun `lerCsv com virgula e aspas trata valores com virgula e linhas invalidas`() {
        val csv = """
            data,exercicio,repeticoes,carga
            2025-04-01,"Remada Curvada, Pegada Pronada",12,"62,5"
            2025-04-01,,10,50
            data-invalida,Rosca Direta,10,20
            2025-04-02,Pulley Frente,15,
        """.trimIndent()

        val resultado = LeitorPlanilha.ler(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        assertEquals(2, resultado.sessoes.size)
        assertEquals(2, resultado.avisos.size)

        val primeiraLinha = resultado.sessoes[0].linhas[0]
        assertEquals("Remada Curvada, Pegada Pronada", primeiraLinha.exercicio)
        assertEquals(12, primeiraLinha.repeticoes)
        assertEquals(62.5, primeiraLinha.carga!!, 0.001)

        val segundaSessaoLinha = resultado.sessoes[1].linhas[0]
        assertEquals("Pulley Frente", segundaSessaoLinha.exercicio)
        assertNull(segundaSessaoLinha.carga)
    }

    @Test
    fun `analisar retorna aviso quando cabecalho obrigatorio nao existe`() {
        val csv = """
            Nome;Observacao
            Teste;Sem colunas obrigatorias
        """.trimIndent()

        val resultado = LeitorPlanilha.ler(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        assertTrue(resultado.sessoes.isEmpty())
        assertEquals(1, resultado.avisos.size)
    }

    @Test
    fun `normalizarTexto remove acentos e pontuacao`() {
        assertEquals("exercicio de supino", normalizarTexto("Exercício de Supino!"))
        assertEquals("repeticoes", normalizarTexto("  Repetições  "))
    }
}
