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
}
