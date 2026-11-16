package com.replit.pinduoduo.localizer.demo

data class DemoProduct(
    val id: String,
    val nameKey: String,
    val originalName: String,
    val descriptionKey: String,
    val originalDescription: String,
    val categoryKey: String,
    val originalCategory: String,
    val priceYuan: Int,
    val icon: String,
)

object DemoProducts {
    val all = listOf(
        DemoProduct(
            id = "shirt",
            nameKey = DemoString.PRODUCT_SHIRT,
            originalName = "棉麻衬衫",
            descriptionKey = DemoString.PRODUCT_SHIRT_DESCRIPTION,
            originalDescription = "演示衬衫。尺码和颜色仅用于展示。",
            categoryKey = DemoString.CATEGORY_CLOTHING,
            originalCategory = "服装",
            priceYuan = 89,
            icon = "👕",
        ),
        DemoProduct(
            id = "shoes",
            nameKey = DemoString.PRODUCT_SHOES,
            originalName = "轻便运动鞋",
            descriptionKey = DemoString.PRODUCT_SHOES_DESCRIPTION,
            originalDescription = "用于展示商品详情的演示鞋款。",
            categoryKey = DemoString.CATEGORY_SHOES,
            originalCategory = "鞋类",
            priceYuan = 129,
            icon = "👟",
        ),
        DemoProduct(
            id = "mug",
            nameKey = DemoString.PRODUCT_MUG,
            originalName = "陶瓷马克杯",
            descriptionKey = DemoString.PRODUCT_MUG_DESCRIPTION,
            originalDescription = "用于演示家居用品的测试杯子。",
            categoryKey = DemoString.CATEGORY_HOME,
            originalCategory = "家居",
            priceYuan = 39,
            icon = "☕",
        ),
        DemoProduct(
            id = "bag",
            nameKey = DemoString.PRODUCT_BAG,
            originalName = "旅行双肩包",
            descriptionKey = DemoString.PRODUCT_BAG_DESCRIPTION,
            originalDescription = "带有示例属性的测试背包。",
            categoryKey = DemoString.CATEGORY_BAGS,
            originalCategory = "箱包",
            priceYuan = 159,
            icon = "🎒",
        ),
    )
}

/** In-memory cart state. It is intentionally local and has no network or checkout behavior. */
data class DemoCartState(
    val quantities: Map<String, Int> = emptyMap(),
) {
    fun add(productId: String): DemoCartState =
        copy(quantities = quantities + (productId to ((quantities[productId] ?: 0) + 1)))

    fun removeOne(productId: String): DemoCartState {
        val current = quantities[productId] ?: return this
        val updated = if (current <= 1) {
            quantities - productId
        } else {
            quantities + (productId to (current - 1))
        }
        return copy(quantities = updated)
    }

    fun itemCount(): Int = quantities.values.sum()

    fun totalYuan(products: List<DemoProduct> = DemoProducts.all): Int =
        products.sumOf { product -> product.priceYuan * (quantities[product.id] ?: 0) }
}
