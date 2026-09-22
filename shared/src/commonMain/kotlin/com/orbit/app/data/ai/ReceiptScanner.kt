package com.orbit.app.data.ai

import com.orbit.app.domain.model.ScannedReceipt

/** OCR + on-device model: receipt photo to structured lines. Null when nothing could be read. */
interface ReceiptScanner {
    suspend fun scan(imageBytes: ByteArray): ScannedReceipt?
}
