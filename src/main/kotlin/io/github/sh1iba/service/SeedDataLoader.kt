package io.github.sh1iba.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import io.github.sh1iba.entity.*
import io.github.sh1iba.repository.*
import java.math.BigDecimal

@Component
class SeedDataLoader(
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository,
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val imageStorageService: ImageStorageService,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (sellerRepository.count() > 0) return
        seedAdminUser()
        seedCategories()
        val sellers = seedSellers()
        val imgs = uploadSeedImages()
        seedProducts(sellers, imgs)
    }

    private fun seedAdminUser() {
        if (userRepository.findByEmail("admin@mail.ru") == null) {
            userRepository.save(User(
                email = "admin@mail.ru",
                name = "Admin",
                passwordHash = passwordEncoder.encode("1"),
                role = Role.ADMIN
            ))
        }
    }

    // ── Категории ─────────────────────────────────────────────────────────

    private fun seedCategories() {
        val names = listOf("Кофе", "Напитки", "Выпечка", "Сандвичи", "Десерты", "Завтраки", "Специи", "Салаты")
        val existing = productCategoryRepository.findAll().map { it.type }.toSet()
        names.filter { it !in existing }.forEach { productCategoryRepository.save(ProductCategory(type = it)) }
    }

    // ── Продавцы ──────────────────────────────────────────────────────────

    private data class SellerSeed(
        val email: String, val ownerName: String,
        val shopName: String, val description: String,
        val category: String, val rating: Double
    )

    private fun seedSellers(): Map<String, Seller> {
        val password = passwordEncoder.encode("1")
        val seeds = listOf(
            SellerSeed("urbanbrew@mail.ru",   "Артём Кофейников", "Urban Brew",      "Авторский кофе из зёрен со всего мира. Каждая чашка — история о вкусе и качестве.",              "Кофейня",           4.9),
            SellerSeed("boulangerie@mail.ru", "Мария Булочкина",  "La Boulangerie",  "Свежая французская выпечка и изысканные десерты по рецептам парижских кондитеров.",             "Пекарня",           4.8),
            SellerSeed("freshbites@mail.ru",  "Дмитрий Свежев",   "Fresh Bites",     "Сытные сэндвичи и завтраки из свежих локальных продуктов. Быстро, вкусно, полезно.",            "Кухня",             4.7),
            SellerSeed("spiceroute@mail.ru",  "Алия Пряникова",   "Spice Route",     "Восточные специи и блюда с ароматом далёких стран. Откройте мир новых вкусов.",                 "Восточная кухня",   4.6),
            SellerSeed("greenbowl@mail.ru",   "Никита Зеленцов",  "Green Bowl",      "Свежие салаты, боулы и полезная еда для тех, кто заботится о здоровье.",                        "Здоровое питание",  4.8)
        )
        return seeds.associate { s ->
            val user = userRepository.findByEmail(s.email) ?: userRepository.save(
                User(email = s.email, name = s.ownerName, passwordHash = password, role = Role.SELLER)
            )
            val seller = sellerRepository.findByUserId(user.id) ?: sellerRepository.save(
                Seller(user = user, name = s.shopName, description = s.description,
                    category = s.category, rating = s.rating)
            )
            s.shopName to seller
        }
    }

    // ── Картинки ──────────────────────────────────────────────────────────

    private fun uploadSeedImages(): Map<String, String> {
        val names = listOf(
            "espresso", "americano", "cappuccino", "latte", "mocha", "macchiato",
            "lungo", "ristretto", "flat_white", "affogato", "corretto", "doppio",
            "cold_brew", "nitro_cold_brew", "iced_latte", "iced_americano", "iced_mocha", "frappe"
        )
        return names.associateWith { name ->
            val resource = ClassPathResource("images/$name.jpg")
            if (resource.exists()) imageStorageService.saveBytes(resource.inputStream.readBytes(), "seed/$name")
            else ""
        }
    }

    // ── Товары ────────────────────────────────────────────────────────────

    private data class ProductSeed(
        val catName: String, val name: String, val description: String, val imgKey: String,
        val shopName: String, val variants: List<Triple<String, Double, String?>>
    )

    private fun seedProducts(sellers: Map<String, Seller>, imgs: Map<String, String>) {
        val cats = productCategoryRepository.findAll().associateBy { it.type }

        val products = listOf(
            // ── Urban Brew: горячий кофе (12) ────────────────────────────
            ProductSeed("Кофе", "Эспрессо",    "Классический итальянский эспрессо из отборных зёрен с насыщенным ароматом и плотной крема.",    "espresso",    "Urban Brew", listOf(Triple("S",149.0,"50 мл"),  Triple("M",189.0,"100 мл"), Triple("L",229.0,"150 мл"))),
            ProductSeed("Кофе", "Американо",   "Эспрессо с горячей водой — мягкий ароматный напиток для бодрого старта дня.",                   "americano",   "Urban Brew", listOf(Triple("S",159.0,"200 мл"), Triple("M",199.0,"300 мл"), Triple("L",239.0,"400 мл"))),
            ProductSeed("Кофе", "Капучино",    "Нежный эспрессо с пышной молочной пеной — идеальный баланс вкуса и аромата.",                   "cappuccino",  "Urban Brew", listOf(Triple("S",199.0,"200 мл"), Triple("M",249.0,"300 мл"), Triple("L",299.0,"400 мл"))),
            ProductSeed("Кофе", "Латте",       "Мягкий кофе с большим количеством взбитого молока и тонким слоем пены.",                        "latte",       "Urban Brew", listOf(Triple("S",219.0,"250 мл"), Triple("M",269.0,"350 мл"), Triple("L",319.0,"450 мл"))),
            ProductSeed("Кофе", "Мокко",       "Эспрессо с шоколадным сиропом и взбитым молоком — сладкое кофейное удовольствие.",              "mocha",       "Urban Brew", listOf(Triple("S",229.0,"250 мл"), Triple("M",279.0,"350 мл"), Triple("L",329.0,"450 мл"))),
            ProductSeed("Кофе", "Макиато",     "Эспрессо с небольшой каплей нежной молочной пены — насыщенный вкус с мягким финишем.",          "macchiato",   "Urban Brew", listOf(Triple("S",179.0,"80 мл"),  Triple("M",219.0,"120 мл"), Triple("L",259.0,"160 мл"))),
            ProductSeed("Кофе", "Лунго",       "Длинный эспрессо с большим количеством воды — мягкий и ароматный напиток.",                     "lungo",       "Urban Brew", listOf(Triple("S",169.0,"100 мл"), Triple("M",209.0,"150 мл"), Triple("L",249.0,"200 мл"))),
            ProductSeed("Кофе", "Ристретто",   "Короткий концентрированный эспрессо — самый насыщенный кофейный опыт.",                         "ristretto",   "Urban Brew", listOf(Triple("S",149.0,"25 мл"),  Triple("M",189.0,"50 мл"),  Triple("L",229.0,"75 мл"))),
            ProductSeed("Кофе", "Флэт Уайт",   "Двойной эспрессо с бархатным микропенным молоком — фаворит кофейной культуры.",                 "flat_white",  "Urban Brew", listOf(Triple("S",219.0,"150 мл"), Triple("M",269.0,"200 мл"), Triple("L",319.0,"250 мл"))),
            ProductSeed("Кофе", "Аффогато",    "Шарик ванильного мороженого, залитый горячим эспрессо — итальянский десертный кофе.",           "affogato",    "Urban Brew", listOf(Triple("S",259.0,"150 мл"), Triple("M",309.0,"200 мл"), Triple("L",359.0,"250 мл"))),
            ProductSeed("Кофе", "Корретто",    "Эспрессо с добавлением граппы или ликёра — итальянская классика для особых моментов.",          "corretto",    "Urban Brew", listOf(Triple("S",239.0,"80 мл"),  Triple("M",289.0,"120 мл"), Triple("L",339.0,"160 мл"))),
            ProductSeed("Кофе", "Доппио",      "Двойная порция эспрессо для настоящих ценителей крепкого кофе.",                               "doppio",      "Urban Brew", listOf(Triple("S",169.0,"60 мл"),  Triple("M",209.0,"80 мл"),  Triple("L",249.0,"100 мл"))),
            // ── Urban Brew: холодные напитки (6) ─────────────────────────
            ProductSeed("Напитки", "Колд Брю",        "Кофе холодного заваривания — 12 часов настаивания для мягкого вкуса без горечи.",        "cold_brew",       "Urban Brew", listOf(Triple("S",249.0,"250 мл"), Triple("M",299.0,"350 мл"), Triple("L",349.0,"450 мл"))),
            ProductSeed("Напитки", "Нитро Колд Брю",  "Колд брю под давлением азота — кремовая текстура и мягкий вкус без горечи.",             "nitro_cold_brew", "Urban Brew", listOf(Triple("S",279.0,"250 мл"), Triple("M",329.0,"350 мл"), Triple("L",379.0,"450 мл"))),
            ProductSeed("Напитки", "Айс Латте",       "Охлаждённый эспрессо с холодным молоком и льдом. Идеален для жаркого дня.",              "iced_latte",      "Urban Brew", listOf(Triple("S",239.0,"300 мл"), Triple("M",289.0,"400 мл"), Triple("L",339.0,"500 мл"))),
            ProductSeed("Напитки", "Айс Американо",   "Охлаждённый американо со льдом — освежающий кофейный напиток в любую погоду.",           "iced_americano",  "Urban Brew", listOf(Triple("S",199.0,"300 мл"), Triple("M",249.0,"400 мл"), Triple("L",299.0,"500 мл"))),
            ProductSeed("Напитки", "Айс Мокко",       "Холодный шоколадный кофейный напиток со льдом — сладкое летнее удовольствие.",           "iced_mocha",      "Urban Brew", listOf(Triple("S",249.0,"300 мл"), Triple("M",299.0,"400 мл"), Triple("L",349.0,"500 мл"))),
            ProductSeed("Напитки", "Фраппе",          "Взбитый холодный кофейный напиток с молоком — освежающий и бодрящий.",                   "frappe",          "Urban Brew", listOf(Triple("S",249.0,"300 мл"), Triple("M",299.0,"400 мл"), Triple("L",349.0,"500 мл"))),
            // ── La Boulangerie: выпечка (4) и десерты (2) ────────────────
            ProductSeed("Выпечка", "Круасан классик",    "Слоёный маслянистый круасан из французского теста с хрустящей золотистой корочкой.",  "", "La Boulangerie", listOf(Triple("Штука",189.0,null), Triple("Двойная",329.0,null))),
            ProductSeed("Выпечка", "Миндальный круасан", "Круасан с нежной миндальной начинкой и посыпкой из лепестков миндаля.",              "", "La Boulangerie", listOf(Triple("Штука",229.0,null), Triple("Двойная",399.0,null))),
            ProductSeed("Выпечка", "Синабон",            "Мягкая булочка с корицей, политая сливочной глазурью. Классика американских кофеен.", "", "La Boulangerie", listOf(Triple("Штука",249.0,null), Triple("Двойная",439.0,null))),
            ProductSeed("Выпечка", "Маффин с черникой",  "Пышный маффин с сочными ягодами черники и хрустящей сахарной корочкой.",             "", "La Boulangerie", listOf(Triple("Штука",179.0,null), Triple("Двойная",299.0,null))),
            ProductSeed("Десерты", "Эклер ванильный",    "Классический французский эклер с нежным ванильным кремом и шоколадной глазурью.",    "", "La Boulangerie", listOf(Triple("Штука",219.0,null), Triple("Двойная",389.0,null))),
            ProductSeed("Десерты", "Тарт с лимоном",     "Хрустящая песочная корзиночка с кремом лимон-курд и нежной итальянской меренгой.",  "", "La Boulangerie", listOf(Triple("Штука",259.0,null), Triple("Двойная",459.0,null))),
            // ── Fresh Bites: сэндвичи (3) и завтраки (2) ─────────────────
            ProductSeed("Сандвичи", "Клаб Сэндвич",      "Многослойный сэндвич с куриным филе, беконом, томатами, листьями салата и фирменным соусом.", "", "Fresh Bites", listOf(Triple("Порция",399.0,null), Triple("Двойная",699.0,null))),
            ProductSeed("Сандвичи", "Сэндвич с лососем", "Багет с нежным слабосолёным лососем, сливочным сыром, каперсами и свежим огурцом.",          "", "Fresh Bites", listOf(Triple("Порция",449.0,null), Triple("Двойная",799.0,null))),
            ProductSeed("Сандвичи", "Авокадо Тост",      "Хрустящий тост с кремом из авокадо, яйцом пашот, чили и микрозеленью.",                     "", "Fresh Bites", listOf(Triple("Порция",349.0,null), Triple("Двойная",599.0,null))),
            ProductSeed("Завтраки", "Эгг Бенедикт",      "Английский маффин с ветчиной, яйцом пашот и нежным соусом голландез.",                      "", "Fresh Bites", listOf(Triple("Порция",479.0,null), Triple("Двойная",859.0,null))),
            ProductSeed("Завтраки", "Панкейки",           "Воздушные американские панкейки с кленовым сиропом и свежими сезонными ягодами.",            "", "Fresh Bites", listOf(Triple("Порция",329.0,null), Triple("Двойная",579.0,null))),
            // ── Spice Route: специи и восточная кухня ────────────────────
            ProductSeed("Специи",   "Масала Чай",      "Традиционный индийский чай с молоком, кардамоном, корицей и имбирём.",                         "", "Spice Route", listOf(Triple("S",199.0,"250 мл"), Triple("M",249.0,"350 мл"), Triple("L",299.0,"450 мл"))),
            ProductSeed("Специи",   "Куркума Латте",   "Согревающий напиток на основе куркумы с молоком и чёрным перцем. Золотое молоко.",              "", "Spice Route", listOf(Triple("S",219.0,"250 мл"), Triple("M",269.0,"350 мл"), Triple("L",319.0,"450 мл"))),
            ProductSeed("Сандвичи", "Шаурма классик",  "Тонкий лаваш с сочной курицей, свежими овощами и фирменным соусом.",                           "", "Spice Route", listOf(Triple("Порция",349.0,null), Triple("Двойная",619.0,null))),
            ProductSeed("Сандвичи", "Пита с фалафелем","Свежая пита с хрустящим фалафелем, хумусом, помидорами и зеленью.",                            "", "Spice Route", listOf(Triple("Порция",299.0,null), Triple("Двойная",549.0,null))),
            ProductSeed("Завтраки", "Яйца с лавашом",  "Яйца по-восточному с лавашом, зеленью и острым томатным соусом.",                              "", "Spice Route", listOf(Triple("Порция",329.0,null), Triple("Двойная",589.0,null))),
            ProductSeed("Десерты",  "Пахлава",         "Слоёное тесто с мёдом и грецкими орехами — восточная сладость с насыщенным вкусом.",            "", "Spice Route", listOf(Triple("Штука",179.0,null), Triple("Двойная",319.0,null))),
            // ── Green Bowl: салаты и здоровое питание ────────────────────
            ProductSeed("Салаты",  "Боул с киноа",     "Питательный боул с киноа, авокадо, черри, нутом и лимонной заправкой.",                        "", "Green Bowl", listOf(Triple("Порция",449.0,null), Triple("Двойная",799.0,null))),
            ProductSeed("Салаты",  "Греческий салат",  "Свежие овощи, оливки, фета и орегано с оливковым маслом.",                                      "", "Green Bowl", listOf(Triple("Порция",349.0,null), Triple("Двойная",619.0,null))),
            ProductSeed("Салаты",  "Авокадо Боул",     "Боул с авокадо, яйцом пашот, шпинатом и кунжутной заправкой.",                                 "", "Green Bowl", listOf(Triple("Порция",399.0,null), Triple("Двойная",699.0,null))),
            ProductSeed("Напитки", "Зелёный Смузи",    "Смузи из шпината, банана, манго и кокосового молока — заряд энергии на весь день.",             "", "Green Bowl", listOf(Triple("S",249.0,"300 мл"), Triple("M",299.0,"400 мл"), Triple("L",349.0,"500 мл"))),
            ProductSeed("Напитки", "Детокс Сок",       "Свежевыжатый сок из яблока, имбиря, лимона и сельдерея — очищение и бодрость.",                "", "Green Bowl", listOf(Triple("S",229.0,"250 мл"), Triple("M",279.0,"350 мл"), Triple("L",329.0,"450 мл"))),
            ProductSeed("Завтраки", "Овсянка с ягодами","Томлёная овсянка с черникой, малиной, мёдом и семенами чиа.",                                 "", "Green Bowl", listOf(Triple("Порция",299.0,null), Triple("Двойная",529.0,null)))
        )

        for (p in products) {
            val cat = cats[p.catName] ?: continue
            val seller = sellers[p.shopName] ?: continue
            val imageUrl = if (p.imgKey.isNotEmpty()) imgs[p.imgKey] ?: "" else ""
            val product = productRepository.save(
                Product(category = cat, seller = seller, name = p.name,
                    description = p.description, imageUrl = imageUrl)
            )
            productVariantRepository.saveAll(
                p.variants.map { (size, price, volume) ->
                    ProductVariant(product = product, size = size,
                        price = BigDecimal.valueOf(price), volume = volume)
                }
            )
        }
    }
}
