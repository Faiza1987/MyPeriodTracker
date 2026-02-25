package org.tracker.exceptions

class InsufficientCycleHistoryException(
    message: String = "Not enough cycle history to make a prediction. Please record at least 2 periods."
) : IllegalStateException(message)