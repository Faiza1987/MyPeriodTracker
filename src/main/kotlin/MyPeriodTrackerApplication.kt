package org.tracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MyPeriodTrackerApplication

fun main(args: Array<String>) {
    runApplication<MyPeriodTrackerApplication>(*args)
}
