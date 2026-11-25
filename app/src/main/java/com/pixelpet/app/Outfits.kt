package com.pixelpet.app

import android.graphics.Color
import kotlin.math.abs

enum class Sleeve { NONE, SHORT, LONG }

/**
 * سياق رسم الزي: يحمل مقاسات الجسم واللون الرئيسي، ويُمرَّر لدوال الرسم.
 * الإحداثيات: الأصل عند القدمين، وy السالب للأعلى. neck = مستوى الرقبة، bb = أسفل الجذع.
 */
class OC(
    val r: PetRenderer, val look: Look, val p: Pose, val t: Float, val side: Boolean,
    val legH: Float, val bb: Float, val neck: Float
) {
    val skin = look.skin
    val skinD = look.skinD
    val main = look.outfitMain
    val dark = shade(main, 0.74f)
    val light = shade(main, 1.25f)

    /** نصف عرض الجذع (الأنثى أنحف قليلاً). */
    val w: Float = if (look.gender == Gender.FEMALE) 3.7f else 4.0f
    val hw: Float get() = if (side) 3.0f else w

    fun rect(x0: Float, y0: Float, x1: Float, y1: Float, c: Int, a: Int = 255) = r.rect(x0, y0, x1, y1, c, a)
}

/**
 * الأزياء العشرة. لكل زي ثلاث دوال رسم: legs (الأرجل والحذاء)، torso (الجذع/الفستان)، neckwear (الرقبة).
 * تُرسم بهذا الترتيب: الأرجل ثم الجذع ثم الرقبة ثم الرأس ثم الذراعان.
 */
enum class Outfit(
    val arabic: String,
    val baseColor: Int,
    val sleeve: Sleeve,
    val legs: (OC) -> Unit,
    val torso: (OC) -> Unit,
    val neckwear: (OC) -> Unit,
    val sleeveColor: Int? = null,
    val hood: Boolean = false
) {
    COAT("معطف شتوي", Color.parseColor("#7A4A2B"), Sleeve.LONG, Outfits::coatLegs, Outfits::coatTorso, Outfits::coatNeck),
    TSHIRT("قميص وجينز", Color.parseColor("#2E86AB"), Sleeve.SHORT, Outfits::tshirtLegs, Outfits::tshirtTorso, Outfits::tshirtNeck),
    HOODIE("هودي", Color.parseColor("#6F86A0"), Sleeve.LONG, Outfits::hoodieLegs, Outfits::hoodieTorso, Outfits::hoodieNeck, hood = true),
    SUIT("بدلة رسمية", Color.parseColor("#2C3E50"), Sleeve.LONG, Outfits::suitLegs, Outfits::suitTorso, Outfits::suitNeck),
    DRESS("فستان", Color.parseColor("#C2185B"), Sleeve.SHORT, Outfits::dressLegs, Outfits::dressTorso, Outfits::dressNeck),
    SKIRT("تنورة وبلوزة", Color.parseColor("#8E3B6B"), Sleeve.LONG, Outfits::skirtLegs, Outfits::skirtTorso, Outfits::skirtNeck,
        sleeveColor = Color.parseColor("#F4F1EC")),
    THOBE("ثوب / جلابية", Color.parseColor("#F2EFE6"), Sleeve.LONG, Outfits::thobeLegs, Outfits::thobeTorso, Outfits::thobeNeck),
    SPORT("ملابس رياضية", Color.parseColor("#D32F2F"), Sleeve.LONG, Outfits::sportLegs, Outfits::sportTorso, Outfits::sportNeck),
    PAJAMAS("بيجاما", Color.parseColor("#8EC5FC"), Sleeve.LONG, Outfits::pajamaLegs, Outfits::pajamaTorso, Outfits::pajamaNeck),
    SUMMER("ملابس صيفية", Color.parseColor("#FFB74D"), Sleeve.NONE, Outfits::summerLegs, Outfits::summerTorso, Outfits::summerNeck)
}

// ───────────────────────── أدوات مشتركة ─────────────────────────

