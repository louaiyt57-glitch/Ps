package com.pixelpet.app

import kotlin.random.Random

/** تعبيرات الوجه */
enum class Face { NORMAL, CLOSED, SURPRISED, HAPPY, SLEEPY, ANGRY, SAD, SHY, LAUGH, YAWN, THINK }

/** نوع الحركة في الشاشة (تتحكم بها الخدمة عبر updateViewLayout) */
enum class Motion { NONE, WALK, RUN, SNEAK, ROLL, EDGE }

/** الأغراض (Props) التي تُرسم بجانب الشخصية أو في يدها */
enum class Prop {
    NONE, BED, LAPTOP, SNOWMAN, BOOK, NOTEBOOK, PHONE, SCREWDRIVER,
    SNOWBALL_BIG, SNOWBALL_SMALL, UMBRELLA, COFFEE, TEA, PIZZA, APPLE
}

/** المؤثرات العائمة حول الشخصية */
enum class Fx { NONE, ZZZ, HEARTS, STARS, STEAM, TEAR, QMARK, EXCL, NOTES, SNOW, SWEAT, BULB, ANGER }

/** مفاتيح الرسوم المتحركة (يحسبها PetPose) */
enum class Anim {
    WALK, RUN, SNEAK, JUMP, WAVE, LOOK, TRIP, CLIMB,
    YAWN, SLEEP, WAKE_JUMP, STRETCH, DROWSY, SLEEP_SHIVER, SIT_BED,
    TYPE, THINK, IDEA, READ, WRITE, PHONE, HAMMER, CHEER, SLUMP,
    SHOW, PUSH, SHIVER, SCARF, THROW, LOOK_UP, UMBRELLA,
    SIP, EAT, BELLY, RELAX,
    HAPPY, ANGRY, SHY, LAUGH, SIT_SAD, SHOCK, PROUD,
    ASK, DANCE, EXERCISE, WAVE_SOFT,
    // الحركات الجديدة (51 – 100)
    SKATE, SKIP, SHADOW_BOX, JUGGLE, YOGA, PUSHUP, HIGH_JUMP, BACKFLIP, MOONWALK, GUITAR,
    SING, MEDITATE, STARGAZE,
    NOODLE, ICE_CREAM, POPSICLE, HOT_MILK, SNACK_HIDE, WATER_DRINK, TEA_POUR,
    SNOW_ANGEL, IGLOO, WARM_HANDS, COLD_BREATH, SNOW_SHOVEL, SLED, HAT_PULL, BOOTS_STOMP,
    SELFIE, GAME_OVER, CHARGE_PHONE, BUG_FOUND, COFFEE_REFILL, WATER_PLANT, TIME_CHECK, GLASSES_CLEAN, TYPE_FAST, STRETCH_BACK,
    CHEER_UP, PATIENCE, GENIUS, HEAD_PAT, FINGER_HEART, MOTIVATED, SIGH, SHY_COVER,
    SWAY
}

enum class Category(val arabic: String) {
    MOVE("المشي والتنقل"),
    SLEEP("النوم والاستيقاظ"),
    WORK("العمل والدراسة والتقنية"),
    WINTER("الشتاء والثلج"),
    FOOD("الطعام والشراب"),
    EMOTION("المشاعر والتعبيرات"),
    FUN("أسئلة وأنشطة مرحة")
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
    val question: Question? = null
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

