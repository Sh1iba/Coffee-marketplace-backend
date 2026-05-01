package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.ProductCategory

interface ProductCategoryRepository : JpaRepository<ProductCategory, Int>
