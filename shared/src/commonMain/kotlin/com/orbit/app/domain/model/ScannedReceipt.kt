package com.orbit.app.domain.model

data class ScannedReceipt(
    val merchant: String?,
    val totalCents: Long,
    val lines: List<Line>,
) {
    data class Line(val label: String, val amountCents: Long)
}