private fun OC.legFront(x: Float, lift: Float, col: Int, shoe: Int, bare: Float) {
    val top = -legH - lift
    val bot = -1f - lift
    val cut = if (bare > 0f) maxOf(top + 0.6f, bot - bare) else bot
    if (bare > 0f) rect(x, top, x + 3f, bot, skin)
    rect(x, top, x + 3f, cut, col)
    rect(x - 0.4f, bot - 0.2f, x + 3.4f, -lift, shoe)
}

/** رجل جانبية: القدم تتجه للأمام (+x). */
private fun OC.legSide(x: Float, lift: Float, col: Int, shoe: Int, bare: Float) {
    val top = -legH - lift
    val bot = -1f - lift
    val cut = if (bare > 0f) maxOf(top + 0.6f, bot - bare) else bot
    if (bare > 0f) rect(x - 1.5f, top, x + 1.5f, bot, skin)
    rect(x - 1.5f, top, x + 1.5f, cut, col)
    rect(x - 1.5f, bot - 0.2f, x + 2.9f, -lift, shoe)
}

/** بنطال/شورت/أرجل عارية (col = لون البشرة). bare = طول الجزء العاري في الأسفل. */
private fun OC.pants(col: Int, shoe: Int, bare: Float = 0f) {
    if (side) {
        legSide(-p.swing, p.legR, shade(col, 0.78f), shoe, bare)   // الرجل الخلفية أغمق
        legSide(p.swing, p.legL, col, shoe, bare)
    } else {
        legFront(-3.7f - p.legSpread, p.legL, col, shoe, bare)
        legFront(0.7f + p.legSpread, p.legR, col, shoe, bare)
    }
}

/** جذع صندوقي (معطف/قميص): من الرقبة إلى الورك. */
private fun OC.torsoBox(col: Int, colD: Int, hem: Float = 0.4f) {
    rect(-hw - hem, bb - 3f, hw + hem, bb, col)
    rect(-hw, neck, hw, bb - 3f, col)
    rect(-hw, neck, -hw + 0.8f, bb, colD)
    if (!side) rect(hw - 0.8f, neck, hw, bb, colD)
    rect(-hw - hem, bb - 0.8f, hw + hem, bb, colD)
}

/** تنورة/فستان/ثوب: من الخصر حتى الحافة hemY، تتسع تدريجياً، وتتمايل مع المشي والدوران. */
private fun OC.skirt(col: Int, hemY: Float, flare: Float, pleats: Boolean = false) {
    val topY = bb - 3.2f
    if (hemY <= topY + 0.5f) return
    val rows = (hemY - topY).toInt().coerceAtLeast(1)
    val rh = (hemY - topY) / rows
    val fl = flare + p.flare * 2.4f
    for (i in 0 until rows) {
        val f = (i + 1f) / rows
        val half = hw + 0.4f + fl * f * (if (side) 0.55f else 1f)
        val sx = if (side) -p.swing * 0.18f * f else 0f
        val y0 = topY + i * rh
        rect(-half + sx, y0, half + sx, y0 + rh + 0.02f, if (i == rows - 1) dark else col)
        if (pleats && !side && i < rows - 1) {
            for (k in -2..2) rect(k * 1.7f - 0.15f, y0, k * 1.7f + 0.15f, y0 + rh, dark, 150)
        }
    }
}

private fun OC.placket(colD: Int, to: Float = bb) {
    if (side) rect(1.6f, neck + 1.4f, 2.2f, to, colD) else rect(-0.3f, neck + 1.4f, 0.3f, to, colD)
}

private fun OC.buttons(col: Int, n: Int = 2) {
    for (i in 0 until n) {
        val y = neck + 3.5f + i * 2f
        if (side) rect(2.2f, y, 2.9f, y + 0.7f, col) else rect(0.9f, y, 1.6f, y + 0.7f, col)
    }
}

private fun OC.neckSkin() {
    if (side) rect(-1.4f, neck - 1.2f, 1.8f, neck + 0.4f, skin) else rect(-1.6f, neck - 1.2f, 1.6f, neck + 0.4f, skin)
}

