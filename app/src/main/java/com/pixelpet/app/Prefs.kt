package com.pixelpet.app

import android.content.Context

/** إعدادات بسيطة محفوظة في SharedPreferences. */
object Prefs {
    private const val FILE = "pixelpet_prefs"
    private const val K_AUTO = "auto_move"
    private const val K_SPEED = "speed"
    private const val K_X = "pos_x"
    private const val K_Y = "pos_y"
    private const val K_GENDER = "gender"
    private const val K_SOUND = "tap_sound"
    private const val K_GLASSES = "glasses"
    private const val K_SCARF = "scarf"
    private const val K_OUTFIT = "outfit"
    private const val K_HAIRSTYLE = "hairstyle"
    private const val K_QUALITY = "quality"
    private const val K_STICK_Y = "stick_ground_y"
    private const val K_STICK_FIGHTS = "stick_fights"

    const val Q_128 = 128
    const val Q_256 = 256
    const val Q_512 = 512

    const val SPEED_SLOW = 0
    const val SPEED_NORMAL = 1
    const val SPEED_FAST = 2

    const val GENDER_MALE = 0
    const val GENDER_FEMALE = 1

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun autoMove(c: Context): Boolean = sp(c).getBoolean(K_AUTO, true)
    fun setAutoMove(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_AUTO, v).apply()

    fun speed(c: Context): Int = sp(c).getInt(K_SPEED, SPEED_NORMAL)
    fun setSpeed(c: Context, v: Int) = sp(c).edit().putInt(K_SPEED, v).apply()

    /** معامل مدة الحالة: كلما زادت السرعة قصرت المدة وقلّت فترة الراحة بين الحركات. */
    fun durationScale(c: Context): Float = when (speed(c)) {
        SPEED_SLOW -> 1.6f
        SPEED_FAST -> 0.6f
        else -> 1.0f
    }

    /** فترة السكون بين حركتين (بالميلي ثانية). */
    fun idleGapMs(c: Context): Long = when (speed(c)) {
        SPEED_SLOW -> 6000L
        SPEED_FAST -> 1200L
        else -> 3000L
    }

    fun savedX(c: Context): Int = sp(c).getInt(K_X, -1)
    fun savedY(c: Context): Int = sp(c).getInt(K_Y, -1)
    fun savePos(c: Context, x: Int, y: Int) = sp(c).edit().putInt(K_X, x).putInt(K_Y, y).apply()

    /** جنس الشخصية: 0 = ذكر، 1 = أنثى. */
    fun gender(c: Context): Int = sp(c).getInt(K_GENDER, GENDER_MALE)
    fun setGender(c: Context, v: Int) = sp(c).edit().putInt(K_GENDER, v).apply()

    /** صوت الضربة عند النقر على الشخصية. */
    fun tapSound(c: Context): Boolean = sp(c).getBoolean(K_SOUND, true)
    fun setTapSound(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_SOUND, v).apply()

    /** النظارة: تُظهر أو تُخفي النظارات عن الشخصية. */
    fun glasses(c: Context): Boolean = sp(c).getBoolean(K_GLASSES, true)
    fun setGlasses(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_GLASSES, v).apply()

    /** الوشاح: يُضاف أو يُزال. */
    fun scarf(c: Context): Boolean = sp(c).getBoolean(K_SCARF, true)
    fun setScarf(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_SCARF, v).apply()

    /** الملابس: 0 كلاسيكي، 1 رياضي (هودي/فستان أزرق)، 2 رسمي (بدلة/فستان أخضر). */
    fun outfit(c: Context): Int = sp(c).getInt(K_OUTFIT, 0)
    fun setOutfit(c: Context, v: Int) = sp(c).edit().putInt(K_OUTFIT, v).apply()

    /** تسريحة الشعر: 0 قصير، 1 كيريّ/ضفائر، 2 طويل. */
    fun hairstyle(c: Context): Int = sp(c).getInt(K_HAIRSTYLE, 0)
    fun setHairstyle(c: Context, v: Int) = sp(c).edit().putInt(K_HAIRSTYLE, v).apply()

    /** دقة الشخصية بالبكسل: 128 خشن، 256 متوسط (افتراضي)، 512 أدق تفصيلاً. */
    fun quality(c: Context): Int = sp(c).getInt(K_QUALITY, Q_256)
    fun setQuality(c: Context, v: Int) = sp(c).edit().putInt(K_QUALITY, v).apply()

    /** أرضية مجموعة العيدان (y بالبكسل)، -1 = غير محفوظة. */
    fun stickGroundY(c: Context): Int = sp(c).getInt(K_STICK_Y, -1)
    fun setStickGroundY(c: Context, v: Int) = sp(c).edit().putInt(K_STICK_Y, v).apply()

    /** مشاهد القتال في وضع العيدان. */
    fun stickFights(c: Context): Boolean = sp(c).getBoolean(K_STICK_FIGHTS, true)
    fun setStickFights(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_STICK_FIGHTS, v).apply()
}