    // ───── ثامناً: حركات جديدة — أنشطة وحركة (51 – 65) ─────
    SKATE(51, Category.FUN, "أزلق على لوح التزحلق!", Anim.SKATE, face = Face.HAPPY, motion = Motion.RUN, durationMs = 6000),
    SKIP(52, Category.FUN, "قفزات صغيرة مرح!", Anim.SKIP, face = Face.HAPPY, durationMs = 5000),
    SHADOW_BOX(53, Category.FUN, "لكمة! وأخرى! أنا بطل!", Anim.SHADOW_BOX, face = Face.ANGRY, durationMs = 5000),
    JUGGLE(54, Category.FUN, "أراقب ثلاث كرات... ولا تسقط واحدة!", Anim.JUGGLE, face = Face.THINK, fx = Fx.STARS, durationMs = 6000),
    YOGA(55, Category.FUN, "تنفّس... واسترخِ...", Anim.YOGA, face = Face.CLOSED, durationMs = 6500),
    PUSHUP(56, Category.FUN, "عشرون... واحد وعشرون!", Anim.PUSHUP, face = Face.NORMAL, fx = Fx.SWEAT, durationMs = 6000),
    HIGH_JUMP(57, Category.FUN, "قفزة عالية جديدة!", Anim.HIGH_JUMP, face = Face.HAPPY, durationMs = 4500),
    BACKFLIP(58, Category.FUN, "شقلبة هوائية! هل رأيتها؟", Anim.BACKFLIP, face = Face.LAUGH, durationMs = 4500),
    MOONWALK(59, Category.FUN, "أمشي على القمر كما مايكل!", Anim.MOONWALK, face = Face.LAUGH, motion = Motion.WALK, durationMs = 6000),
    GUITAR(60, Category.FUN, "عزفاً على الجيتار لأجلك!", Anim.GUITAR, face = Face.HAPPY, fx = Fx.NOTES, durationMs = 6500),
    SING(61, Category.FUN, "لا لا لا... أغنيتي المفضلة!", Anim.SING, face = Face.HAPPY, fx = Fx.NOTES, durationMs = 6000),
    MEDITATE(62, Category.EMOTION, "صفاء ذهني تام...", Anim.MEDITATE, face = Face.CLOSED, durationMs = 7000),
    STARGAZE(63, Category.EMOTION, "النجوم جميلة الليلة!", Anim.STARGAZE, face = Face.HAPPY, fx = Fx.STARS, durationMs = 6500),
    SWAY(64, Category.FUN, "أتمايل مع الإيقاع بهدوء.", Anim.SWAY, face = Face.HAPPY, fx = Fx.NOTES, durationMs = 5500),
    ASK_SNACK(65, Category.FUN, "هل نأكل وجبة خفيفة؟", Anim.ASK, durationMs = 14000,
        question = Question("رائع! شوكولاتة من فضلك!", Face.HAPPY, "حسناً، سأصمد... لأدفعة الدقائق!", Face.SAD)),

    // ───── تاسعاً: طعام وشراب جديد (66 – 72) ─────
    NOODLE(66, Category.FOOD, "شعيرات صلصة... يم يم!", Anim.NOODLE, face = Face.HAPPY, fx = Fx.STEAM, durationMs = 5500),
    ICE_CREAM(67, Category.FOOD, "آيس كريم! لا يفوتني!", Anim.ICE_CREAM, face = Face.LAUGH, durationMs = 5500),
    POPSICLE(68, Category.FOOD, "مثلجة باردة في يوم حار!", Anim.POPSICLE, face = Face.HAPPY, durationMs = 5500),
    HOT_MILK(69, Category.FOOD, "كوب حليب دافئ قبل النوم.", Anim.HOT_MILK, face = Face.HAPPY, fx = Fx.STEAM, durationMs = 5500),
    SNACK_HIDE(70, Category.FOOD, "لا أحد يرى بسكويتي السري!", Anim.SNACK_HIDE, face = Face.SHY, durationMs = 5000),
    WATER_DRINK(71, Category.FOOD, "رشفة ماء باردة... رائعة!", Anim.WATER_DRINK, face = Face.HAPPY, durationMs = 5000),
    TEA_POUR(72, Category.FOOD, "أسكب الشاي بحذر شديد...", Anim.TEA_POUR, face = Face.THINK, fx = Fx.STEAM, durationMs = 5500),

