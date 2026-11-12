package com.dorai.ai.voice.benchmark

/**
 * The Bengali sentence set the Phase 0 gate measures against.
 *
 * Every sentence is an input to a speaker: the reference transcript is the sentence
 * itself, so the sentences must be speakable aloud, sentence-shaped and stable. They
 * cover the categories the plan calls out: phoneme-rich text (heavy conjuncts), names,
 * Bengali/English mixed speech, numbers and long complex utterances.
 *
 * This set is a **draft**. A native Bengali speaker should review it (and confirm the
 * two accents — Bangladeshi and West Bengali — are fairly represented) before any
 * decision memo is signed off; treat the categories here as a starting point, not as a
 * statistically validated sample.
 */
enum class EvalCategory {
    /** Everyday sentences, natural prosody. */
    GENERAL,

    /** Names of people, places and products. */
    NAMES,

    /** Bengali prose mixed with English technical terms. */
    MIXED_CODE_SWITCH,

    /** Numbers, dates, times and quantities. */
    NUMBERS,

    /** Heavy conjuncts and virama clusters. */
    CONJUNCTS,

    /** Longer, more complex utterances. */
    LONG_FORMS,
}

data class EvalSentence(
    val id: String,
    val category: EvalCategory,
    val text: String,
)

private const val P = "bn"

private fun sentence(number: Int, category: EvalCategory, text: String): EvalSentence = EvalSentence(id = P + "-" + number.toString().padStart(3, '0'), category = category, text = text)

