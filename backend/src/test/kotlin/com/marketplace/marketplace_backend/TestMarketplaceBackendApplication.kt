package com.marketplace.marketplace_backend

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<MarketplaceBackendApplication>().with(TestcontainersConfiguration::class).run(*args)
}
