package it.lagioiaproductions.nutrislot.ui.scanner

import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

internal data class ScannerProductUi(
    val id: Int,
    val name: String,
    val subtitle: String,
    val barcode: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int
)

internal enum class ScannerMode(
    val label: String
) {
    BARCODE("Barcode"),
    MANUAL("Ricerca manuale")
}

internal fun scannerDemoProducts(): List<ScannerProductUi> {
    return listOf(
        ScannerProductUi(
            id = 1,
            name = "Cereali integrali",
            subtitle = "Porzione 45 g • colazione",
            barcode = "8001234567890",
            calories = 200,
            protein = 8,
            carbs = 32,
            fibre = 5
        ),
        ScannerProductUi(
            id = 2,
            name = "Yogurt greco bianco",
            subtitle = "Vasetto 170 g • snack",
            barcode = "8005550001112",
            calories = 145,
            protein = 15,
            carbs = 6,
            fibre = 0
        ),
        ScannerProductUi(
            id = 3,
            name = "Pane proteico",
            subtitle = "2 fette • pranzo",
            barcode = "8019993334445",
            calories = 180,
            protein = 14,
            carbs = 18,
            fibre = 7
        )
    )
}

internal fun ScannerProductUi.toLinkedProduct(): LinkedScannedProductUi {
    return LinkedScannedProductUi(
        name = name,
        subtitle = subtitle,
        barcode = barcode,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fibre = fibre
    )
}