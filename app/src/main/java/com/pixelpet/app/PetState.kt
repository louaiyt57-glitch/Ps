package com.pixelpet.app

import kotlin.random.Random

/** تعبيرات الوجه */
enum class Face { NORMAL, CLOSED, SURPRISED, HAPPY, SLEEPY, ANGRY, SAD, SHY, LAUGH, YAWN, THINK }

/** نوع الحركة في الشاشة (تتحكم بها الخدمة عبر updateViewLayout) */
enum class Motion { NONE, WALK, RUN, SNEAK, ROLL, EDGE }

/** الأغراض (Props) التي تُرسم بجانب الشخصية أو في يدها */
enum class Prop {
    NONE, PILLOW, FAN, BED, LAPTOP, SNOWMAN, BOOK, NOTEBOOK, PHONE, SCREWDRIVER,
    SNOWBALL_BIG, SNOWBALL_SMALL, UMBRELLA, COFFEE, TEA, PIZZA, APPLE
}

/** المؤثرات العائمة حول الشخصية */
enum class Fx { NONE, ZZZ, HEARTS, STARS, STEAM, TEAR, QMARK, EXCL, NOTES, SNOW, SWEAT, BULB, ANGER }

/** مفاتيح الرسوم المتحركة (يحسبها PetPose) */
enum class Anim {
    CUSTOM,   // الحركة تأتي من دالة move الخاصة بالحالة (انظر Actions.kt)
    WALK, RUN, SNEAK, JUMP, WAVE, LOOK, TRIP, CLIMB,
    YAWN, SLEEP, WAKE_JUMP, STRETCH, DROWSY, SLEEP_SHIVER, SIT_BED,
    TYPE, THINK, IDEA, READ, WRITE, PHONE, HAMMER, CHEER, SLUMP,
    SHOW, PUSH, SHIVER, SCARF, THROW, LOOK_UP, UMBRELLA,
    SIP, EAT, BELLY, RELAX,
    HAPPY, ANGRY, SHY, LAUGH, SIT_SAD, SHOCK, PROUD,
    ASK, DANCE, EXERCISE, WAVE_SOFT
}

enum class Category(val arabic: String) {
    MOVE("المشي والتنقل"),
    SLEEP("النوم والاستيقاظ"),
    WORK("العمل والدراسة والتقنية"),
    WINTER("الشتاء والثلج"),
    FOOD("الطعام والشراب"),
    EMOTION("المشاعر والتعبيرات"),
    FUN("أسئلة وأنشطة مرحة"),
    HAIR("حركات التسريحات"),
    OUTFIT("حركات الملابس")
}

/** سؤال نعم / لا مع رد فعل مختلف لكل زر */
class Question(
    val yesText: String, val yesFace: Face,
    val noText: String, val noFace: Face
)

/**
 * الحالات الـ 50 كاملة (State Machine).
 * لكل حالة: رقمها، تصنيفها، جملتها العربية، الحركة، تعبير الوجه، الغرض، المؤثر، ومدتها.
 */
