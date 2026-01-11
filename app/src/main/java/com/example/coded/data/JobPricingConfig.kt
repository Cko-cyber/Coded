package com.example.coded.data

class JobPricingConfig {
    // Platform fee percentage (handled in background, not shown to user)
    val platformFeePercentage: Double = 0.15 // 15% platform fee

    // Mobile money fee (2%)
    val mobileMoneyFeePercentage: Double = 0.02

    // VAT (15%)
    val vatPercentage: Double = 0.15

    // Travel fee per km (beyond 5km)
    val travelFeePerKm: Double = 15.0

    // Minimum job value
    val minimumJobValue: Double = 100.0

    // ==================== OUTDOOR & YARD SERVICES ====================

    // Grass Cutting (per m²)
    val grassCuttingRateSmall: Double = 1.0 // ≤150 m²
    val grassCuttingRateMedium: Double = 0.85 // 150–300 m²
    val grassCuttingRateLarge: Double = 0.73 // 300–600 m²
    val grassCuttingRateCommercial: Double = 3.25 // per m² for estate/commercial

    // Grass cutting modifiers
    val overgrownGrassSurcharge: Double = 0.30 // +30%
    val slopedTerrainSurcharge: Double = 0.20 // +20%
    val recurringDiscount: Double = 0.15 // -15% for recurring

    // Yard Clearing
    val yardClearingLight: Double = 350.0
    val yardClearingMedium: Double = 650.0
    val yardClearingHeavyPerM2: Double = 4.0 // per m²

    // Yard clearing add-ons
    val wasteRemovalFee: Double = 185.0
    val sameDayServiceFee: Double = 100.0

    // Tree Felling
    val treeFellingSmall: Double = 2000.0
    val treeFellingMedium: Double = 3750.0
    val treeFellingLarge: Double = 6250.0

    // Tree felling add-ons
    val stumpRemovalFee: Double = 1150.0
    val highRiskSurcharge: Double = 0.30 // +30%

    // ==================== CLEANING SERVICES ====================

    // Residential Cleaning
    val cleaningBasic: Double = 275.0
    val cleaningDeepSmall: Double = 600.0
    val cleaningFullHouse: Double = 975.0

    // Commercial Cleaning (per m²)
    val cleaningCommercialSmall: Double = 25.0 // per m²

    // Cleaning add-ons
    val postConstructionSurcharge: Double = 0.40 // +40%

    // ==================== TECHNICAL SERVICES ====================

    // DStv Installation
    val dstvBasic: Double = 625.0
    val dstvStandard: Double = 1400.0
    val dstvExtraView: Double = 2500.0

    // DStv add-ons
    val cableExtensionPerMeter: Double = 20.0
    val decoderRelocation: Double = 300.0

    // TV Mounting
    val tvMountingStandard: Double = 450.0
    val tvMountingConcrete: Double = 600.0

    // ==================== ERRANDS & SMALL TASKS ====================

    val errandSingle: Double = 60.0
    val errandMultiple: Double = 115.0
    val deliveryLocal: Double = 85.0

    // Waiting time (per hour beyond 15 min)
    val waitingTimePerHour: Double = 30.0

    // ==================== PLUMBING SERVICES ====================

    val plumbingMinorFix: Double = 250.0
    val plumbingToiletRepair: Double = 525.0
    val plumbingPipeReplace: Double = 900.0

    // Emergency call-out fee
    val emergencyCalloutFee: Double = 200.0

    // ==================== ELECTRICAL SERVICES ====================

    val electricalSocketReplace: Double = 325.0
    val electricalLightInstall: Double = 450.0
    val electricalFaultFinding: Double = 600.0

    // ==================== OTHER SERVICES ====================

    // Furniture Assembly
    val furnitureAssembly: Double = 200.0

    // Painting
    val paintingPerM2: Double = 30.0

    // Moving Help (per hour)
    val movingHelpPerHour: Double = 80.0

    // Maintenance
    val maintenanceBasic: Double = 300.0

    // ==================== SERVICE TYPE DEFINITIONS ====================

    fun getServiceTypeDisplayName(serviceType: String): String {
        return when (serviceType) {
            "grass_cutting" -> "Grass Cutting"
            "yard_clearing" -> "Yard Clearing"
            "gardening" -> "Gardening"
            "tree_felling" -> "Tree Felling"
            "cleaning" -> "Cleaning"
            "plumbing" -> "Plumbing"
            "electrical" -> "Electrical"
            "dstv_installation" -> "DSTV Installation"
            "maintenance" -> "Maintenance"
            "errands" -> "Errands"
            "furniture_assembly" -> "Furniture Assembly"
            "moving_help" -> "Moving Help"
            "painting" -> "Painting"
            else -> "Service"
        }
    }

    // ==================== ESTIMATED HOURS ====================

    fun getEstimatedHours(serviceType: String, area: Double? = null): Double {
        return when (serviceType) {
            "grass_cutting" -> {
                when {
                    area == null -> 2.0
                    area <= 150 -> 2.0
                    area <= 300 -> 3.0
                    area <= 600 -> 4.0
                    else -> 6.0
                }
            }
            "yard_clearing" -> {
                when {
                    area == null -> 4.0
                    area <= 100 -> 3.0
                    area <= 300 -> 5.0
                    else -> 8.0
                }
            }
            "tree_felling" -> 6.0
            "cleaning" -> 3.0
            "plumbing" -> 2.0
            "electrical" -> 2.0
            "dstv_installation" -> 3.0
            "maintenance" -> 2.0
            "errands" -> 1.0
            "furniture_assembly" -> 2.0
            "moving_help" -> 4.0
            "painting" -> area?.let { it / 20 } ?: 4.0
            else -> 2.0
        }
    }
}