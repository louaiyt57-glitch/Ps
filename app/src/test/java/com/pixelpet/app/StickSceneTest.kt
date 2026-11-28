package com.pixelpet.app

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * محاكاة جدولة المشاهد على JVM: كل مشهد يجب أن ينتهي وحده بلا انتظار مزامنة مفقود،
 * وأن تبقى كل شخصية على الشاشة وبإحداثيات منتهية طوال العرض.
 */
class StickSceneTest {

    @Test
    fun everySceneFinishesOnItsOwn() {
        val d = StickDirector(density = 2f, screenW = 1080)
        d.auto = false
        val dt = 1f / 60f
        for (k in 0 until 22) {
            d.playScene(k)
            var t = 0f
            while (d.actors.any { it.busy } && t < 90f) { d.update(dt); t += dt }
            assertTrue("المشهد $k عُلّق ولم ينته خلال 90 ثانية", d.actors.none { it.busy })
            for ((i, a) in d.actors.withIndex()) {
                assertTrue("المشهد $k شخصية $i: x غير منتهية", a.x.isFinite())
                assertTrue("المشهد $k شخصية $i: خرجت عن الشاشة x=${a.x}", a.x > 0f && a.x < 1080f)
            }
        }
    }
}
