package com.funglejunk.stockecho.model

sealed class CalculationError {
    object EmptyPrefsData : CalculationError()
    object IsinNotFoundInRemoteData : CalculationError()
    object ZeroShares : CalculationError()
    object TooManyCloseValues : CalculationError()
    object NoRemoteCloseValue : CalculationError()
    object ZeroRemoteCloseValue : CalculationError()
}