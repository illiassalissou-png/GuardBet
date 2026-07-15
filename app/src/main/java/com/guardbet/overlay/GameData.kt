package com.guardbet.overlay

data class CasinoGame(
    val name: String,
    val rtpPercent: Double,
    val houseEdgePercent: Double,
    val note: String
)

object GameData {
    val games = listOf(
        CasinoGame(
            "Blackjack (stratégie de base)",
            99.5, 0.5,
            "Le meilleur taux si tu appliques la stratégie de base parfaite. Sans stratégie, l'avantage maison grimpe à 2-5%."
        ),
        CasinoGame(
            "Roulette européenne (1 zéro)",
            97.3, 2.7,
            "Nettement mieux que la version américaine. Toujours
