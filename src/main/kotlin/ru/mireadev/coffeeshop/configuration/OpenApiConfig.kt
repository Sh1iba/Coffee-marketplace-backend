package ru.mireadev.coffeeshop.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
@OpenAPIDefinition(
    info = io.swagger.v3.oas.annotations.info.Info(
        title = "Coffee Shop Mireadev",
        description = "API приложения для заказа кофе",
        version = "0.0.1",
        contact = Contact(name = "Иброгимов З.А.", email = "zafar_ibrogimov@mail.ru")
    )
)
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(Info().title("API Documentation").version("1.0"))
    }
}