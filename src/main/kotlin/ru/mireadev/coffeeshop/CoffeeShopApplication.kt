package ru.mireadev.coffeeshop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoffeeShopApplication

fun main(args: Array<String>) {
    runApplication<CoffeeShopApplication>(*args)
}
