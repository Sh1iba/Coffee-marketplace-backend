package io.github.sh1iba.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import io.github.sh1iba.entity.*
import io.github.sh1iba.entity.SellerStatus
import io.github.sh1iba.repository.*
import java.math.BigDecimal

@Component
class SeedDataLoader(
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository,
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val branchRepository: BranchRepository,
    private val imageStorageService: ImageStorageService,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (sellerRepository.count() > 0) {
            ensureBranchCredentials()
            return
        }
        seedAdminUser()
        seedCategories()
        val sellers = seedSellers()
        seedDefaultBranches(sellers)
        val imgs = uploadSeedImages()
        seedProducts(sellers, imgs)
    }

    private fun ensureBranchCredentials() {
        val emailMap = mapOf(
            "Urban Brew"     to "urbanbrew.branch@mail.ru",
            "La Boulangerie" to "boulangerie.branch@mail.ru",
            "Fresh Bites"    to "freshbites.branch@mail.ru",
            "Spice Route"    to "spiceroute.branch@mail.ru",
            "Green Bowl"     to "greenbowl.branch@mail.ru"
        )
        val branchPassword = passwordEncoder.encode("branch1")
        branchRepository.findAllWithSeller().forEach { branch ->
            if (branch.email == null) {
                val email = emailMap.entries
                    .find { (k, _) -> branch.seller.name.contains(k, ignoreCase = true) }?.value
                if (email != null && branchRepository.findByEmail(email) == null) {
                    branch.email = email
                    branch.passwordHash = branchPassword
                    branchRepository.save(branch)
                }
            }
        }
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
        val names = listOf("Кофе", "Напитки", "Чай", "Выпечка", "Сандвичи", "Десерты", "Завтраки", "Специи", "Салаты", "Супы", "Закуски", "Пицца", "Паста", "Боулы")
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
                    category = s.category, rating = s.rating, status = SellerStatus.APPROVED)
            )
            s.shopName to seller
        }
    }

    // ── Филиалы ───────────────────────────────────────────────────────────

    private data class BranchSeed(
        val name: String, val address: String, val city: String,
        val email: String
    )

    private fun seedDefaultBranches(sellers: Map<String, Seller>) {
        val branchPassword = passwordEncoder.encode("branch1")
        val cityData = mapOf(
            "Urban Brew"      to BranchSeed("Urban Brew — Тверская",    "ул. Тверская, 15",       "Москва", "urbanbrew.branch@mail.ru"),
            "La Boulangerie"  to BranchSeed("La Boulangerie — Арбат",   "ул. Арбат, 28",           "Москва", "boulangerie.branch@mail.ru"),
            "Fresh Bites"     to BranchSeed("Fresh Bites — Кутузовский","Кутузовский пр-т, 7",    "Москва", "freshbites.branch@mail.ru"),
            "Spice Route"     to BranchSeed("Spice Route — Пятницкая",  "ул. Пятницкая, 22",       "Москва", "spiceroute.branch@mail.ru"),
            "Green Bowl"      to BranchSeed("Green Bowl — Покровка",    "ул. Покровка, 3",         "Москва", "greenbowl.branch@mail.ru")
        )
        for ((shopName, seller) in sellers) {
            if (!branchRepository.existsBySellerId(seller.id)) {
                val seed = cityData[shopName] ?: continue
                branchRepository.save(
                    Branch(
                        seller = seller,
                        name = seed.name,
                        address = seed.address,
                        city = seed.city,
                        deliveryFee = BigDecimal("199.00"),
                        minOrderAmount = BigDecimal("500.00"),
                        workingHours = "09:00–22:00",
                        email = seed.email,
                        passwordHash = branchPassword
                    )
                )
            }
        }
    }

    // ── Картинки ──────────────────────────────────────────────────────────

    private fun uploadSeedImages(): Map<String, String> = mapOf(
        "espresso"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116252/coffee-marketplace/seed/espresso.jpg",
        "americano"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116254/coffee-marketplace/seed/americano.jpg",
        "cappuccino"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116255/coffee-marketplace/seed/cappuccino.jpg",
        "latte"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116257/coffee-marketplace/seed/latte.jpg",
        "mocha"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116258/coffee-marketplace/seed/mocha.jpg",
        "macchiato"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116259/coffee-marketplace/seed/macchiato.jpg",
        "lungo"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116260/coffee-marketplace/seed/lungo.jpg",
        "ristretto"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116262/coffee-marketplace/seed/ristretto.jpg",
        "flat_white"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116263/coffee-marketplace/seed/flat_white.jpg",
        "affogato"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116264/coffee-marketplace/seed/affogato.jpg",
        "corretto"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116265/coffee-marketplace/seed/corretto.jpg",
        "doppio"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116267/coffee-marketplace/seed/doppio.jpg",
        "cold_brew"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116268/coffee-marketplace/seed/cold_brew.jpg",
        "nitro_cold_brew" to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116269/coffee-marketplace/seed/nitro_cold_brew.jpg",
        "iced_latte"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116270/coffee-marketplace/seed/iced_latte.webp",
        "iced_americano" to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116271/coffee-marketplace/seed/iced_americano.jpg",
        "iced_mocha"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116273/coffee-marketplace/seed/iced_mocha.png",
        "frappe"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778116274/coffee-marketplace/seed/frappe.webp",
        "boulangerie/Круасан_классик"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197584/coffee-marketplace/seed/boulangerie/%D0%9A%D1%80%D1%83%D0%B0%D1%81%D0%B0%D0%BD_%D0%BA%D0%BB%D0%B0%D1%81%D1%81%D0%B8%D0%BA.jpg",
        "boulangerie/Миндальный_круасан"   to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197590/coffee-marketplace/seed/boulangerie/%D0%9C%D0%B8%D0%BD%D0%B4%D0%B0%D0%BB%D1%8C%D0%BD%D1%8B%D0%B9_%D0%BA%D1%80%D1%83%D0%B0%D1%81%D0%B0%D0%BD.jpg",
        "boulangerie/Шоколадный_круасан"   to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197601/coffee-marketplace/seed/boulangerie/%D0%A8%D0%BE%D0%BA%D0%BE%D0%BB%D0%B0%D0%B4%D0%BD%D1%8B%D0%B9_%D0%BA%D1%80%D1%83%D0%B0%D1%81%D0%B0%D0%BD.jpg",
        "boulangerie/Синабон"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197594/coffee-marketplace/seed/boulangerie/%D0%A1%D0%B8%D0%BD%D0%B0%D0%B1%D0%BE%D0%BD.jpg",
        "boulangerie/Маффин_с_черникой"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197587/coffee-marketplace/seed/boulangerie/%D0%9C%D0%B0%D1%84%D1%84%D0%B8%D0%BD_%D1%81_%D1%87%D0%B5%D1%80%D0%BD%D0%B8%D0%BA%D0%BE%D0%B9.jpg",
        "boulangerie/Французский_багет"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197597/coffee-marketplace/seed/boulangerie/%D0%A4%D1%80%D0%B0%D0%BD%D1%86%D1%83%D0%B7%D1%81%D0%BA%D0%B8%D0%B9_%D0%B1%D0%B0%D0%B3%D0%B5%D1%82.jpg",
        "boulangerie/Пирог_с_яблоком"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197592/coffee-marketplace/seed/boulangerie/%D0%9F%D0%B8%D1%80%D0%BE%D0%B3_%D1%81_%D1%8F%D0%B1%D0%BB%D0%BE%D0%BA%D0%BE%D0%BC.jpg",
        "boulangerie/Эклер_ванильный"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197603/coffee-marketplace/seed/boulangerie/%D0%AD%D0%BA%D0%BB%D0%B5%D1%80_%D0%B2%D0%B0%D0%BD%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D0%B9.jpg",
        "boulangerie/Тарт_с_лимоном"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197596/coffee-marketplace/seed/boulangerie/%D0%A2%D0%B0%D1%80%D1%82_%D1%81_%D0%BB%D0%B8%D0%BC%D0%BE%D0%BD%D0%BE%D0%BC.jpg",
        "boulangerie/Чизкейк_Нью-Йорк"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197599/coffee-marketplace/seed/boulangerie/%D0%A7%D0%B8%D0%B7%D0%BA%D0%B5%D0%B9%D0%BA_%D0%9D%D1%8C%D1%8E-%D0%99%D0%BE%D1%80%D0%BA.jpg",
        "boulangerie/Макарон_ассорти"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197585/coffee-marketplace/seed/boulangerie/%D0%9C%D0%B0%D0%BA%D0%B0%D1%80%D0%BE%D0%BD_%D0%B0%D1%81%D1%81%D0%BE%D1%80%D1%82%D0%B8.jpg",
        "boulangerie/Мильфей_ванильный"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197589/coffee-marketplace/seed/boulangerie/%D0%9C%D0%B8%D0%BB%D1%8C%D1%84%D0%B5%D0%B9_%D0%B2%D0%B0%D0%BD%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D0%B9.jpg",
        "boulangerie/Горячий_шоколад"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778197580/coffee-marketplace/seed/boulangerie/%D0%93%D0%BE%D1%80%D1%8F%D1%87%D0%B8%D0%B9_%D1%88%D0%BE%D0%BA%D0%BE%D0%BB%D0%B0%D0%B4.jpg",
        "freshbites/Клаб_Сэндвич"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198431/coffee-marketplace/seed/freshbites/%D0%9A%D0%BB%D0%B0%D0%B1_%D0%A1%D1%8D%D0%BD%D0%B4%D0%B2%D0%B8%D1%87.jpg",
        "freshbites/Сэндвич_с_лососем"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198445/coffee-marketplace/seed/freshbites/%D0%A1%D1%8D%D0%BD%D0%B4%D0%B2%D0%B8%D1%87_%D1%81_%D0%BB%D0%BE%D1%81%D0%BE%D1%81%D0%B5%D0%BC.jpg",
        "freshbites/Авокадо_Тост"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198421/coffee-marketplace/seed/freshbites/%D0%90%D0%B2%D0%BE%D0%BA%D0%B0%D0%B4%D0%BE_%D0%A2%D0%BE%D1%81%D1%82.jpg",
        "freshbites/Ролл_с_курицей"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198441/coffee-marketplace/seed/freshbites/%D0%A0%D0%BE%D0%BB%D0%BB_%D1%81_%D0%BA%D1%83%D1%80%D0%B8%D1%86%D0%B5%D0%B9.jpg",
        "freshbites/Бургер_с_говядиной"   to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198427/coffee-marketplace/seed/freshbites/%D0%91%D1%83%D1%80%D0%B3%D0%B5%D1%80_%D1%81_%D0%B3%D0%BE%D0%B2%D1%8F%D0%B4%D0%B8%D0%BD%D0%BE%D0%B9.jpg",
        "freshbites/Эгг_Бенедикт"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198449/coffee-marketplace/seed/freshbites/%D0%AD%D0%B3%D0%B3_%D0%91%D0%B5%D0%BD%D0%B5%D0%B4%D0%B8%D0%BA%D1%82.jpg",
        "freshbites/Панкейки"             to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198435/coffee-marketplace/seed/freshbites/%D0%9F%D0%B0%D0%BD%D0%BA%D0%B5%D0%B9%D0%BA%D0%B8.jpg",
        "freshbites/Французский_тост"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198447/coffee-marketplace/seed/freshbites/%D0%A4%D1%80%D0%B0%D0%BD%D1%86%D1%83%D0%B7%D1%81%D0%BA%D0%B8%D0%B9_%D1%82%D0%BE%D1%81%D1%82.jpg",
        "freshbites/Гранола_с_йогуртом"   to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198429/coffee-marketplace/seed/freshbites/%D0%93%D1%80%D0%B0%D0%BD%D0%BE%D0%BB%D0%B0_%D1%81_%D0%B9%D0%BE%D0%B3%D1%83%D1%80%D1%82%D0%BE%D0%BC.jpg",
        "freshbites/Омлет_с_овощами"      to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198432/coffee-marketplace/seed/freshbites/%D0%9E%D0%BC%D0%BB%D0%B5%D1%82_%D1%81_%D0%BE%D0%B2%D0%BE%D1%89%D0%B0%D0%BC%D0%B8.jpg",
        "freshbites/Апельсиновый_фреш"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198423/coffee-marketplace/seed/freshbites/%D0%90%D0%BF%D0%B5%D0%BB%D1%8C%D1%81%D0%B8%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9_%D1%84%D1%80%D0%B5%D1%88.jpg",
        "freshbites/Смузи_с_клубникой"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198443/coffee-marketplace/seed/freshbites/%D0%A1%D0%BC%D1%83%D0%B7%D0%B8_%D1%81_%D0%BA%D0%BB%D1%83%D0%B1%D0%BD%D0%B8%D0%BA%D0%BE%D0%B9.jpg",
        "freshbites/Брауни"               to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778198425/coffee-marketplace/seed/freshbites/%D0%91%D1%80%D0%B0%D1%83%D0%BD%D0%B8.jpg",
        "spiceroute/Масала_Чай"           to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199323/coffee-marketplace/seed/spiceroute/%D0%9C%D0%B0%D1%81%D0%B0%D0%BB%D0%B0_%D0%A7%D0%B0%D0%B9.jpg",
        "spiceroute/Куркума_Латте"        to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199314/coffee-marketplace/seed/spiceroute/%D0%9A%D1%83%D1%80%D0%BA%D1%83%D0%BC%D0%B0_%D0%9B%D0%B0%D1%82%D1%82%D0%B5.jpg",
        "spiceroute/Матча_Латте"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199324/coffee-marketplace/seed/spiceroute/%D0%9C%D0%B0%D1%82%D1%87%D0%B0_%D0%9B%D0%B0%D1%82%D1%82%D0%B5.jpg",
        "spiceroute/Чай_Карк"             to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199332/coffee-marketplace/seed/spiceroute/%D0%A7%D0%B0%D0%B9_%D0%9A%D0%B0%D1%80%D0%BA.jpg",
        "spiceroute/Шаурма_классик"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199335/coffee-marketplace/seed/spiceroute/%D0%A8%D0%B0%D1%83%D1%80%D0%BC%D0%B0_%D0%BA%D0%BB%D0%B0%D1%81%D1%81%D0%B8%D0%BA.jpg",
        "spiceroute/Пита_с_фалафелем"     to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199330/coffee-marketplace/seed/spiceroute/%D0%9F%D0%B8%D1%82%D0%B0_%D1%81_%D1%84%D0%B0%D0%BB%D0%B0%D1%84%D0%B5%D0%BB%D0%B5%D0%BC.jpg",
        "spiceroute/Кебаб_в_лаваше"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199310/coffee-marketplace/seed/spiceroute/%D0%9A%D0%B5%D0%B1%D0%B0%D0%B1_%D0%B2_%D0%BB%D0%B0%D0%B2%D0%B0%D1%88%D0%B5.jpg",
        "spiceroute/Дюрюм_с_говядиной"    to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199308/coffee-marketplace/seed/spiceroute/%D0%94%D1%8E%D1%80%D1%8E%D0%BC_%D1%81_%D0%B3%D0%BE%D0%B2%D1%8F%D0%B4%D0%B8%D0%BD%D0%BE%D0%B9.jpg",
        "spiceroute/Яйца_с_лавашом"       to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199342/coffee-marketplace/seed/spiceroute/%D0%AF%D0%B9%D1%86%D0%B0_%D1%81_%D0%BB%D0%B0%D0%B2%D0%B0%D1%88%D0%BE%D0%BC.jpg",
        "spiceroute/Шакшука"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199333/coffee-marketplace/seed/spiceroute/%D0%A8%D0%B0%D0%BA%D1%88%D1%83%D0%BA%D0%B0.jpg",
        "spiceroute/Менемен"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199326/coffee-marketplace/seed/spiceroute/%D0%9C%D0%B5%D0%BD%D0%B5%D0%BC%D0%B5%D0%BD.jpg",
        "spiceroute/Пахлава"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199328/coffee-marketplace/seed/spiceroute/%D0%9F%D0%B0%D1%85%D0%BB%D0%B0%D0%B2%D0%B0.jpg",
        "spiceroute/Лукум"                to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199321/coffee-marketplace/seed/spiceroute/%D0%9B%D1%83%D0%BA%D1%83%D0%BC.jpg",
        "spiceroute/Кунафе"               to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199312/coffee-marketplace/seed/spiceroute/%D0%9A%D1%83%D0%BD%D0%B0%D1%84%D0%B5.jpg",
        "spiceroute/Айран"                to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778199306/coffee-marketplace/seed/spiceroute/%D0%90%D0%B9%D1%80%D0%B0%D0%BD.jpg",
        "greenbowl/Боул_с_киноа"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200714/coffee-marketplace/seed/greenbowl/%D0%91%D0%BE%D1%83%D0%BB_%D1%81_%D0%BA%D0%B8%D0%BD%D0%BE%D0%B0.jpg",
        "greenbowl/Греческий_салат"           to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200720/coffee-marketplace/seed/greenbowl/%D0%93%D1%80%D0%B5%D1%87%D0%B5%D1%81%D0%BA%D0%B8%D0%B9_%D1%81%D0%B0%D0%BB%D0%B0%D1%82.jpg",
        "greenbowl/Авокадо_Боул"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200711/coffee-marketplace/seed/greenbowl/%D0%90%D0%B2%D0%BE%D0%BA%D0%B0%D0%B4%D0%BE_%D0%91%D0%BE%D1%83%D0%BB.jpg",
        "greenbowl/Будда_Боул"                to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200716/coffee-marketplace/seed/greenbowl/%D0%91%D1%83%D0%B4%D0%B4%D0%B0_%D0%91%D0%BE%D1%83%D0%BB.jpg",
        "greenbowl/Салат_Нисуаз"              to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200733/coffee-marketplace/seed/greenbowl/%D0%A1%D0%B0%D0%BB%D0%B0%D1%82_%D0%9D%D0%B8%D1%81%D1%83%D0%B0%D0%B7.jpg",
        "greenbowl/Цезарь_с_курицей"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200735/coffee-marketplace/seed/greenbowl/%D0%A6%D0%B5%D0%B7%D0%B0%D1%80%D1%8C_%D1%81_%D0%BA%D1%83%D1%80%D0%B8%D1%86%D0%B5%D0%B9.jpg",
        "greenbowl/Акай_Боул"                 to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200712/coffee-marketplace/seed/greenbowl/%D0%90%D0%BA%D0%B0%D0%B9_%D0%91%D0%BE%D1%83%D0%BB.jpg",
        "greenbowl/Овсянка_с_ягодами"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200731/coffee-marketplace/seed/greenbowl/%D0%9E%D0%B2%D1%81%D1%8F%D0%BD%D0%BA%D0%B0_%D1%81_%D1%8F%D0%B3%D0%BE%D0%B4%D0%B0%D0%BC%D0%B8.jpg",
        "greenbowl/Гранола_с_кокосом"         to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200717/coffee-marketplace/seed/greenbowl/%D0%93%D1%80%D0%B0%D0%BD%D0%BE%D0%BB%D0%B0_%D1%81_%D0%BA%D0%BE%D0%BA%D0%BE%D1%81%D0%BE%D0%BC.jpg",
        "greenbowl/Зелёный_Смузи"             to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200723/coffee-marketplace/seed/greenbowl/%D0%97%D0%B5%D0%BB%D1%91%D0%BD%D1%8B%D0%B9_%D0%A1%D0%BC%D1%83%D0%B7%D0%B8.jpg",
        "greenbowl/Детокс_Сок"                to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200721/coffee-marketplace/seed/greenbowl/%D0%94%D0%B5%D1%82%D0%BE%D0%BA%D1%81_%D0%A1%D0%BE%D0%BA.jpg",
        "greenbowl/Имбирный_лимонад"          to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200727/coffee-marketplace/seed/greenbowl/%D0%98%D0%BC%D0%B1%D0%B8%D1%80%D0%BD%D1%8B%D0%B9_%D0%BB%D0%B8%D0%BC%D0%BE%D0%BD%D0%B0%D0%B4.jpg",
        "greenbowl/Ягодный_смузи"             to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200738/coffee-marketplace/seed/greenbowl/%D0%AF%D0%B3%D0%BE%D0%B4%D0%BD%D1%8B%D0%B9_%D1%81%D0%BC%D1%83%D0%B7%D0%B8.jpg",
        "greenbowl/Матча_Смузи"               to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200729/coffee-marketplace/seed/greenbowl/%D0%9C%D0%B0%D1%82%D1%87%D0%B0_%D0%A1%D0%BC%D1%83%D0%B7%D0%B8.jpg",
        "greenbowl/Энергетический_батончик"   to "https://res.cloudinary.com/dobkbgyjc/image/upload/v1778200736/coffee-marketplace/seed/greenbowl/%D0%AD%D0%BD%D0%B5%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8%D0%B9_%D0%B1%D0%B0%D1%82%D0%BE%D0%BD%D1%87%D0%B8%D0%BA.jpg"
    )

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
            // ── La Boulangerie: выпечка (7) десерты (5) напитки (1) = 13 ─
            ProductSeed("Выпечка", "Круасан классик",      "Слоёный маслянистый круасан из французского теста с хрустящей золотистой корочкой.",        "boulangerie/Круасан_классик", "La Boulangerie", listOf(Triple("Штука",189.0,null), Triple("Двойная",329.0,null))),
            ProductSeed("Выпечка", "Миндальный круасан",   "Круасан с нежной миндальной начинкой и посыпкой из лепестков миндаля.",                     "boulangerie/Миндальный_круасан", "La Boulangerie", listOf(Triple("Штука",229.0,null), Triple("Двойная",399.0,null))),
            ProductSeed("Выпечка", "Шоколадный круасан",   "Слоёный круасан с начинкой из тёмного бельгийского шоколада.",                             "boulangerie/Шоколадный_круасан", "La Boulangerie", listOf(Triple("Штука",219.0,null), Triple("Двойная",379.0,null))),
            ProductSeed("Выпечка", "Синабон",               "Мягкая булочка с корицей, политая сливочной глазурью. Классика американских кофеен.",       "boulangerie/Синабон", "La Boulangerie", listOf(Triple("Штука",249.0,null), Triple("Двойная",439.0,null))),
            ProductSeed("Выпечка", "Маффин с черникой",     "Пышный маффин с сочными ягодами черники и хрустящей сахарной корочкой.",                   "boulangerie/Маффин_с_черникой", "La Boulangerie", listOf(Triple("Штука",179.0,null), Triple("Двойная",299.0,null))),
            ProductSeed("Выпечка", "Французский багет",     "Хрустящий багет по традиционному рецепту с золотистой корочкой и мягкой мякотью.",          "boulangerie/Французский_багет", "La Boulangerie", listOf(Triple("Штука",149.0,null), Triple("Двойная",269.0,null))),
            ProductSeed("Выпечка", "Пирог с яблоком",       "Нежный яблочный пирог с корицей на тонком слоёном тесте в лучших традициях французской кухни.", "boulangerie/Пирог_с_яблоком", "La Boulangerie", listOf(Triple("Кусок",199.0,null), Triple("Целый",849.0,null))),
            ProductSeed("Десерты", "Эклер ванильный",       "Классический французский эклер с нежным ванильным кремом и шоколадной глазурью.",           "boulangerie/Эклер_ванильный", "La Boulangerie", listOf(Triple("Штука",219.0,null), Triple("Двойная",389.0,null))),
            ProductSeed("Десерты", "Тарт с лимоном",        "Хрустящая песочная корзиночка с кремом лимон-курд и нежной итальянской меренгой.",          "boulangerie/Тарт_с_лимоном", "La Boulangerie", listOf(Triple("Штука",259.0,null), Triple("Двойная",459.0,null))),
            ProductSeed("Десерты", "Чизкейк Нью-Йорк",     "Классический нью-йоркский чизкейк на основе из печенья с нежной сливочной текстурой.",      "boulangerie/Чизкейк_Нью-Йорк", "La Boulangerie", listOf(Triple("Кусок",289.0,null), Triple("Целый",1490.0,null))),
            ProductSeed("Десерты", "Макарон ассорти",       "Французские миндальные макароны: малина, ваниль, фисташка, шоколад. Набор из 4 штук.",      "boulangerie/Макарон_ассорти", "La Boulangerie", listOf(Triple("Набор 4 шт",349.0,null), Triple("Набор 8 шт",649.0,null))),
            ProductSeed("Десерты", "Мильфей ванильный",     "Хрустящее слоёное тесто с кремом патисьер и свежей клубникой — французская классика.",      "boulangerie/Мильфей_ванильный", "La Boulangerie", listOf(Triple("Штука",279.0,null), Triple("Двойная",499.0,null))),
            ProductSeed("Напитки", "Горячий шоколад",       "Насыщенный горячий шоколад из бельгийского какао с нежной молочной пеной.",                 "boulangerie/Горячий_шоколад", "La Boulangerie", listOf(Triple("S",199.0,"200 мл"), Triple("M",249.0,"300 мл"), Triple("L",299.0,"400 мл"))),
            // ── Fresh Bites: сэндвичи (5) завтраки (5) напитки (2) десерты (1) = 13 ──
            ProductSeed("Сандвичи", "Клаб Сэндвич",         "Многослойный сэндвич с куриным филе, беконом, томатами, листьями салата и фирменным соусом.", "freshbites/Клаб_Сэндвич", "Fresh Bites", listOf(Triple("Порция",399.0,null), Triple("Двойная",699.0,null))),
            ProductSeed("Сандвичи", "Сэндвич с лососем",    "Багет с нежным слабосолёным лососем, сливочным сыром, каперсами и свежим огурцом.",         "freshbites/Сэндвич_с_лососем", "Fresh Bites", listOf(Triple("Порция",449.0,null), Triple("Двойная",799.0,null))),
            ProductSeed("Сандвичи", "Авокадо Тост",          "Хрустящий тост с кремом из авокадо, яйцом пашот, чили и микрозеленью.",                    "freshbites/Авокадо_Тост", "Fresh Bites", listOf(Triple("Порция",349.0,null), Triple("Двойная",599.0,null))),
            ProductSeed("Сандвичи", "Ролл с курицей",        "Тортилья с куриным филе гриль, свежими овощами, пармезаном и соусом цезарь.",               "freshbites/Ролл_с_курицей", "Fresh Bites", listOf(Triple("Порция",369.0,null), Triple("Двойная",649.0,null))),
            ProductSeed("Сандвичи", "Бургер с говядиной",    "Сочная котлета из говядины, карамелизированный лук, томат, сыр чеддер и фирменный соус.",   "freshbites/Бургер_с_говядиной", "Fresh Bites", listOf(Triple("Порция",479.0,null), Triple("Двойная",849.0,null))),
            ProductSeed("Завтраки", "Эгг Бенедикт",          "Английский маффин с ветчиной, яйцом пашот и нежным соусом голландез.",                     "freshbites/Эгг_Бенедикт", "Fresh Bites", listOf(Triple("Порция",479.0,null), Triple("Двойная",859.0,null))),
            ProductSeed("Завтраки", "Панкейки",               "Воздушные американские панкейки с кленовым сиропом и свежими сезонными ягодами.",           "freshbites/Панкейки", "Fresh Bites", listOf(Triple("Порция",329.0,null), Triple("Двойная",579.0,null))),
            ProductSeed("Завтраки", "Французский тост",       "Гренки из бриоши с яйцом и корицей, политые кленовым сиропом со свежими ягодами.",         "freshbites/Французский_тост", "Fresh Bites", listOf(Triple("Порция",299.0,null), Triple("Двойная",529.0,null))),
            ProductSeed("Завтраки", "Гранола с йогуртом",     "Домашняя гранола с греческим йогуртом, мёдом и сезонными фруктами.",                       "freshbites/Гранола_с_йогуртом", "Fresh Bites", listOf(Triple("Порция",279.0,null), Triple("Двойная",499.0,null))),
            ProductSeed("Завтраки", "Омлет с овощами",        "Пышный омлет с болгарским перцем, томатами черри, шпинатом и сыром фета.",                  "freshbites/Омлет_с_овощами", "Fresh Bites", listOf(Triple("Порция",319.0,null), Triple("Двойная",579.0,null))),
            ProductSeed("Напитки",  "Апельсиновый фреш",      "Свежевыжатый сок из отборных апельсинов — натуральный заряд витамина C.",                  "freshbites/Апельсиновый_фреш", "Fresh Bites", listOf(Triple("S",199.0,"250 мл"), Triple("M",249.0,"350 мл"), Triple("L",299.0,"500 мл"))),
            ProductSeed("Напитки",  "Смузи с клубникой",      "Смузи из свежей клубники, банана и натурального йогурта без сахара.",                      "freshbites/Смузи_с_клубникой", "Fresh Bites", listOf(Triple("S",229.0,"300 мл"), Triple("M",279.0,"400 мл"), Triple("L",329.0,"500 мл"))),
            ProductSeed("Десерты",  "Брауни",                  "Плотный шоколадный брауни с хрустящей корочкой и тающей начинкой внутри.",                 "freshbites/Брауни", "Fresh Bites", listOf(Triple("Штука",199.0,null), Triple("Двойная",359.0,null))),
            // ── Spice Route: 15 товаров ───────────────────────────────────
            ProductSeed("Чай",      "Масала Чай",             "Традиционный индийский чай с молоком, кардамоном, корицей и имбирём.",                     "spiceroute/Масала_Чай", "Spice Route", listOf(Triple("S",199.0,"250 мл"), Triple("M",249.0,"350 мл"), Triple("L",299.0,"450 мл"))),
            ProductSeed("Чай",      "Куркума Латте",           "Согревающий напиток на основе куркумы с молоком и чёрным перцем. Золотое молоко.",         "spiceroute/Куркума_Латте", "Spice Route", listOf(Triple("S",219.0,"250 мл"), Triple("M",269.0,"350 мл"), Triple("L",319.0,"450 мл"))),
            ProductSeed("Чай",      "Матча Латте",             "Японский зелёный чай маття с молоком и нотками ванили — энергия без кофеиновых скачков.",  "spiceroute/Матча_Латте", "Spice Route", listOf(Triple("S",229.0,"250 мл"), Triple("M",279.0,"350 мл"), Triple("L",329.0,"450 мл"))),
            ProductSeed("Чай",      "Чай Карк",                "Традиционный эмиратский чай с кардамоном, шафраном и розовой водой.",                      "spiceroute/Чай_Карк", "Spice Route", listOf(Triple("S",189.0,"250 мл"), Triple("M",239.0,"350 мл"), Triple("L",289.0,"450 мл"))),
            ProductSeed("Сандвичи", "Шаурма классик",          "Тонкий лаваш с сочной курицей, свежими овощами и фирменным соусом.",                      "spiceroute/Шаурма_классик", "Spice Route", listOf(Triple("Порция",349.0,null), Triple("Двойная",619.0,null))),
            ProductSeed("Сандвичи", "Пита с фалафелем",        "Свежая пита с хрустящим фалафелем, хумусом, помидорами и зеленью.",                       "spiceroute/Пита_с_фалафелем", "Spice Route", listOf(Triple("Порция",299.0,null), Triple("Двойная",549.0,null))),
            ProductSeed("Сандвичи", "Кебаб в лаваше",          "Сочный кебаб из баранины с луком, зеленью и соусом из йогурта с чесноком.",               "spiceroute/Кебаб_в_лаваше", "Spice Route", listOf(Triple("Порция",379.0,null), Triple("Двойная",679.0,null))),
            ProductSeed("Сандвичи", "Дюрюм с говядиной",       "Тонкий лаваш с говядиной гриль, свежими овощами, томатным соусом и специями.",            "spiceroute/Дюрюм_с_говядиной", "Spice Route", listOf(Triple("Порция",359.0,null), Triple("Двойная",639.0,null))),
            ProductSeed("Завтраки", "Яйца с лавашом",          "Яйца по-восточному с лавашом, зеленью и острым томатным соусом.",                         "spiceroute/Яйца_с_лавашом", "Spice Route", listOf(Triple("Порция",329.0,null), Triple("Двойная",589.0,null))),
            ProductSeed("Завтраки", "Шакшука",                  "Яйца, запечённые в пряном томатном соусе с болгарским перцем и восточными специями.",     "spiceroute/Шакшука", "Spice Route", listOf(Triple("Порция",349.0,null), Triple("Двойная",619.0,null))),
            ProductSeed("Завтраки", "Менемен",                  "Турецкий омлет с томатами, зелёным перцем и специями — сытный восточный завтрак.",        "spiceroute/Менемен", "Spice Route", listOf(Triple("Порция",319.0,null), Triple("Двойная",579.0,null))),
            ProductSeed("Десерты",  "Пахлава",                  "Слоёное тесто с мёдом и грецкими орехами — восточная сладость с насыщенным вкусом.",      "spiceroute/Пахлава", "Spice Route", listOf(Triple("Штука",179.0,null), Triple("Двойная",319.0,null))),
            ProductSeed("Десерты",  "Лукум",                    "Традиционная турецкая сладость с розовой водой, фисташками и сахарной пудрой.",           "spiceroute/Лукум", "Spice Route", listOf(Triple("Штука",149.0,null), Triple("Двойная",269.0,null))),
            ProductSeed("Десерты",  "Кунафе",                   "Тёплый десерт из тонкого теста с сыром и медовым сиропом — сладость с хрустящей корочкой.", "spiceroute/Кунафе", "Spice Route", listOf(Triple("Порция",299.0,null), Triple("Двойная",549.0,null))),
            ProductSeed("Напитки",  "Айран",                    "Освежающий кисломолочный напиток с щепоткой соли — идеальное сопровождение к блюдам.",    "spiceroute/Айран", "Spice Route", listOf(Triple("S",149.0,"250 мл"), Triple("M",189.0,"350 мл"), Triple("L",229.0,"500 мл"))),
            // ── Green Bowl: 15 товаров ────────────────────────────────────
            ProductSeed("Салаты",   "Боул с киноа",             "Питательный боул с киноа, авокадо, черри, нутом и лимонной заправкой.",                   "greenbowl/Боул_с_киноа", "Green Bowl", listOf(Triple("Порция",449.0,null), Triple("Двойная",799.0,null))),
            ProductSeed("Салаты",   "Греческий салат",           "Свежие овощи, оливки, фета и орегано с оливковым маслом.",                               "greenbowl/Греческий_салат", "Green Bowl", listOf(Triple("Порция",349.0,null), Triple("Двойная",619.0,null))),
            ProductSeed("Салаты",   "Авокадо Боул",              "Боул с авокадо, яйцом пашот, шпинатом и кунжутной заправкой.",                           "greenbowl/Авокадо_Боул", "Green Bowl", listOf(Triple("Порция",399.0,null), Triple("Двойная",699.0,null))),
            ProductSeed("Салаты",   "Будда Боул",                "Боул с бурым рисом, запечёнными овощами, хумусом и соусом тахини.",                       "greenbowl/Будда_Боул", "Green Bowl", listOf(Triple("Порция",429.0,null), Triple("Двойная",759.0,null))),
            ProductSeed("Салаты",   "Салат Нисуаз",              "Французский салат с тунцом, яйцом, стручковой фасолью, томатами и оливками.",            "greenbowl/Салат_Нисуаз", "Green Bowl", listOf(Triple("Порция",389.0,null), Triple("Двойная",689.0,null))),
            ProductSeed("Салаты",   "Цезарь с курицей",          "Классический цезарь с куриным филе гриль, хрустящими крутонами и соусом из пармезана.",   "greenbowl/Цезарь_с_курицей", "Green Bowl", listOf(Triple("Порция",369.0,null), Triple("Двойная",659.0,null))),
            ProductSeed("Завтраки", "Акай Боул",                 "Замороженное акай-пюре с гранолой, бананом, клубникой и кокосовой стружкой.",             "greenbowl/Акай_Боул", "Green Bowl", listOf(Triple("Порция",379.0,null), Triple("Двойная",679.0,null))),
            ProductSeed("Завтраки", "Овсянка с ягодами",         "Томлёная овсянка с черникой, малиной, мёдом и семенами чиа.",                            "greenbowl/Овсянка_с_ягодами", "Green Bowl", listOf(Triple("Порция",299.0,null), Triple("Двойная",529.0,null))),
            ProductSeed("Завтраки", "Гранола с кокосом",         "Хрустящая гранола с кокосовой стружкой, орехами, мёдом и растительным молоком.",         "greenbowl/Гранола_с_кокосом", "Green Bowl", listOf(Triple("Порция",279.0,null), Triple("Двойная",499.0,null))),
            ProductSeed("Напитки",  "Зелёный Смузи",             "Смузи из шпината, банана, манго и кокосового молока — заряд энергии на весь день.",      "greenbowl/Зелёный_Смузи", "Green Bowl", listOf(Triple("S",249.0,"300 мл"), Triple("M",299.0,"400 мл"), Triple("L",349.0,"500 мл"))),
            ProductSeed("Напитки",  "Детокс Сок",                "Свежевыжатый сок из яблока, имбиря, лимона и сельдерея — очищение и бодрость.",          "greenbowl/Детокс_Сок", "Green Bowl", listOf(Triple("S",229.0,"250 мл"), Triple("M",279.0,"350 мл"), Triple("L",329.0,"450 мл"))),
            ProductSeed("Напитки",  "Имбирный лимонад",          "Освежающий лимонад с имбирём, свежей мятой и кокосовым сахаром.",                        "greenbowl/Имбирный_лимонад", "Green Bowl", listOf(Triple("S",219.0,"250 мл"), Triple("M",269.0,"350 мл"), Triple("L",319.0,"450 мл"))),
            ProductSeed("Напитки",  "Ягодный смузи",             "Смузи из черники, малины, клубники и миндального молока — антиоксиданты в каждом глотке.", "greenbowl/Ягодный_смузи", "Green Bowl", listOf(Triple("S",239.0,"300 мл"), Triple("M",289.0,"400 мл"), Triple("L",339.0,"500 мл"))),
            ProductSeed("Напитки",  "Матча Смузи",               "Смузи из маття, банана, шпината и кокосового молока — энергия и польза в одном стакане.", "greenbowl/Матча_Смузи", "Green Bowl", listOf(Triple("S",249.0,"300 мл"), Triple("M",299.0,"400 мл"), Triple("L",349.0,"500 мл"))),
            ProductSeed("Десерты",  "Энергетический батончик",   "Домашний батончик из фиников, орехов, овсянки и мёда без сахара и консервантов.",        "greenbowl/Энергетический_батончик", "Green Bowl", listOf(Triple("Штука",149.0,null), Triple("Двойная",269.0,null)))
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