private val INK = Color.parseColor("#1B1B22")
private val WHITE = Color.parseColor("#F4F4F4")

/** دوال رسم الأزياء. */
object Outfits {

    // ───── 1) معطف شتوي (الشكل الأصلي: معطف بني + وشاح أحمر) ─────
    fun coatLegs(x: OC) = x.pants(Color.parseColor("#3A2B22"), Color.parseColor("#1E1E1E"))

    fun coatTorso(x: OC) {
        x.torsoBox(x.main, x.dark)
        x.placket(x.dark)
        x.buttons(INK)
    }

    fun coatNeck(x: OC) {
        val neck = x.neck
        val scarf = Color.parseColor("#C62828")
        val scarfD = Color.parseColor("#8E1B1B")
        if (!x.side) {
            val sw = kotlin.math.sin(x.t * 3f) * 0.3f
            x.rect(-4.6f, neck - 1.3f, 4.6f, neck + 1.5f, scarf)
            x.rect(-4.6f, neck + 0.9f, 4.6f, neck + 1.5f, scarfD)
            x.rect(0.6f + sw, neck + 1.5f, 3.2f + sw, neck + 6.5f, scarf)
            x.rect(0.6f + sw, neck + 5.6f, 3.2f + sw, neck + 6.5f, scarfD)
        } else {
            // الوشاح يرفرف للخلف ويطول عند الركض
            val speed = (abs(x.p.swing) / 3.8f).coerceIn(0f, 1f)
            val tail = 3f + speed * 3f
            val fl = kotlin.math.sin(x.t * 9f) * 0.5f * (0.4f + speed)
            x.rect(-3.8f, neck - 1.3f, 3.8f, neck + 1.5f, scarf)
            x.rect(-3.8f, neck + 0.9f, 3.8f, neck + 1.5f, scarfD)
            x.rect(-3.8f - tail, neck + 0.6f + fl, -3.4f, neck + 2.4f + fl, scarf)
            x.rect(-3.8f - tail, neck + 1.8f + fl, -3.4f, neck + 2.4f + fl, scarfD)
        }
    }

    // ───── 2) قميص وجينز ─────
    fun tshirtLegs(x: OC) = x.pants(Color.parseColor("#3B5B8C"), Color.parseColor("#ECECEC"))

    fun tshirtTorso(x: OC) {
        x.torsoBox(x.main, x.dark, 0.2f)
        if (!x.side) x.rect(-1.2f, x.neck + 3.2f, 1.2f, x.neck + 5.2f, x.light)   // شعار صغير
    }

    fun tshirtNeck(x: OC) {
        x.neckSkin()
        if (x.side) x.rect(-2.2f, x.neck - 0.2f, 2.4f, x.neck + 0.8f, x.dark)
        else x.rect(-2.6f, x.neck - 0.2f, 2.6f, x.neck + 0.8f, x.dark)
    }

    // ───── 3) هودي ─────
    fun hoodieLegs(x: OC) = x.pants(Color.parseColor("#3E4451"), Color.parseColor("#F2F2F2"))

    fun hoodieTorso(x: OC) {
        x.torsoBox(x.main, x.dark)
        if (!x.side) {
            x.rect(-2.6f, x.bb - 4.6f, 2.6f, x.bb - 2.2f, x.dark)                 // جيب الكنغر
            x.rect(-1.6f, x.neck + 0.6f, -1.1f, x.neck + 4f, WHITE)               // خيطا القلنسوة
            x.rect(1.1f, x.neck + 0.6f, 1.6f, x.neck + 4f, WHITE)
        } else {
            x.rect(0.2f, x.bb - 4.4f, 2.8f, x.bb - 2.4f, x.dark)
        }
    }

    fun hoodieNeck(x: OC) {
        x.neckSkin()
        x.rect(-x.hw - 0.6f, x.neck - 1f, x.hw + 0.6f, x.neck + 1.8f, x.dark)     // القلنسوة المطوية خلف الرقبة
    }

