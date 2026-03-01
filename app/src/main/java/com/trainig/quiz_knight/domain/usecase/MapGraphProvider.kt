package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.Settlement

/**
 * Provides the static map graph (settlements + connections).
 * Injected into use cases that need topology knowledge.
 * Implemented in the data layer.
 */
interface MapGraphProvider {
    fun getAllSettlements(): List<Settlement>
    fun getSettlement(id: String): Settlement?
}