val VOICE_EVAL_SENTENCES: List<EvalSentence> = listOf(
    sentence(1, EvalCategory.GENERAL, "আজ দুপুরে বাজার করতে যেতে হবে।"),
    sentence(2, EvalCategory.GENERAL, "আমার অফিস থেকে বাসা অনেক দুরে।"),
    sentence(3, EvalCategory.GENERAL, "বৃষ্টি হলে আমরা বাসায় থাকব।"),
    sentence(4, EvalCategory.GENERAL, "আজকের আবহাওয়া খুবই গরম।"),
    sentence(5, EvalCategory.GENERAL, "কাল সকালে ডাক্তারের কাছে যাব।"),
    sentence(6, EvalCategory.GENERAL, "আমার বোন এখন ঢাকায় থাকেন।"),
    sentence(7, EvalCategory.GENERAL, "বাবা বাজার থেকে মাছ কিনেছেন।"),
    sentence(8, EvalCategory.GENERAL, "বৃষ্টির শব্দে আমার ভালো লাগে।"),
    sentence(9, EvalCategory.GENERAL, "আজ মা খুব সকালে উঠেছেন।"),
    sentence(10, EvalCategory.GENERAL, "রাতের খাবারে ভাত ও ডাল রান্না হয়েছে।"),
    sentence(11, EvalCategory.GENERAL, "আমার ছোট ভাই বই পড়তে ভালোবাসে।"),
    sentence(12, EvalCategory.GENERAL, "দুপুরের পর একটু বিশ্রাম নেব।"),
    sentence(13, EvalCategory.GENERAL, "আগামী সপ্তাহে আমাদের জন্মদিন।"),
    sentence(14, EvalCategory.GENERAL, "তাড়াহুড়ো করলে কিছুই হয় না।"),
    sentence(15, EvalCategory.GENERAL, "পানির ঘাটতি হলে গাছ মরে যায়।"),
    sentence(16, EvalCategory.GENERAL, "আজকের খবর পড়েছিলাম, অনেক মজার ছিল।"),
    sentence(17, EvalCategory.GENERAL, "গতকাল রাতে খুব ভালো ঘুম হয়েছে।"),
    sentence(18, EvalCategory.GENERAL, "আমার অফিস থেকে বাস বন্ধ হয়ে গেছে।"),
    sentence(19, EvalCategory.GENERAL, "পড়তে গিয়ে অনেক নতুন কিছু জানলাম।"),
    sentence(20, EvalCategory.GENERAL, "সকালের সূর্য উঠেছে এবং সকাল সতেজ হয়ে উঠেছে।"),

    sentence(21, EvalCategory.NAMES, "রহিম সাহেব আগামীকাল আসবেন।"),
    sentence(22, EvalCategory.NAMES, "সালমা খানম মিটিং করবে।"),
    sentence(23, EvalCategory.NAMES, "তানভীর হাসান ঢাকা বিশ্ববিদ্যালয়ের ছাত্র।"),
    sentence(24, EvalCategory.NAMES, "নুসরাত জাহান আমাদের ইন্টার্ন।"),
    sentence(25, EvalCategory.NAMES, "শাহরুখ খান ভারতের বিখ্যাত অভিনেতা।"),
    sentence(26, EvalCategory.NAMES, "সাকিব আল হাসান বাংলাদেশ দলের অধিনায়ক।"),
    sentence(27, EvalCategory.NAMES, "শেখ মুজিবুর রহমান বাংলাদেশের জাতির পিতা।"),
    sentence(28, EvalCategory.NAMES, "মীর মশাররফ হোসেন ছিলেন বাঙালি কবি।"),
    sentence(29, EvalCategory.NAMES, "মাইকেল মধুসূদন দত্ত ছিলেন আধুনিক বাংলা সাহিত্যের জনক।"),
    sentence(30, EvalCategory.NAMES, "কাজী নজরুল ইসলাম আমাদের জাতীয় কবি।"),
    sentence(31, EvalCategory.NAMES, "রবীন্দ্রনাথ ঠাকুর গীতাঞ্জলি লিখেছেন।"),
    sentence(32, EvalCategory.NAMES, "বেগম রোকেয়া ছিলেন সমাজ সংস্কারক।"),
    sentence(33, EvalCategory.NAMES, "হযরত মুহাম্মদ মানবতার শিক্ষক।"),
    sentence(34, EvalCategory.NAMES, "চট্টগ্রামের প্রতাপ শাহী ছিলেন বাংলার সুলতান।"),
    sentence(35, EvalCategory.NAMES, "কলকাতার সিরাজউদ্দৌলা ছিলেন বাংলার নবাব।"),
    sentence(36, EvalCategory.NAMES, "মুহাম্মদ শহীদুল্লাহ ছিলেন পণ্ডিত।"),
    sentence(37, EvalCategory.NAMES, "ইফতেখার আহমেদ আমাদের দলে নতুন।"),
    sentence(38, EvalCategory.NAMES, "মাফিউল ইসলাম খুব ভালো ছাত্র।"),
    sentence(39, EvalCategory.NAMES, "নাজমুল হক আমাদের ইঞ্জিনিয়ার।"),
    sentence(40, EvalCategory.NAMES, "আরিফুল ইসলাম কলকাতায় গিয়েছেন।"),

    sentence(41, EvalCategory.MIXED_CODE_SWITCH, "আমি today মিটিংয়ে যাব।"),
    sentence(42, EvalCategory.MIXED_CODE_SWITCH, "এই অ্যাপটা খুব slow চলে।"),
    sentence(43, EvalCategory.MIXED_CODE_SWITCH, "ফাইলে error এসেছে, চেক করো।"),
    sentence(44, EvalCategory.MIXED_CODE_SWITCH, "এই ফোল্ডারে সব document আছে।"),
    sentence(45, EvalCategory.MIXED_CODE_SWITCH, "এই সার্ভারটা down আছে।"),
    sentence(46, EvalCategory.MIXED_CODE_SWITCH, "update টা install হয়ে গেছে।"),
    sentence(47, EvalCategory.MIXED_CODE_SWITCH, "login করতে পারছি না।"),
    sentence(48, EvalCategory.MIXED_CODE_SWITCH, "wrong password দিলে ড্যাশবোর্ড খুলবে না।"),
    sentence(49, EvalCategory.MIXED_CODE_SWITCH, "build fail হয়েছে কিন্তু run হয়েছে।"),
    sentence(50, EvalCategory.MIXED_CODE_SWITCH, "স্ক্রীনে মেসেজ show করেছে।"),
    sentence(51, EvalCategory.MIXED_CODE_SWITCH, "এই bug টা fix করো।"),
    sentence(52, EvalCategory.MIXED_CODE_SWITCH, "গুগল ক্রোমে open করো।"),
    sentence(53, EvalCategory.MIXED_CODE_SWITCH, "ফাইলটা কপি করে save করো।"),
    sentence(54, EvalCategory.MIXED_CODE_SWITCH, "রিপোর্টটা email করো।"),
    sentence(55, EvalCategory.MIXED_CODE_SWITCH, "এই link টা শেয়ার করো।"),
    sentence(56, EvalCategory.MIXED_CODE_SWITCH, "কনসোলে log দেখো।"),
    sentence(57, EvalCategory.MIXED_CODE_SWITCH, "আমাদের company খুব দ্রুত বাড়ছে।"),
    sentence(58, EvalCategory.MIXED_CODE_SWITCH, "deadline শেষ হয়ে গেল।"),
    sentence(59, EvalCategory.MIXED_CODE_SWITCH, "notification টা পাইনি।"),
    sentence(60, EvalCategory.MIXED_CODE_SWITCH, "ড্যাশবোর্ডে সব data আছে।"),

    sentence(61, EvalCategory.NUMBERS, "আমার বয়স ২৬ বছর।"),
    sentence(62, EvalCategory.NUMBERS, "আজ তারিখ ১২ই নভেম্বর।"),
    sentence(63, EvalCategory.NUMBERS, "আমার নম্বর ৭।"),
    sentence(64, EvalCategory.NUMBERS, "এই বইয়ের দাম ৩০০ টাকা।"),
    sentence(65, EvalCategory.NUMBERS, "আগামীকাল সকাল ১০টায় মিটিং।"),
    sentence(66, EvalCategory.NUMBERS, "আমার বাড়ির নাম্বার ৭৭।"),
    sentence(67, EvalCategory.NUMBERS, "এই গাছে ৩০০ পাতা আছে।"),
    sentence(68, EvalCategory.NUMBERS, "আমার শেখানো ক্লাসের শিক্ষার্থী ৫৬ জন।"),
    sentence(69, EvalCategory.NUMBERS, "২০২৪ সালে স্বাধীনতা দিবস পালিত হয়েছে।"),
    sentence(70, EvalCategory.NUMBERS, "আমার ক্যালেন্ডারে ২৫ই ডিসেম্বর ডাক্তার দেখাতে আছে।"),
    sentence(71, EvalCategory.NUMBERS, "অফিসে প্রায় ১২০০ মানুষ কাজ করে।"),
    sentence(72, EvalCategory.NUMBERS, "আমার পকেটে ২টি কলম আছে।"),
    sentence(73, EvalCategory.NUMBERS, "২ লাখ ৫৬ হাজার টাকা ঋণ আছে।"),
    sentence(74, EvalCategory.NUMBERS, "গাড়ির নাম্বার ১৯-৬৫-৪৮।"),
    sentence(75, EvalCategory.NUMBERS, "পরীক্ষায় ৫০ নম্বর পেয়েছি।"),
    sentence(76, EvalCategory.NUMBERS, "এক থেকে ১০০ পর্যন্ত গণনা করতে পারো।"),
    sentence(77, EvalCategory.NUMBERS, "আজ ৩জন অতিথি আসবেন।"),
    sentence(78, EvalCategory.NUMBERS, "বাড়ির ভাড়া ৩৫০০ টাকা।"),
    sentence(79, EvalCategory.NUMBERS, "আমার ৩ বোনের সবার নাম রাখা হয়েছে।"),
    sentence(80, EvalCategory.NUMBERS, "মিটিংটি সপ্তাহে ৩বার হয়।"),

    sentence(81, EvalCategory.CONJUNCTS, "বাংলাদেশের রাজধানী ঢাকা।"),
    sentence(82, EvalCategory.CONJUNCTS, "স্বাধীনতা ও অধিকারের জন্য সংগ্রাম হয়েছে।"),
    sentence(83, EvalCategory.CONJUNCTS, "রাষ্ট্র ও রাজনীতি নিয়ে আলোচনা হয়েছে।"),
    sentence(84, EvalCategory.CONJUNCTS, "কার্যক্রমটি আগামীকাল শুরু হবে।"),
    sentence(85, EvalCategory.CONJUNCTS, "ক্ষমতাবানরা সাধারণত প্রভাব খাটায়।"),
    sentence(86, EvalCategory.CONJUNCTS, "জ্ঞানাভাবে মাঝে মাঝে ভুল সিদ্ধান্ত হয়।"),
    sentence(87, EvalCategory.CONJUNCTS, "ত্রুটিপূর্ণ ফাইলটি ঠিক করে দিতে হবে।"),
    sentence(88, EvalCategory.CONJUNCTS, "স্ট্রেসাল কাজে মন দেন না।"),
    sentence(89, EvalCategory.CONJUNCTS, "প্রয়োজনে আমাকে জানাতে হবে।"),
    sentence(90, EvalCategory.CONJUNCTS, "আকাঙ্ক্ষা থাকলে অনেক কিছু সম্ভব।"),
    sentence(91, EvalCategory.CONJUNCTS, "অশালীন কথাবার্তা বর্জনীয়।"),
    sentence(92, EvalCategory.CONJUNCTS, "তমসাচ্ছন্ন রাতে পথচলা কঠিন।"),
    sentence(93, EvalCategory.CONJUNCTS, "সংরক্ষণের জন্য ব্যবস্থা নেওয়া হয়েছে।"),
    sentence(94, EvalCategory.CONJUNCTS, "যোগ্যতা না থাকলে কাজ সঠিক হয় না।"),
    sentence(95, EvalCategory.CONJUNCTS, "স্মরণীয় ঘটনা মনে পড়ছে।"),
    sentence(96, EvalCategory.CONJUNCTS, "জটিল বিষয় সহজ করে বলা উচিত।"),
    sentence(97, EvalCategory.CONJUNCTS, "সন্তুষ্টি জীবনের সবচাইতে বড় অর্জন।"),
    sentence(98, EvalCategory.CONJUNCTS, "অস্ত্রাগারে সব সময় সতর্ক থাকা উচিত।"),
    sentence(99, EvalCategory.CONJUNCTS, "নির্ভুলভাবে কাজ শেষ করতে চাও।"),
    sentence(100, EvalCategory.CONJUNCTS, "উদাহরণ দিয়ে বুঝিয়ে দিতে হবে।"),

    sentence(101, EvalCategory.LONG_FORMS, "আজকের দিনের বৈদেশিক বাণিজ্য খুবই চ্যালেঞ্জিং, কারণ আমদানির খরচ বেড়ে গেছে।"),
    sentence(102, EvalCategory.LONG_FORMS, "আমার বাবা সারাদিন কাজ করার পর বাসায় ফিরে আবার পড়াশোনা করেন।"),
    sentence(103, EvalCategory.LONG_FORMS, "বাংলাদেশে ধান চাষ করা হয় যেখানে বেশিরভাগ মানুষের জীবিকার উৎস।"),
    sentence(104, EvalCategory.LONG_FORMS, "যদি বৃষ্টি হয় তবে আমরা রাস্তা বন্ধ করে দেব।"),
    sentence(105, EvalCategory.LONG_FORMS, "শহরের যানজট সমস্যার সমাধানের জন্য অনেক পরিকল্পনা নেওয়া হয়েছে।"),
    sentence(106, EvalCategory.LONG_FORMS, "এই শহরের নতুন সফটওয়্যার চালু হয়েছে যা সবাইকে সাহায্য করবে।"),
    sentence(107, EvalCategory.LONG_FORMS, "সঠিক খাদ্য গ্রহণ না করলে শরীর দুর্বল হয়ে যায়।"),
    sentence(108, EvalCategory.LONG_FORMS, "ভালোভাবে পড়াশোনা করলে যেকোনো পরীক্ষায় ভালো ফলাফল পাওয়া যায়।"),
    sentence(109, EvalCategory.LONG_FORMS, "মাঠের মাঝখানে একটি বিশাল গাছ আছে যেখানে কেউ বিশ্রাম নেওয়ার জন্য বসে।"),
    sentence(110, EvalCategory.LONG_FORMS, "নতুন প্রকল্পটি শুরু করতে হলে অনেক কঠোর পরিশ্রম করতে হবে।"),
    sentence(111, EvalCategory.LONG_FORMS, "পরিবারের সঙ্গে সময় কাটানো সবথেকে আনন্দদায়ক অভিজ্ঞতা।"),
    sentence(112, EvalCategory.LONG_FORMS, "পরিবেশ দূষণ রোধে আমাদের সবাইকে সচেতন হতে হবে।"),
    sentence(113, EvalCategory.LONG_FORMS, "যদি পর্যাপ্ত পড়াশোনা করা হয় তবে এই পরীক্ষায় ভালো ফলাফল পাওয়া সম্ভব।"),
    sentence(114, EvalCategory.LONG_FORMS, "ছোটবেলার স্মৃতিগুলো আজও আমার মনে উজ্জ্বল হয়ে আছে।"),
    sentence(115, EvalCategory.LONG_FORMS, "কাজের চাপ বেশি হলেও আমাদের সবাইকে একসাথে সাহায্য করতে হবে।"),
    sentence(116, EvalCategory.LONG_FORMS, "বিদ্যুৎ সাশ্রয় করলে বিল কমে যায় এবং পরিবেশও ভালো থাকে।"),
    sentence(117, EvalCategory.LONG_FORMS, "প্রতিদিনের পড়াশোনাই দীর্ঘদিনের লক্ষ্যের পথে বড় পদক্ষেপ।"),
    sentence(118, EvalCategory.LONG_FORMS, "বাংলা ভাষার সমৃদ্ধিকে আমাদের সবার দায়িত্বে রক্ষা করতে হবে।"),
    sentence(119, EvalCategory.LONG_FORMS, "যাত্রার ঠিক আগে একটা জরুরি কাজ শেষ করতে হয়েছিল।"),
    sentence(120, EvalCategory.LONG_FORMS, "সকালে উঠে বাদাম খেয়ে নিলে শরীরের শক্তি বাড়ে এবং মন স্থির থাকে।"),
)

val VOICE_EVAL_CATEGORY_COUNTS: Map<EvalCategory, Int> =
    VOICE_EVAL_SENTENCES.groupingBy { it.category }.eachCount()