    // ───── عاشراً: شتاء جديد (73 – 80) ─────
    SNOW_ANGEL(73, Category.WINTER, "أرسم ملاكاً في الثلج!", Anim.SNOW_ANGEL, face = Face.LAUGH, fx = Fx.SNOW, durationMs = 6000),
    IGLOO(74, Category.WINTER, "أبني قلعة ثلجية صغيرة.", Anim.IGLOO, face = Face.THINK, fx = Fx.SNOW, durationMs = 6500),
    WARM_HANDS(75, Category.WINTER, "أفرد يدي على النار الدافئة.", Anim.WARM_HANDS, face = Face.HAPPY, durationMs = 5500),
    COLD_BREATH(76, Category.WINTER, "انظر! نَفَسي بخار!", Anim.COLD_BREATH, face = Face.SURPRISED, fx = Fx.SNOW, durationMs = 5000),
    SNOW_SHOVEL(77, Category.WINTER, "أزيح الثلج عن الطريق!", Anim.SNOW_SHOVEL, face = Face.NORMAL, fx = Fx.SNOW, durationMs = 6000),
    SLED(78, Category.WINTER, "انطلاقة الزحليقة! حذارِ!", Anim.SLED, face = Face.LAUGH, motion = Motion.RUN, durationMs = 5000),
    HAT_PULL(79, Category.WINTER, "قبعة صوفية دافئة... تمام.", Anim.HAT_PULL, face = Face.HAPPY, durationMs = 4500),
    BOOTS_STOMP(80, Category.WINTER, "أخبط قدمي لأدفأ!", Anim.BOOTS_STOMP, face = Face.NORMAL, fx = Fx.SNOW, durationMs = 5000),

    // ───── حادي عشر: عمل وتقنية جديد (81 – 90) ─────
    SELFIE(81, Category.WORK, "صورة سيلفي! ابتسم!", Anim.SELFIE, face = Face.LAUGH, prop = Prop.PHONE, durationMs = 5000),
    GAME_OVER(82, Category.WORK, "خسرة... جولة أخرى!", Anim.GAME_OVER, face = Face.ANGRY, prop = Prop.PHONE, fx = Fx.ANGER, durationMs = 5000),
    CHARGE_PHONE(83, Category.WORK, "البطارية 1%! خطر حر!", Anim.CHARGE_PHONE, face = Face.SURPRISED, prop = Prop.PHONE, fx = Fx.EXCL, durationMs = 5000),
    BUG_FOUND(84, Category.WORK, "وجدت الخلل! في السطر 42!", Anim.BUG_FOUND, face = Face.SURPRISED, prop = Prop.LAPTOP, fx = Fx.BULB, durationMs = 5500),
    COFFEE_REFILL(85, Category.WORK, "إعادة تعبئة القهوة... الوقود الرسمي!", Anim.COFFEE_REFILL, face = Face.HAPPY, prop = Prop.COFFEE, fx = Fx.STEAM, durationMs = 5500),
    WATER_PLANT(86, Category.WORK, "أسقي نباتي الصغير كل يوم.", Anim.WATER_PLANT, face = Face.HAPPY, durationMs = 5500),
    TIME_CHECK(87, Category.WORK, "الساعة... متأخرون!", Anim.TIME_CHECK, face = Face.SURPRISED, fx = Fx.EXCL, durationMs = 4500),
    GLASSES_CLEAN(88, Category.WORK, "أنظف نظارتي، ترى الدنيا أوضح.", Anim.GLASSES_CLEAN, face = Face.CLOSED, durationMs = 5000),
    TYPE_FAST(89, Category.WORK, "موعد نهائي غداً... كتابة سريعة!", Anim.TYPE_FAST, face = Face.NORMAL, prop = Prop.LAPTOP, fx = Fx.SWEAT, durationMs = 6000),
    STRETCH_BACK(90, Category.WORK, "تمطيط الظهر بعد جلوس طويل.", Anim.STRETCH_BACK, face = Face.HAPPY, durationMs = 5000),

