package com.sleepfuriously.mypostman.data

/**
 * The errors that may be returned from a PH device request.
 */
data class PHErrorResponse(
    val errors: PHError
)

data class PHError(
    val description: String
)