package com.pixelpet.app

import android.graphics.Color

/** الجنس: يغيّر الملامح (رموش، شفاه، لحية) والتسريحة الافتراضية. */
enum class Gender(val arabic: String) {
    MALE("ذكر"), FEMALE("أنثى")
}

/** التسريحات. لكل واحدة دالة رسم في Hair.kt ودالة حركة خاصة في Actions.kt. */
enum class HairStyle(val arabic: String) {
    SHORT("قصير"),
    LONG("طويل"),
    PONYTAIL("ذيل حصان"),
    BRAID("ضفيرة"),
    BUN("كعكة"),
    CURLY("مجعّد"),
    MOHAWK("موهوك"),
    SPIKY("أشواك")
}

/** لوحات الألوان. */
object Palette {
    val SKIN = intArrayOf(
        Color.parseColor("#F6D3B8"), Color.parseColor("#E8B48E"), Color.parseColor("#C98F6B"),
        Color.parseColor("#9C6B48"), Color.parseColor("#6F4A32"), Color.parseColor("#4A2F20")
    )
    val HAIR = intArrayOf(
        Color.parseColor("#1B1B22"), Color.parseColor("#4A2C17"), Color.parseColor("#8A5A2B"),
        Color.parseColor("#D9B25F"), Color.parseColor("#B5382B"), Color.parseColor("#9AA3AD"),
        Color.parseColor("#7C4DFF"), Color.parseColor("#2EC4B6")
    )
    val OUTFIT = intArrayOf(
        Color.parseColor("#C62828"), Color.parseColor("#E67E22"), Color.parseColor("#F1C40F"),
        Color.parseColor("#2E9E5B"), Color.parseColor("#2E86C1"), Color.parseColor("#7D3C98"),
        Color.parseColor("#E84393"), Color.parseColor("#2B2B33")
    )
}

fun shade(c: Int, f: Float): Int = Color.rgb(
    (Color.red(c) * f).toInt().coerceIn(0, 255),
    (Color.green(c) * f).toInt().coerceIn(0, 255),
    (Color.blue(c) * f).toInt().coerceIn(0, 255)
)

/**
 * مظهر الشخصية الكامل.
 * الألوان تُخزَّن كفهارس في [Palette]؛ فهرس لون الملابس 0 = اللون الأصلي للزي.
 */
data class Look(
    val gender: Gender = Gender.MALE,
    val hair: HairStyle = HairStyle.SHORT,
    val hairColorIdx: Int = 0,
    val skinIdx: Int = 1,
    val outfit: Outfit = Outfit.COAT,
    val outfitColorIdx: Int = 0,
    val beard: Boolean = true,
    val glasses: Boolean = true
) {
    val skin: Int get() = Palette.SKIN[skinIdx.coerceIn(0, Palette.SKIN.size - 1)]
    val skinD: Int get() = shade(skin, 0.82f)
    val hairCol: Int get() = Palette.HAIR[hairColorIdx.coerceIn(0, Palette.HAIR.size - 1)]
    val hairD: Int get() = shade(hairCol, 0.72f)
    val outfitMain: Int
        get() = if (outfitColorIdx <= 0) outfit.baseColor
        else Palette.OUTFIT[(outfitColorIdx - 1).coerceIn(0, Palette.OUTFIT.size - 1)]

    fun encode(): String =
        "${gender.name},${hair.name},$hairColorIdx,$skinIdx,${outfit.name},$outfitColorIdx,${if (beard) 1 else 0},${if (glasses) 1 else 0}"

    companion object {
        val DEFAULT = Look()

        /** مظهر افتراضي مناسب عند تغيير الجنس. */
        fun defaultFor(g: Gender): Look =
            if (g == Gender.FEMALE) Look(Gender.FEMALE, HairStyle.LONG, 1, 0, Outfit.DRESS, 0, beard = false, glasses = false)
            else DEFAULT

        fun decode(s: String?): Look {
            if (s.isNullOrBlank()) return DEFAULT
            return try {
                val a = s.split(",")
                Look(
                    Gender.valueOf(a[0]), HairStyle.valueOf(a[1]), a[2].toInt(), a[3].toInt(),
                    Outfit.valueOf(a[4]), a[5].toInt(), a[6] == "1", a[7] == "1"
                )
            } catch (e: Exception) {
                DEFAULT
            }
        }
    }
}