    // ───── ثاني عشر: مشاعر جديدة (91 – 100) ─────
    CHEER_UP(91, Category.EMOTION, "لا تحزن! سأغني لك أغنية!", Anim.CHEER_UP, face = Face.HAPPY, fx = Fx.NOTES, durationMs = 6000),
    PATIENCE(92, Category.EMOTION, "الصبر مفتاح الفرج...", Anim.PATIENCE, face = Face.THINK, fx = Fx.QMARK, durationMs = 6000),
    GENIUS(93, Category.EMOTION, "أنا عبقري... أحياناً!", Anim.GENIUS, face = Face.HAPPY, fx = Fx.BULB, durationMs = 4500),
    HEAD_PAT(94, Category.EMOTION, "شكراً للرعشة على الرأس!", Anim.HEAD_PAT, face = Face.SHY, fx = Fx.HEARTS, durationMs = 5000),
    FINGER_HEART(95, Category.EMOTION, "قلب صغير من إصبعين لك!", Anim.FINGER_HEART, face = Face.HAPPY, fx = Fx.HEARTS, durationMs = 4500),
    MOTIVATED(96, Category.EMOTION, "اليوم يومي! سأنجز كل شيء!", Anim.MOTIVATED, face = Face.HAPPY, fx = Fx.STARS, durationMs = 5500),
    SIGH(97, Category.EMOTION, "يوم طويل... آه.", Anim.SIGH, face = Face.SAD, fx = Fx.TEAR, durationMs = 5500),
    SHY_COVER(98, Category.EMOTION, "لا تنظر إليّ الآن!", Anim.SHY_COVER, face = Face.SHY, durationMs = 5000),
    ASK_WALK(99, Category.FUN, "هل نخرج للتنزه قليلاً؟", Anim.ASK, durationMs = 14000,
        question = Question("هيا! الهواء الطلق مفيد!", Face.HAPPY, "حسناً، نتنزه لاحقاً إذن.", Face.SAD)),
    ASK_STORY(100, Category.FUN, "هل أحكي لك قصة قبل النوم؟", Anim.ASK, durationMs = 14000,
        question = Question("كان يا ما كان... في قديم الزمان!", Face.HAPPY, "أكذوبة! النوم أفضل.", Face.SURPRISED));

    val isQuestion: Boolean get() = question != null

    companion object {
        val ALL: List<PetState> = entries.toList()

        fun byId(id: Int): PetState? = ALL.firstOrNull { it.id == id }

        /** اختيار عشوائي موزون: الأسئلة أقل تكراراً، ولا تتكرر الحالة نفسها مرتين متتاليتين. */
        fun random(exclude: PetState? = null): PetState {
            val pool = ALL.filter { it != exclude && !it.sleeping }
            val total = pool.sumOf { weight(it) }
            var r = Random.nextInt(total)
            for (s in pool) {
                r -= weight(s)
                if (r < 0) return s
            }
            return pool.first()
        }

        private fun weight(s: PetState): Int = if (s.isQuestion) 1 else 3

        /** الحالة التالية بعد انتهاء الحالة: سلاسل منطقية (نعاس ← نوم ← استيقاظ) أو عشوائي. */
        fun follow(current: PetState): PetState = when (current) {
            YAWN_SLEEPY -> SLEEP_BED
            SLEEP_BED -> if (Random.nextBoolean()) WAKE_ENERGETIC else WAKE_NAP
            NIGHTMARE -> WAKE_SUDDEN
            BACK_TO_SLEEP -> SLEEP_BED
            WAKE_SUDDEN -> if (Random.nextInt(3) == 0) BACK_TO_SLEEP else random(current)
            TIRED_WORK -> if (Random.nextBoolean()) YAWN_SLEEPY else random(current)
            LAPTOP_WORK -> if (Random.nextInt(3) == 0) CELEBRATE else random(current)
            NOODLE, ICE_CREAM, POPSICLE -> FULL_RELAX
            HOT_MILK -> if (Random.nextBoolean()) YAWN_SLEEPY else random(current)
            GAME_OVER -> if (Random.nextInt(3) == 0) ANGRY_CUTE else random(current)
            CHARGE_PHONE -> COFFEE_REFILL
            SIGH -> if (Random.nextBoolean()) CHEER_UP else random(current)
            else -> random(current)
        }
    }
}