    // ───── 4) بدلة رسمية ─────
    fun suitLegs(x: OC) = x.pants(shade(x.main, 0.8f), Color.parseColor("#151515"))

    fun suitTorso(x: OC) {
        x.torsoBox(x.main, x.dark)
        val tie = Color.parseColor("#B03A48")
        if (!x.side) {
            x.rect(-1.4f, x.neck, 1.4f, x.neck + 5.2f, WHITE)                    // القميص
            x.rect(-0.45f, x.neck + 0.9f, 0.45f, x.neck + 5.6f, tie)             // ربطة العنق
            x.rect(-0.7f, x.neck + 0.3f, 0.7f, x.neck + 1.2f, shade(tie, 0.8f))
            x.rect(-2.3f, x.neck, -1.4f, x.neck + 4.6f, x.dark)                  // طيات الياقة
            x.rect(1.4f, x.neck, 2.3f, x.neck + 4.6f, x.dark)
            x.rect(0.9f, x.bb - 4.4f, 1.6f, x.bb - 3.7f, INK)
        } else {
            x.rect(1.7f, x.neck, 2.6f, x.neck + 5f, WHITE)
            x.rect(1.9f, x.neck + 1f, 2.4f, x.neck + 5.4f, tie)
            x.rect(1.0f, x.neck, 1.7f, x.neck + 4.4f, x.dark)
        }
    }

    fun suitNeck(x: OC) {
        x.neckSkin()
        if (x.side) x.rect(-2f, x.neck - 0.6f, 2.4f, x.neck + 0.6f, WHITE)
        else x.rect(-2.2f, x.neck - 0.6f, 2.2f, x.neck + 0.6f, WHITE)
    }

    // ───── 5) فستان ─────
    fun dressLegs(x: OC) = x.pants(x.skin, Color.parseColor("#3B1F1F"))

    fun dressTorso(x: OC) {
        val hw = x.hw
        x.rect(-hw, x.neck, hw, x.bb - 3f, x.main)
        x.rect(-hw, x.neck, -hw + 0.8f, x.bb - 3f, x.dark)
        if (!x.side) x.rect(hw - 0.8f, x.neck, hw, x.bb - 3f, x.dark)
        x.skirt(x.main, if (x.legH > 3f) -1.5f else -0.7f, 2.4f)
        x.rect(-hw - 0.2f, x.bb - 3.8f, hw + 0.2f, x.bb - 2.8f, x.light)         // حزام
    }

    fun dressNeck(x: OC) {
        x.neckSkin()
        x.rect(if (x.side) -1.8f else -2.2f, x.neck, 2.2f, x.neck + 1.4f, x.skin)
        x.rect(if (x.side) -1.8f else -2.2f, x.neck + 1.4f, 2.2f, x.neck + 1.9f, x.dark)
    }

    // ───── 6) تنورة وبلوزة ─────
    fun skirtLegs(x: OC) = x.pants(x.skin, Color.parseColor("#2D2D2D"))

    fun skirtTorso(x: OC) {
        val blouse = Color.parseColor("#F4F1EC")
        val blouseD = Color.parseColor("#D8D2C8")
        val hw = x.hw
        x.rect(-hw, x.neck, hw, x.bb - 3f, blouse)
        x.rect(-hw, x.neck, -hw + 0.8f, x.bb - 3f, blouseD)
        if (!x.side) x.rect(hw - 0.8f, x.neck, hw, x.bb - 3f, blouseD)
        x.skirt(x.main, minOf(-3.8f, x.bb - 0.5f), 1.4f, pleats = true)
        x.rect(-hw - 0.2f, x.bb - 3.4f, hw + 0.2f, x.bb - 2.6f, x.dark)         // حزام التنورة
    }

