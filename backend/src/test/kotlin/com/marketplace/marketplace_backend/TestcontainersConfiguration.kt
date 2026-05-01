package com.marketplace.marketplace_backend

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            // Wait.forListeningPort() verifies the mapped host port is reachable
            // from the JVM, not just that the container's internal port is open.
            // This is necessary on macOS with Colima (VZ) where host-side port
            // forwarding is set up slightly after the container's internal readiness.
            .waitingFor(Wait.forListeningPort())
}
