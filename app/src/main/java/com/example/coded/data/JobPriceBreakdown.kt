package com.example.coded.data

data class JobPriceBreakdown(
    val basePrice: Double = 0.0,
    val vegetationSurcharge: Double = 0.0,
    val growthSurcharge: Double = 0.0,
    val terrainSurcharge: Double = 0.0,
    val serviceSurcharge: Double = 0.0,
    val disposalFee: Double = 0.0,
    val travelFee: Double = 0.0,
    val urgencyFee: Double = 0.0,
    val subtotal: Double = 0.0,
    val mobileMoneyFee: Double = 0.0,
    val vat: Double = 0.0,
    val totalAmount: Double = 0.0,
    val estimatedHours: Double = 0.0
) {
    // Format total amount for display
    val formattedTotal: String
        get() = String.format("E%.2f", totalAmount)

    // Format estimated time
    val formattedEstimatedTime: String
        get() = "~${estimatedHours.toInt()}h"

    // Get detailed breakdown for UI (without platform fee)
    fun getDetailedBreakdown(): List<Pair<String, String>> {
        val breakdown = mutableListOf<Pair<String, String>>()

        // Base price
        if (basePrice > 0) {
            breakdown.add("Base Service" to String.format("E%.2f", basePrice))
        }

        // Vegetation surcharge
        if (vegetationSurcharge > 0) {
            breakdown.add("Vegetation Type" to String.format("E%.2f", vegetationSurcharge))
        }

        // Growth surcharge
        if (growthSurcharge > 0) {
            breakdown.add("Growth Stage" to String.format("E%.2f", growthSurcharge))
        }

        // Terrain surcharge
        if (terrainSurcharge > 0) {
            breakdown.add("Terrain Type" to String.format("E%.2f", terrainSurcharge))
        }

        // Service surcharge
        if (serviceSurcharge > 0) {
            breakdown.add("Service Variant" to String.format("E%.2f", serviceSurcharge))
        }

        // Disposal fee
        if (disposalFee > 0) {
            breakdown.add("Waste Removal" to String.format("E%.2f", disposalFee))
        }

        // Travel fee
        if (travelFee > 0) {
            breakdown.add("Travel Distance" to String.format("E%.2f", travelFee))
        }

        // Urgency fee
        if (urgencyFee > 0) {
            breakdown.add("Urgent Job" to String.format("E%.2f", urgencyFee))
        }

        // Subtotal
        breakdown.add("Subtotal" to String.format("E%.2f", subtotal))

        // Mobile money fee (2%)
        if (mobileMoneyFee > 0) {
            breakdown.add("Mobile Money Fee (2%)" to String.format("E%.2f", mobileMoneyFee))
        }

        // VAT (15%)
        if (vat > 0) {
            breakdown.add("VAT (15%)" to String.format("E%.2f", vat))
        }

        // Total
        breakdown.add("TOTAL AMOUNT" to String.format("E%.2f", totalAmount))

        return breakdown
    }
}