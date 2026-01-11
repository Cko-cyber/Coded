package com.example.coded.data

object JobPricingCalculator {

    /**
     * Calculate price for grass cutting, yard clearing, and gardening
     */
    fun calculateAreaBasedPrice(
        areaSqM: Double,
        serviceType: String,
        vegetationType: String = "medium",
        growthStage: String = "medium",
        terrainType: String = "flat",
        needsDisposal: Boolean = false,
        travelDistanceKm: Double = 0.0,
        isUrgent: Boolean = false,
        isRecurring: Boolean = false,
        config: JobPricingConfig = JobPricingConfig()
    ): JobPriceBreakdown {
        var basePrice = 0.0
        var estimatedHours = config.getEstimatedHours(serviceType, areaSqM)

        when (serviceType) {
            "grass_cutting" -> {
                // Determine base rate based on area
                basePrice = when {
                    areaSqM <= 150 -> areaSqM * config.grassCuttingRateSmall
                    areaSqM <= 300 -> areaSqM * config.grassCuttingRateMedium
                    areaSqM <= 600 -> areaSqM * config.grassCuttingRateLarge
                    else -> areaSqM * config.grassCuttingRateCommercial
                }

                // Apply recurring discount
                if (isRecurring) {
                    basePrice *= (1 - config.recurringDiscount)
                }
            }

            "yard_clearing", "gardening" -> {
                basePrice = when (vegetationType) {
                    "light" -> config.yardClearingLight
                    "medium" -> config.yardClearingMedium
                    "heavy", "overgrown" -> areaSqM * config.yardClearingHeavyPerM2
                    else -> areaSqM * config.yardClearingHeavyPerM2
                }

                // Add waste removal for yard clearing
                if (serviceType == "yard_clearing" && needsDisposal) {
                    basePrice += config.wasteRemovalFee
                }
            }
        }

        // Calculate surcharges
        val vegetationSurcharge = when (vegetationType) {
            "overgrown" -> basePrice * config.overgrownGrassSurcharge
            else -> 0.0
        }

        val growthSurcharge = when (growthStage) {
            "mature" -> basePrice * 0.15 // +15% for mature growth
            else -> 0.0
        }

        val terrainSurcharge = when (terrainType) {
            "sloped" -> basePrice * config.slopedTerrainSurcharge
            "uneven" -> basePrice * 0.15 // +15% for uneven
            else -> 0.0
        }

        val travelFee = if (travelDistanceKm > 5) {
            (travelDistanceKm - 5) * config.travelFeePerKm
        } else {
            0.0
        }

        val urgencyFee = if (isUrgent) config.sameDayServiceFee else 0.0

        // Calculate subtotal (with platform fee included but not shown)
        val subtotalWithoutPlatform = basePrice + vegetationSurcharge + growthSurcharge +
                terrainSurcharge + travelFee + urgencyFee

        // Apply minimum job value
        val adjustedSubtotal = subtotalWithoutPlatform.coerceAtLeast(config.minimumJobValue)

        // Add platform fee (15%) to subtotal but don't show separately
        val subtotalWithPlatform = adjustedSubtotal * (1 + config.platformFeePercentage)

        // Calculate mobile money fee (2%)
        val mobileMoneyFee = subtotalWithPlatform * config.mobileMoneyFeePercentage

        // Calculate VAT (15%)
        val vat = subtotalWithPlatform * config.vatPercentage

        // Total amount
        val totalAmount = subtotalWithPlatform + mobileMoneyFee + vat

        return JobPriceBreakdown(
            basePrice = basePrice,
            vegetationSurcharge = vegetationSurcharge,
            growthSurcharge = growthSurcharge,
            terrainSurcharge = terrainSurcharge,
            disposalFee = if (needsDisposal && serviceType == "yard_clearing") config.wasteRemovalFee else 0.0,
            travelFee = travelFee,
            urgencyFee = urgencyFee,
            subtotal = subtotalWithPlatform,
            mobileMoneyFee = mobileMoneyFee,
            vat = vat,
            totalAmount = totalAmount,
            estimatedHours = estimatedHours
        )
    }