enum class PetState(
    val id: Int,
    val category: Category,
    val text: String,
    val anim: Anim,
    val face: Face = Face.NORMAL,
    val prop: Prop = Prop.NONE,
    val fx: Fx = Fx.NONE,
    val motion: Motion = Motion.NONE,
    val durationMs: Long = 5000,
    val bubble: Boolean = true,
    val sleeping: Boolean = false,
    val question: Question? = null,
    /** دالة حركة مخصصة (t بالثواني، prog 0..1، الوضعية). تتجاوز anim عند وجودها. */
    val move: ((Float, Float, Pose) -> Unit)? = null,
    /** حالة خاصة بتسريحة معيّنة: لا تظهر إلا إذا اختارها المستخدم. */
    val forHair: HairStyle? = null,
    /** حالة خاصة بزي معيّن: لا تظهر إلا إذا ارتداه المستخدم. */
    val forOutfit: Outfit? = null
) {
    // ───── أولاً: المشي والتنقل (1 – 8) ─────
    WALK_NORMAL(1, Category.MOVE, "أتمشى قليلاً في شاشتك...", Anim.WALK, motion = Motion.WALK, durationMs = 8000),
    RUN_FAST(2, Category.MOVE, "مستعجل جداً! أفسحوا الطريق!", Anim.RUN, face = Face.SURPRISED, motion = Motion.RUN, durationMs = 5000),
    SNEAK_TIPTOE(3, Category.MOVE, "ششش... لا أريد إزعاجك.", Anim.SNEAK, motion = Motion.SNEAK, durationMs = 7000),
    JOY_JUMP(4, Category.MOVE, "يا سلام! يوم رائع!", Anim.JUMP, face = Face.HAPPY, durationMs = 4000),
    WAVE_HELLO(5, Category.MOVE, "أهلاً بك يا صديقي!", Anim.WAVE, face = Face.HAPPY, durationMs = 4500),
    CURIOUS_LOOK(6, Category.MOVE, "ماذا تفعل في هذا التطبيق؟", Anim.LOOK, durationMs = 5000),
    TRIP_FALL(7, Category.MOVE, "أوبس! كدت أسقط!", Anim.TRIP, face = Face.SURPRISED, fx = Fx.EXCL, durationMs = 4500),
    EDGE_CLIMB(8, Category.MOVE, "المنظر جميل من هنا!", Anim.CLIMB, face = Face.HAPPY, motion = Motion.EDGE, durationMs = 8000),

    // ───── ثانياً: النوم والاستيقاظ (9 – 15) ─────
    YAWN_SLEEPY(9, Category.SLEEP, "أشعر بالنعاس الشديد...", Anim.YAWN, face = Face.YAWN, durationMs = 4500),
    SLEEP_BED(10, Category.SLEEP, "z z z...", Anim.SLEEP, face = Face.CLOSED, prop = Prop.BED, fx = Fx.ZZZ,
        durationMs = 9000, bubble = false, sleeping = true),
    WAKE_SUDDEN(11, Category.SLEEP, "هاه؟! لم أكن نائماً!", Anim.WAKE_JUMP, face = Face.SURPRISED, fx = Fx.EXCL, durationMs = 3500),
    WAKE_ENERGETIC(12, Category.SLEEP, "أنا مستيقظ! أنا مستيقظ!", Anim.STRETCH, face = Face.HAPPY, durationMs = 4500),
    BACK_TO_SLEEP(13, Category.SLEEP, "خمس دقائق فقط...", Anim.DROWSY, face = Face.SLEEPY, prop = Prop.BED,
        durationMs = 5000, sleeping = true),
    NIGHTMARE(14, Category.SLEEP, "لا... ليس الامتحان مجدداً!", Anim.SLEEP_SHIVER, face = Face.CLOSED, prop = Prop.BED,
        fx = Fx.SWEAT, durationMs = 5500, sleeping = true),
    WAKE_NAP(15, Category.SLEEP, "صباح الخير... أم مساء الخير؟", Anim.SIT_BED, face = Face.SLEEPY, prop = Prop.BED, durationMs = 5500),

    // ───── ثالثاً: العمل والدراسة والتقنية (16 – 24) ─────
    LAPTOP_WORK(16, Category.WORK, "أكتب كوداً مهماً الآن!", Anim.TYPE, prop = Prop.LAPTOP, durationMs = 6500),
    DEEP_THINK(17, Category.WORK, "هممم... دعني أفكر قليلاً.", Anim.THINK, face = Face.THINK, fx = Fx.QMARK, durationMs = 5000),
    IDEA_BULB(18, Category.WORK, "وجدتها! فكرة عبقرية!", Anim.IDEA, face = Face.HAPPY, fx = Fx.BULB, durationMs = 4500),
    READ_BOOK(19, Category.WORK, "المعرفة قوة!", Anim.READ, prop = Prop.BOOK, durationMs = 6000),
    WRITE_NOTE(20, Category.WORK, "أسجل ملاحظة مهمة...", Anim.WRITE, prop = Prop.NOTEBOOK, durationMs = 5500),
    STARE_PHONE(21, Category.WORK, "هذا مثير للاهتمام!", Anim.PHONE, face = Face.SURPRISED, prop = Prop.PHONE, durationMs = 5500),
    FIX_BUG(22, Category.WORK, "أصلح بعض الأخطاء هنا...", Anim.HAMMER, prop = Prop.SCREWDRIVER, durationMs = 5500),
    CELEBRATE(23, Category.WORK, "تم الإنجاز بنجاح!", Anim.CHEER, face = Face.HAPPY, fx = Fx.STARS, durationMs = 4500),
    TIRED_WORK(24, Category.WORK, "أحتاج إلى استراحة قصيرة...", Anim.SLUMP, face = Face.SLEEPY, prop = Prop.LAPTOP, fx = Fx.SWEAT, durationMs = 5500),

    // ───── رابعاً: الشتاء والثلج (25 – 31) ─────
    SNOWMAN_BUILD(25, Category.WINTER, "انظر إلى صديقي الجديد!", Anim.SHOW, face = Face.HAPPY, prop = Prop.SNOWMAN, fx = Fx.SNOW, durationMs = 6000),
    SNOWBALL_ROLL(26, Category.WINTER, "كرة ثلج عملاقة في الطريق!", Anim.PUSH, prop = Prop.SNOWBALL_BIG, motion = Motion.ROLL, durationMs = 7000),
    SHIVER_COLD(27, Category.WINTER, "الجو بارد جداً هنا!", Anim.SHIVER, face = Face.SAD, fx = Fx.SNOW, durationMs = 5000),
    ADJUST_SCARF(28, Category.WINTER, "هكذا أدفأ بكثير.", Anim.SCARF, face = Face.HAPPY, durationMs = 4500),
    SNOWBALL_THROW(29, Category.WINTER, "خذ هذه!", Anim.THROW, face = Face.LAUGH, prop = Prop.SNOWBALL_SMALL, durationMs = 4000),
    WATCH_SNOW(30, Category.WINTER, "الثلج يتساقط!", Anim.LOOK_UP, face = Face.HAPPY, fx = Fx.SNOW, durationMs = 5500),
    UMBRELLA_SHELTER(31, Category.WINTER, "مستعد لأي طقس!", Anim.UMBRELLA, face = Face.HAPPY, prop = Prop.UMBRELLA, fx = Fx.SNOW, durationMs = 5500),

    // ───── خامساً: الطعام والشراب (32 – 37) ─────
    COFFEE_SIP(32, Category.FOOD, "رشفة قهوة لاستعادة النشاط!", Anim.SIP, prop = Prop.COFFEE, fx = Fx.STEAM, durationMs = 5500),
    TEA_SIP(33, Category.FOOD, "كوب شاي يروق المزاج.", Anim.SIP, face = Face.HAPPY, prop = Prop.TEA, fx = Fx.STEAM, durationMs = 5500),
    PIZZA_EAT(34, Category.FOOD, "يم يم! لذيذة جداً!", Anim.EAT, face = Face.HAPPY, prop = Prop.PIZZA, durationMs = 5500),
    APPLE_EAT(35, Category.FOOD, "صحة وعافية!", Anim.EAT, face = Face.HAPPY, prop = Prop.APPLE, durationMs = 5500),
    HUNGRY(36, Category.FOOD, "بطني يقرقر... حان وقت الأكل!", Anim.BELLY, face = Face.SAD, durationMs = 5000),
    FULL_RELAX(37, Category.FOOD, "الحمد لله، شبعت تماماً.", Anim.RELAX, face = Face.HAPPY, durationMs = 5500),

    // ───── سادساً: المشاعر والتعبيرات (38 – 44) ─────
    LOVE_HAPPY(38, Category.EMOTION, "أنا سعيد جداً بوجودك!", Anim.HAPPY, face = Face.HAPPY, fx = Fx.HEARTS, durationMs = 5000),
    ANGRY_CUTE(39, Category.EMOTION, "توقف عن وخزي!", Anim.ANGRY, face = Face.ANGRY, fx = Fx.ANGER, durationMs = 4500),
    SHY_BLUSH(40, Category.EMOTION, "أخجلتني بكلامك...", Anim.SHY, face = Face.SHY, durationMs = 5000),
    LAUGH(41, Category.EMOTION, "هاهاها! هذا مضحك!", Anim.LAUGH, face = Face.LAUGH, durationMs = 4500),
    SAD_LIGHT(42, Category.EMOTION, "أشعر بالملل قليلاً...", Anim.SIT_SAD, face = Face.SAD, fx = Fx.TEAR, durationMs = 5500),
    SHOCK(43, Category.EMOTION, "مستحيل! حقاً؟!", Anim.SHOCK, face = Face.SURPRISED, fx = Fx.EXCL, durationMs = 4000),
    PROUD(44, Category.EMOTION, "لا شيء يوقفني اليوم!", Anim.PROUD, face = Face.HAPPY, durationMs = 5000),

    // ───── سابعاً: أسئلة نعم/لا وأنشطة مرحة (45 – 50) ─────
    ASK_MATCH(45, Category.FUN, "هل توجد مباراة الليلة؟", Anim.ASK, durationMs = 14000,
        question = Question("رائع! سأشجع معك!", Face.HAPPY, "حسناً، ربما غداً.", Face.SAD)),
    ASK_BREAK(46, Category.FUN, "هل أخذت استراحة اليوم؟", Anim.ASK, durationMs = 14000,
        question = Question("أحسنت! الراحة مهمة.", Face.HAPPY, "خذ استراحة الآن، تستحقها!", Face.SURPRISED)),
    ASK_WATER(47, Category.FUN, "هل شربت الماء مؤخراً؟", Anim.ASK, durationMs = 14000,
        question = Question("ممتاز! استمر!", Face.HAPPY, "اشرب كوب ماء الآن!", Face.SURPRISED)),
    DANCE(48, Category.FUN, "وقت الرقص والموسيقى!", Anim.DANCE, face = Face.HAPPY, fx = Fx.NOTES, durationMs = 6500),
    EXERCISE(49, Category.FUN, "واحد، اثنان... حافظ على لياقتك!", Anim.EXERCISE, face = Face.HAPPY, fx = Fx.SWEAT, durationMs = 6500),
    GOODBYE(50, Category.FUN, "أراك لاحقاً يا بطل!", Anim.WAVE_SOFT, face = Face.HAPPY, durationMs = 4500),

    // ───── ثامناً: حركة خاصة لكل تسريحة (51 – 58) ─────
    HAIR_SHORT(51, Category.HAIR, "شعري مرتب اليوم... أو ربما لا!", Anim.CUSTOM, durationMs = 5000,
        move = Actions::hairRuffle, forHair = HairStyle.SHORT),
    HAIR_LONG(52, Category.HAIR, "شعري ينساب كالحرير!", Anim.CUSTOM, fx = Fx.STARS, durationMs = 5600,
        move = Actions::hairFlip, forHair = HairStyle.LONG),
    HAIR_PONYTAIL(53, Category.HAIR, "ذيل الحصان يرقص معي!", Anim.CUSTOM, durationMs = 5000,
        move = Actions::ponyHop, forHair = HairStyle.PONYTAIL),
    HAIR_BRAID(54, Category.HAIR, "ضفيرتي جميلة اليوم...", Anim.CUSTOM, fx = Fx.HEARTS, durationMs = 5500,
        move = Actions::braidTwirl, forHair = HairStyle.BRAID),
    HAIR_BUN(55, Category.HAIR, "أثبّت الكعكة قبل الخروج.", Anim.CUSTOM, durationMs = 5000,
        move = Actions::bunAdjust, forHair = HairStyle.BUN),
    HAIR_CURLY(56, Category.HAIR, "شعري يقفز معي! بوينغ!", Anim.CUSTOM, durationMs = 5000,
        move = Actions::curlyBounce, forHair = HairStyle.CURLY),
    HAIR_MOHAWK(57, Category.HAIR, "روك آند رول!", Anim.CUSTOM, fx = Fx.NOTES, durationMs = 5500,
        move = Actions::mohawkBang, forHair = HairStyle.MOHAWK),
    HAIR_SPIKY(58, Category.HAIR, "شعري حاد كالسيف!", Anim.CUSTOM, fx = Fx.STARS, durationMs = 5000,
        move = Actions::spikyProud, forHair = HairStyle.SPIKY),

    // ───── تاسعاً: حركة خاصة لكل زي (59 – 68) ─────
    OUTFIT_COAT(59, Category.OUTFIT, "أزرّر المعطف... الجو بارد!", Anim.CUSTOM, fx = Fx.SNOW, durationMs = 5500,
        move = Actions::coatButton, forOutfit = Outfit.COAT),
    OUTFIT_TSHIRT(60, Category.OUTFIT, "انظر إلى عضلاتي!", Anim.CUSTOM, durationMs = 5000,
        move = Actions::tshirtFlex, forOutfit = Outfit.TSHIRT),
    OUTFIT_HOODIE(61, Category.OUTFIT, "هكذا أفضل... لا أحد يراني!", Anim.CUSTOM, durationMs = 6500,
        move = Actions::hoodiePull, forOutfit = Outfit.HOODIE),
    OUTFIT_SUIT(62, Category.OUTFIT, "يجب أن أبدو أنيقاً!", Anim.CUSTOM, fx = Fx.STARS, durationMs = 5500,
        move = Actions::suitTie, forOutfit = Outfit.SUIT),
    OUTFIT_DRESS(63, Category.OUTFIT, "انظر كيف يدور فستاني!", Anim.CUSTOM, fx = Fx.HEARTS, durationMs = 5500,
        move = Actions::dressTwirl, forOutfit = Outfit.DRESS),
    OUTFIT_SKIRT(64, Category.OUTFIT, "تشرفت بمعرفتك!", Anim.CUSTOM, durationMs = 5000,
        move = Actions::skirtCurtsy, forOutfit = Outfit.SKIRT),
    OUTFIT_THOBE(65, Category.OUTFIT, "السلام عليكم ورحمة الله!", Anim.CUSTOM, durationMs = 5500,
        move = Actions::thobeSalam, forOutfit = Outfit.THOBE),
    OUTFIT_SPORT(66, Category.OUTFIT, "لا للكسل! تمرين صباحي!", Anim.CUSTOM, fx = Fx.SWEAT, durationMs = 5500,
        move = Actions::sportJog, forOutfit = Outfit.SPORT),
    OUTFIT_PAJAMAS(67, Category.OUTFIT, "وسادتي العزيزة... تصبحين على خير.", Anim.CUSTOM, prop = Prop.PILLOW, fx = Fx.ZZZ, durationMs = 6500,
        move = Actions::pajamaHug, forOutfit = Outfit.PAJAMAS),
    OUTFIT_SUMMER(68, Category.OUTFIT, "الجو حار جداً اليوم!", Anim.CUSTOM, prop = Prop.FAN, fx = Fx.SWEAT, durationMs = 5500,
        move = Actions::summerFan, forOutfit = Outfit.SUMMER);

    val isQuestion: Boolean get() = question != null

    /** هل تناسب هذه الحالة مظهر المستخدم الحالي؟ (حركات التسريحات والملابس الخاصة) */
    fun fits(look: Look): Boolean =
        (forHair == null || forHair == look.hair) && (forOutfit == null || forOutfit == look.outfit)

    companion object {
        val ALL: List<PetState> = entries.toList()

        fun byId(id: Int): PetState? = ALL.firstOrNull { it.id == id }

        /** اختيار عشوائي موزون: الأسئلة أقل تكراراً، ولا تتكرر الحالة نفسها مرتين متتاليتين. */
        fun random(exclude: PetState? = null, look: Look = Look.DEFAULT): PetState {
            val pool = ALL.filter { it != exclude && !it.sleeping && it.fits(look) }
            val total = pool.sumOf { weight(it) }
            var r = Random.nextInt(total)
            for (s in pool) {
                r -= weight(s)
                if (r < 0) return s
            }
            return pool.first()
        }

        /** حركات التسريحة/الزي الحاليين أكثر ظهوراً ليراها المستخدم. */
        private fun weight(s: PetState): Int = when {
            s.isQuestion -> 1
            s.forHair != null || s.forOutfit != null -> 5
            else -> 3
        }

        /** الحالة التالية بعد انتهاء الحالة: سلاسل منطقية (نعاس ← نوم ← استيقاظ) أو عشوائي. */
        fun follow(current: PetState, look: Look = Look.DEFAULT): PetState = when (current) {
            YAWN_SLEEPY -> SLEEP_BED
            SLEEP_BED -> if (Random.nextBoolean()) WAKE_ENERGETIC else WAKE_NAP
            NIGHTMARE -> WAKE_SUDDEN
            BACK_TO_SLEEP -> SLEEP_BED
            WAKE_SUDDEN -> if (Random.nextInt(3) == 0) BACK_TO_SLEEP else random(current, look)
            TIRED_WORK -> if (Random.nextBoolean()) YAWN_SLEEPY else random(current, look)
            LAPTOP_WORK -> if (Random.nextInt(3) == 0) CELEBRATE else random(current, look)
            else -> random(current, look)
        }
    }
}
