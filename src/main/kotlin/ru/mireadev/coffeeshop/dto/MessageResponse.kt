package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Сущность ответа от сервера в виде сообщения")
data class MessageResponse(
    @field: Schema(description = "Сообщение", example = "Some message")
    val massage: String
)