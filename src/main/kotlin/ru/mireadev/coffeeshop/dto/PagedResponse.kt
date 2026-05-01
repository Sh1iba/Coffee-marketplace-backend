package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Постраничный ответ")
data class PagedResponse<T>(
    @field:Schema(description = "Данные текущей страницы")
    val content: List<T>,

    @field:Schema(description = "Номер текущей страницы (начиная с 0)", example = "0")
    val currentPage: Int,

    @field:Schema(description = "Размер страницы", example = "10")
    val pageSize: Int,

    @field:Schema(description = "Всего элементов", example = "48")
    val totalElements: Long,

    @field:Schema(description = "Всего страниц", example = "5")
    val totalPages: Int,

    @field:Schema(description = "Последняя ли это страница", example = "false")
    val isLast: Boolean
)
