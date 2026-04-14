package it.lagioiaproductions.nutrislot.ui.weeklyplan.parser

private data class MealVisualRule(
    val semanticKey: String,
    val label: String,
    val emoji: String,
    val keywords: List<String>
)

private val MEAL_VISUAL_RULES = listOf(
    MealVisualRule("panino", "Panino", "\uD83E\uDD6A", listOf("panino", "panini")),
    MealVisualRule("piadina", "Piadina", "\uD83C\uDF2F", listOf("piadina")),
    MealVisualRule("frisella", "Frisella", "\uD83E\uDD6F", listOf("frisella", "friselle")),
    MealVisualRule("insalata", "Insalata", "\uD83E\uDD57", listOf("insalatona", "insalata")),
    MealVisualRule(
        "cereale_primo",
        "Primo",
        "\uD83C\uDF5D",
        listOf("pasta fredda", "pasta", "riso", "orzo", "farro", "couscous", "minestrone")
    ),
    MealVisualRule("pancake", "Pancake", "\uD83E\uDD5E", listOf("pancake")),
    MealVisualRule("latticino", "Latticino", "\uD83E\uDD5B", listOf("yogurt", "latte", "kefir")),
    MealVisualRule(
        "carne",
        "Carne",
        "\uD83C\uDF57",
        listOf("pollo", "tacchino", "hamburger", "carne", "bresaola", "prosciutto", "affettato")
    ),
    MealVisualRule("pesce", "Pesce", "\uD83D\uDC1F", listOf("pesce", "salmone", "tonno", "sgombro")),
    MealVisualRule("uova", "Uova", "\uD83E\uDD5A", listOf("uova", "uovo", "frittata")),
    MealVisualRule("pane", "Pane", "\uD83C\uDF5E", listOf("pane")),
    MealVisualRule(
        "colazione_secca",
        "Colazione",
        "\uD83E\uDD63",
        listOf("cereali", "cornflakes", "muesli", "granola", "porridge", "fette biscottate", "biscotti")
    ),
    MealVisualRule("banana", "Banana", "\uD83C\uDF4C", listOf("banana")),
    MealVisualRule("mela", "Mela", "\uD83C\uDF4E", listOf("mela")),
    MealVisualRule("pera", "Pera", "\uD83C\uDF50", listOf("pera")),
    MealVisualRule("cioccolato", "Cioccolato", "\uD83C\uDF6B", listOf("cioccolato")),
    MealVisualRule("frutta", "Frutta", "\uD83C\uDF53", listOf("frutta", "frutto", "fragole", "kiwi", "arancia")),
    MealVisualRule(
        "frutta_secca",
        "Frutta secca",
        "\uD83E\uDD5C",
        listOf("mandorle", "nocciole", "noci", "arachidi", "frutta secca")
    ),
    MealVisualRule("avocado", "Avocado", "\uD83E\uDD51", listOf("avocado")),
    MealVisualRule(
        "formaggio",
        "Formaggio",
        "\uD83E\uDDC0",
        listOf("parmigiano", "primo sale", "ricotta", "mozzarella", "philadelphia", "formaggio", "feta")
    ),
    MealVisualRule("pomodoro", "Pomodoro", "\uD83C\uDF45", listOf("pomodoro", "pomodorini")),
    MealVisualRule("carota", "Carota", "\uD83E\uDD55", listOf("carota", "carote")),
    MealVisualRule(
        "verdura",
        "Verdura",
        "\uD83E\uDD6C",
        listOf("verdura", "lattuga", "lattughino", "songino", "rughetta", "radicchio", "valeriana", "zucchine")
    ),
    MealVisualRule("olio", "Olio", "\uD83E\uDED2", listOf("olio", "olive")),
    MealVisualRule("dolce_spalmabile", "Dolce", "\uD83C\uDF6F", listOf("miele", "marmellata")),
    MealVisualRule("caffe", "Caffe", "\u2615", listOf("caffe", "caff\u00E8"))
)

private val MEAL_SEMANTIC_LABELS =
    MEAL_VISUAL_RULES.associate { rule -> rule.semanticKey to rule.label } +
            mapOf("meal_generic" to "Pasto")

internal fun mealSemanticLabel(semanticKey: String): String {
    return MEAL_SEMANTIC_LABELS[semanticKey] ?: "Pasto"
}

internal fun inferMealVisualInfo(text: String): MealVisualInfo {
    val normalized = text.normalizeMealParserMatchable()
    val matchedRule = MEAL_VISUAL_RULES.firstOrNull { rule ->
        rule.keywords.any { keyword -> normalized.contains(keyword) }
    }

    return if (matchedRule != null) {
        MealVisualInfo(
            emoji = matchedRule.emoji,
            semanticKey = matchedRule.semanticKey
        )
    } else {
        MealVisualInfo(
            emoji = "\uD83C\uDF7D\uFE0F",
            semanticKey = "meal_generic"
        )
    }
}
