package com.daniel.treino.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daniel.treino.TreinoApp
import com.daniel.treino.ui.agenda.AgendaScreen
import com.daniel.treino.ui.backup.BackupScreen
import com.daniel.treino.ui.components.LocalIconesTreinos
import com.daniel.treino.ui.components.LocalOrdemTreinos
import com.daniel.treino.ui.hoje.HojeScreen
import com.daniel.treino.ui.progresso.HistoricoScreen
import com.daniel.treino.ui.progresso.ProgressoScreen
import com.daniel.treino.ui.sessao.SessaoScreen
import com.daniel.treino.ui.treinos.EditorTreinoScreen
import com.daniel.treino.ui.treinos.ImportarHistoricoScreen
import com.daniel.treino.ui.treinos.TreinosScreen

// Reconstructed from S1/a.java, L2/e.java (methods a and d), S1/e..u.java, K2/m.java (cases 15-28), B0/C0085q.java (cases 4-5)

enum class Aba(
    val rota: String,
    val titulo: String,
    val icone: ImageVector,
) {
    Hoje("hoje", "Hoje", Icons.Outlined.Today),
    Treinos("treinos", "Treinos", Icons.Outlined.FitnessCenter),
    Agenda("agenda", "Agenda", Icons.Outlined.CalendarMonth),
    Relatorios("relatorios", "Relatórios", Icons.Outlined.BarChart),
}

@Composable
fun TreinoAppShell() {
    val app = LocalContext.current.applicationContext as TreinoApp
    val treinos by app.treinoRepository.treinos.collectAsState(initial = emptyList())
    val ordem = remember(treinos) {
        treinos
            .sortedBy { it.id }
            .mapIndexed { indice, treino -> treino.id to indice }
            .toMap()
    }
    val icones = remember(treinos) {
        treinos.associate { it.id to it.icone }
    }

    CompositionLocalProvider(
        LocalOrdemTreinos provides ordem,
        LocalIconesTreinos provides icones,
    ) {
        ConteudoShell()
    }
}

@Composable
private fun ConteudoShell() {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val mostrarBarra = Aba.entries.any { it.rota == rotaAtual }

    fun abrirSessao(id: Long) {
        navController.navigate("sessao/$id") {
            launchSingleTop = true
        }
    }

    fun editarTreino(id: Long) {
        navController.navigate("treino/$id")
    }

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = {
                                navController.navigate(aba.rota) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = aba.icone,
                                    contentDescription = aba.titulo,
                                )
                            },
                            label = {
                                Text(aba.titulo)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Aba.Hoje.rota,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(100))
            },
            exitTransition = {
                fadeOut(tween(80))
            },
            popEnterTransition = {
                fadeIn(tween(100))
            },
            popExitTransition = {
                fadeOut(tween(80))
            },
        ) {
            composable(Aba.Hoje.rota) {
                HojeScreen(
                    abrirSessao = ::abrirSessao,
                    abrirBackup = { navController.navigate("backup") },
                )
            }
            composable(Aba.Treinos.rota) {
                TreinosScreen(
                    aoEditar = ::editarTreino,
                    aoCriar = { navController.navigate("treino/-1") },
                    aoImportar = { navController.navigate("importar") },
                )
            }
            composable(Aba.Agenda.rota) {
                AgendaScreen()
            }
            composable(Aba.Relatorios.rota) {
                ProgressoScreen(
                    abrirHistorico = { navController.navigate("historico") },
                )
            }
            composable(
                route = "treino/{id}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                ),
            ) {
                EditorTreinoScreen(
                    voltar = { navController.popBackStack() },
                )
            }
            composable("backup") {
                BackupScreen(
                    voltar = { navController.popBackStack() },
                )
            }
            composable("historico") {
                HistoricoScreen(
                    voltar = { navController.popBackStack() },
                )
            }
            composable("importar") {
                ImportarHistoricoScreen(
                    voltar = { navController.popBackStack() },
                )
            }
            composable(
                route = "sessao/{id}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                ),
            ) {
                SessaoScreen(
                    voltar = { navController.popBackStack() },
                )
            }
        }
    }
}
