package com.replit.pinduoduo.localizer.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replit.pinduoduo.localizer.android.AndroidI18nAdapter
import com.replit.pinduoduo.localizer.android.AndroidLocalization

private enum class DemoTab {
    SHOP,
    CART,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoShopScreen(localization: AndroidLocalization) {
    val i18n = remember(localization) { AndroidI18nAdapter(localization) }
    var language by remember(localization) {
        mutableStateOf(localization.currentLanguage())
    }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(DemoTab.SHOP) }
    var cart by remember { mutableStateOf(DemoCartState()) }
    var selectedProduct by remember { mutableStateOf<DemoProduct?>(null) }
    val strings: (String, String) -> String = { key, original ->
        i18n.getString(key, original)
    }

    val visibleProducts = DemoProducts.all.filter { product ->
        query.isBlank() ||
            strings(product.nameKey, product.originalName).contains(query, ignoreCase = true) ||
            product.originalName.contains(query, ignoreCase = true) ||
            strings(product.categoryKey, product.originalCategory).contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings(DemoString.APP_TITLE, "本地商店演示"))
                        Text(
                            strings(DemoString.LOCAL_ONLY, "不连接拼多多"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == DemoTab.SHOP,
                    onClick = { selectedTab = DemoTab.SHOP },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(strings("home.tab.home", "首页")) },
                )
                NavigationBarItem(
                    selected = selectedTab == DemoTab.CART,
                    onClick = { selectedTab = DemoTab.CART },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = {
                        Text(
                            "${strings(DemoString.CART_TITLE, "购物车")} " +
                                "(${cart.itemCount()})",
                        )
                    },
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
        ) {
            DemoNotice(strings)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == "zh",
                    onClick = {
                        if (localization.selectLanguage("zh")) {
                            language = localization.currentLanguage()
                        }
                    },
                    label = { Text(strings(DemoString.LANGUAGE_CHINESE, "中文")) },
                )
                FilterChip(
                    selected = language == "ru",
                    onClick = {
                        if (localization.selectLanguage("ru")) {
                            language = localization.currentLanguage()
                        }
                    },
                    label = { Text(strings(DemoString.LANGUAGE_RUSSIAN, "Русский")) },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    DemoTab.SHOP -> ShopContent(
                        strings = strings,
                        query = query,
                        onQueryChange = { query = it },
                        products = visibleProducts,
                        cart = cart,
                        onOpenProduct = { selectedProduct = it },
                        onAdd = { cart = cart.add(it.id) },
                    )
                    DemoTab.CART -> CartContent(
                        strings = strings,
                        cart = cart,
                        onAdd = { cart = cart.add(it) },
                        onRemove = { cart = cart.removeOne(it) },
                    )
                }
            }
        }
    }

    selectedProduct?.let { product ->
        ProductDetailsDialog(
            product = product,
            strings = strings,
            onDismiss = { selectedProduct = null },
            onAdd = {
                cart = cart.add(product.id)
                selectedProduct = null
            },
        )
    }
}

@Composable
private fun DemoNotice(strings: (String, String) -> String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF1C2), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            strings(DemoString.NOTICE, "演示 — 测试数据"),
            color = Color(0xFF5C4300),
            fontWeight = FontWeight.Bold,
        )
        Text(
            strings(DemoString.LOCAL_ONLY, "不连接拼多多"),
            color = Color(0xFF6D5A26),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ShopContent(
    strings: (String, String) -> String,
    query: String,
    onQueryChange: (String) -> Unit,
    products: List<DemoProduct>,
    cart: DemoCartState,
    onOpenProduct: (DemoProduct) -> Unit,
    onAdd: (DemoProduct) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(strings("search.input.goods_hint", "搜索你要的商品")) },
        )
        Text(
            text = strings("products.title", "商品"),
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (products.isEmpty()) {
            Text(
                text = strings(DemoString.SEARCH_EMPTY, "没有找到演示商品"),
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(products, key = DemoProduct::id) { product ->
                    ProductCard(
                        product = product,
                        quantity = cart.quantities[product.id] ?: 0,
                        strings = strings,
                        onOpen = { onOpenProduct(product) },
                        onAdd = { onAdd(product) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: DemoProduct,
    quantity: Int,
    strings: (String, String) -> String,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(Color(0xFFF4F0E9), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) {
                Text(product.icon, fontSize = 34.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings(product.nameKey, product.originalName),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    strings(product.categoryKey, product.originalCategory),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "¥${product.priceYuan}",
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFFC9342B),
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpen) {
                        Text(strings(DemoString.VIEW_DETAILS, "查看详情"))
                    }
                    Button(onClick = onAdd) {
                        Text(
                            if (quantity == 0) {
                                strings(DemoString.ADD_TO_CART, "加入购物车")
                            } else {
                                "${strings(DemoString.ADD_TO_CART, "加入购物车")} ($quantity)"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartContent(
    strings: (String, String) -> String,
    cart: DemoCartState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val cartProducts = DemoProducts.all.filter { (cart.quantities[it.id] ?: 0) > 0 }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            strings(DemoString.CART_TITLE, "购物车"),
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            strings(DemoString.CART_LOCAL_NOTE, "购物车仅保存在此演示应用中"),
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (cartProducts.isEmpty()) {
            Text(
                strings(DemoString.CART_EMPTY, "购物车为空"),
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cartProducts, key = DemoProduct::id) { product ->
                    CartProductRow(
                        product = product,
                        quantity = cart.quantities[product.id] ?: 0,
                        strings = strings,
                        onAdd = { onAdd(product.id) },
                        onRemove = { onRemove(product.id) },
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(strings(DemoString.CART_TOTAL, "合计"), fontWeight = FontWeight.Bold)
                    Text(
                        "¥${cart.totalYuan()}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9342B),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartProductRow(
    product: DemoProduct,
    quantity: Int,
    strings: (String, String) -> String,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(product.icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(strings(product.nameKey, product.originalName), fontWeight = FontWeight.SemiBold)
                Text("¥${product.priceYuan}", color = Color(0xFFC9342B))
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = strings(DemoString.REMOVE, "移除"),
                )
            }
            Text(quantity.toString())
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = strings(DemoString.ADD, "增加"),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailsDialog(
    product: DemoProduct,
    strings: (String, String) -> String,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings("products.details.title", "商品详情")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.icon, fontSize = 42.sp)
                Text(strings(product.nameKey, product.originalName), fontWeight = FontWeight.Bold)
                Text(strings(product.descriptionKey, product.originalDescription))
                Text(
                    "${strings("orders.goods_price_prefix", "商品价格：")} ¥${product.priceYuan}",
                    color = Color(0xFFC9342B),
                )
                Text(
                    strings(DemoString.NOTICE, "演示 — 测试数据"),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text(strings(DemoString.ADD_TO_CART, "加入购物车"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings("system.action.cancel", "取消"))
            }
        },
    )
}