    /**
     * Calculate price for tree felling
     */
    fun calculateTreeFellingPrice(
        treeSize: String,
        treeHeight: Double = 10.0,
        locationComplexity: String = "normal",
        needsStumpRemoval: Boolean = false,
        needsCleanup: Boolean = true,
        travelDistanceKm: Double = 0.0,
        config: JobPricingConfig = JobPricingConfig()
    ): JobPriceBreakdown {
        var basePrice = when (treeSize) {
            "small_tree" -> config.treeFellingSmall
            "medium_tree" -> config.treeFellingMedium
            "large_tree" -> config.treeFellingLarge
            "palm_tree" -> config.treeFellingMedium
            "fruit_tree" -> config.treeFellingMedium
            else -> config.treeFellingMedium
        }

        // Adjust for height
        if (treeHeight > 15) {
            basePrice *= 1.2 // +20% for very tall trees
        }

        // Location complexity
        if (locationComplexity == "complex") {
            basePrice *= (1 + config.highRiskSurcharge)
        }

        // Add-ons
        val stumpRemovalFee = if (needsStumpRemoval) config.stumpRemovalFee else 0.0
        val cleanupFee = if (needsCleanup) 200.0 else 0.0

        val travelFee = if (travelDistanceKm > 5) {
            (travelDistanceKm - 5) * config.travelFeePerKm
        } else {
            0.0
        }

        // Calculate subtotal
        val subtotalWithoutPlatform = basePrice + stumpRemovalFee + cleanupFee + travelFee

        // Add platform fee (15%)
        val subtotalWithPlatform = subtotalWithoutPlatform * (1 + config.platformFeePercentage)

        // Calculate mobile money fee (2%)
        val mobileMoneyFee = subtotalWithPlatform * config.mobileMoneyFeePercentage

        // Calculate VAT (15%)
        val vat = subtotalWithPlatform * config.vatPercentage

        // Total amount
        val totalAmount = subtotalWithPlatform + mobileMoneyFee + vat

        return JobPriceBreakdown(
            basePrice = basePrice,
            disposalFee = cleanupFee,
            travelFee = travelFee,
            serviceSurcharge = if (locationComplexity == "complex") basePrice * config.highRiskSurcharge else 0.0,
            subtotal = subtotalWithPlatform,
            mobileMoneyFee = mobileMoneyFee,
            vat = vat,
            totalAmount = totalAmount,
            estimatedHours = config.getEstimatedHours("tree_felling")
        )
    }

    /**
     * Calculate price for other services (plumbing, electrical, cleaning, etc.)
     */
    fun calculateServicePrice(
        serviceType: String,
        serviceVariant: String? = null,
        areaSqM: Double? = null,
        isUrgent: Boolean = false,
        travelDistanceKm: Double = 0.0,
        config: JobPricingConfig = JobPricingConfig()
    ): JobPriceBreakdown {
        var basePrice = 0.0

        when (serviceType) {
            "cleaning" -> {
                basePrice = when (serviceVariant) {
                    "basic" -> config.cleaningBasic
                    "deep" -> config.cleaningDeepSmall
                    "full_house" -> config.cleaningFullHouse
                    "commercial" -> (areaSqM ?: 50.0) * config.cleaningCommercialSmall
                    else -> config.cleaningBasic
                }
            }

            "plumbing" -> {
                basePrice = when (serviceVariant) {
                    "leaking_tap" -> config.plumbingMinorFix
                    "blocked_drain" -> config.plumbingToiletRepair
                    "toilet_repair" -> config.plumbingToiletRepair
                    "pipe_fixing" -> config.plumbingPipeReplace
                    else -> config.plumbingMinorFix
                }
            }

            "electrical" -> {
                basePrice = when (serviceVariant) {
                    "socket_repair" -> config.electricalSocketReplace
                    "light_installation" -> config.electricalLightInstall
                    "switch_fixing" -> config.electricalSocketReplace
                    else -> config.electricalFaultFinding
                }
            }

            "dstv_installation" -> {
                basePrice = when (serviceVariant) {
                    "standard" -> config.dstvStandard
                    "extra_large" -> config.dstvExtraView
                    "dual_view" -> config.dstvExtraView
                    "multi_room" -> config.dstvExtraView * 1.5
                    else -> config.dstvBasic
                }
            }

            "maintenance" -> {
                basePrice = config.maintenanceBasic
            }

            "errands" -> {
                basePrice = if (serviceVariant == "multiple") config.errandMultiple else config.errandSingle
            }

            "furniture_assembly" -> {
                basePrice = config.furnitureAssembly
            }

            "moving_help" -> {
                basePrice = config.movingHelpPerHour * 4 // Default 4 hours
            }

            "painting" -> {
                basePrice = (areaSqM ?: 50.0) * config.paintingPerM2
            }
        }

        // Apply minimum job value
        basePrice = basePrice.coerceAtLeast(config.minimumJobValue)

        // Travel fee
        val travelFee = if (travelDistanceKm > 5) {
            (travelDistanceKm - 5) * config.travelFeePerKm
        } else {
            0.0
        }

        // Urgency fee
        val urgencyFee = if (isUrgent) config.sameDayServiceFee else 0.0

        // Emergency call-out for plumbing/electrical
        val emergencyFee = if (isUrgent && (serviceType == "plumbing" || serviceType == "electrical")) {
            config.emergencyCalloutFee
        } else {
            0.0
        }

        // Calculate subtotal
        val subtotalWithoutPlatform = basePrice + travelFee + urgencyFee + emergencyFee

        // Add platform fee (15%)
        val subtotalWithPlatform = subtotalWithoutPlatform * (1 + config.platformFeePercentage)

        // Calculate mobile money fee (2%)
        val mobileMoneyFee = subtotalWithPlatform * config.mobileMoneyFeePercentage

        // Calculate VAT (15%)
        val vat = subtotalWithPlatform * config.vatPercentage

        // Total amount
        val totalAmount = subtotalWithPlatform + mobileMoneyFee + vat

        return JobPriceBreakdown(
            basePrice = basePrice,
            travelFee = travelFee,
            urgencyFee = urgencyFee,
            serviceSurcharge = emergencyFee,
            subtotal = subtotalWithPlatform,
            mobileMoneyFee = mobileMoneyFee,
            vat = vat,
            totalAmount = totalAmount,
            estimatedHours = config.getEstimatedHours(serviceType, areaSqM)
        )
    }
}