    fun skirtNeck(x: OC) {
        x.neckSkin()
        if (x.side) {
            x.rect(1.2f, x.neck, 2.6f, x.neck + 1.2f, x.main)                    // فيونكة
        } else {
            x.rect(-1.8f, x.neck - 0.2f, -0.3f, x.neck + 1.2f, x.main)
            x.rect(0.3f, x.neck - 0.2f, 1.8f, x.neck + 1.2f, x.main)
            x.rect(-0.4f, x.neck + 0.1f, 0.4f, x.neck + 1f, x.dark)
        }
    }

    // ───── 7) ثوب / جلابية ─────
    fun thobeLegs(x: OC) = x.pants(x.skin, Color.parseColor("#6B4A2B"))

    fun thobeTorso(x: OC) {
        val hw = x.hw
        val hem = if (x.legH > 3f) -1.3f else -0.7f
        x.rect(-hw, x.neck, hw, x.bb - 3f, x.main)
        x.rect(-hw, x.neck, -hw + 0.8f, x.bb - 3f, x.dark)
        if (!x.side) x.rect(hw - 0.8f, x.neck, hw, x.bb - 3f, x.dark)
        x.skirt(x.main, hem, 1.0f)
        x.placket(x.dark, hem - 0.5f)
        x.buttons(shade(x.main, 0.6f), 3)
    }

    fun thobeNeck(x: OC) {
        x.neckSkin()
        x.rect(if (x.side) -1.8f else -2f, x.neck - 0.4f, 2f, x.neck + 0.8f, x.dark)
    }

    // ───── 8) ملابس رياضية ─────
    fun sportLegs(x: OC) = x.pants(shade(x.main, 0.7f), Color.parseColor("#F2F2F2"))

    fun sportTorso(x: OC) {
        x.torsoBox(x.main, x.dark)
        if (!x.side) {
            x.rect(-x.hw + 0.9f, x.neck + 1f, -x.hw + 1.5f, x.bb, WHITE)          // خطوط الجانبين
            x.rect(x.hw - 1.5f, x.neck + 1f, x.hw - 0.9f, x.bb, WHITE)
            x.rect(-0.2f, x.neck + 1.4f, 0.2f, x.bb, WHITE)                        // السحّاب
        } else {
            x.rect(-0.3f, x.neck + 1f, 0.5f, x.bb, WHITE)
        }
    }

    fun sportNeck(x: OC) {
        x.neckSkin()
        x.rect(if (x.side) -2.2f else -2.4f, x.neck - 0.8f, 2.4f, x.neck + 0.8f, x.dark)
    }

    // ───── 9) بيجاما ─────
    fun pajamaLegs(x: OC) = x.pants(shade(x.main, 1.08f), Color.parseColor("#E58FB3"))

    fun pajamaTorso(x: OC) {
        x.torsoBox(x.main, x.dark, 0.2f)
        for (k in 0..3) x.rect(-x.hw, x.neck + 1.2f + k * 2.4f, x.hw, x.neck + 1.8f + k * 2.4f, x.light)   // خطوط
        x.buttons(WHITE, 3)
    }

    fun pajamaNeck(x: OC) {
        x.neckSkin()
        x.rect(-2.2f, x.neck - 0.2f, 2.2f, x.neck + 0.7f, x.light)
    }

    // ───── 10) ملابس صيفية (تي شيرت بلا أكمام + شورت) ─────
    fun summerLegs(x: OC) =
        x.pants(Color.parseColor("#F3E9D2"), Color.parseColor("#8B5E3C"), (x.legH - 1f - 2.2f).coerceAtLeast(0f))

    fun summerTorso(x: OC) {
        x.torsoBox(x.main, x.dark, 0.2f)
        x.rect(-x.hw, x.neck, x.hw, x.neck + 1.8f, x.skin)                           // كتفان عاريان
        if (x.side) {
            x.rect(-0.8f, x.neck, 1.4f, x.neck + 1.8f, x.main)
        } else {
            x.rect(-2.8f, x.neck, -1.5f, x.neck + 1.8f, x.main)
            x.rect(1.5f, x.neck, 2.8f, x.neck + 1.8f, x.main)
        }
    }

    fun summerNeck(x: OC) = x.neckSkin()
}
