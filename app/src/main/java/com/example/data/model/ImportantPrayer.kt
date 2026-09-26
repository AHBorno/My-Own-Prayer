package com.example.data.model

data class PrayerStep(
  val stepNumber: Int,
  val positionTitleEn: String,
  val positionTitleBn: String,
  val positionTitleAr: String,
  val count: Int,
  val instructionEn: String,
  val instructionBn: String,
  val instructionAr: String
)

data class ImportantPrayerItem(
  val id: String,
  val titleEn: String,
  val titleBn: String,
  val titleAr: String,
  val subtitleEn: String,
  val subtitleBn: String,
  val subtitleAr: String,
  val rakahs: String,
  val rakahsBn: String,
  val rakahsAr: String,
  val virtuesEn: String,
  val virtuesBn: String,
  val virtuesAr: String,
  val methodEn: String,
  val methodBn: String,
  val methodAr: String,
  val duaArabic: String,
  val duaPronunciationBn: String,
  val duaPronunciationEn: String,
  val duaMeaningEn: String,
  val duaMeaningBn: String,
  val duaMeaningAr: String,
  val steps: List<PrayerStep> = emptyList(),
  val importantNoteEn: String? = null,
  val importantNoteBn: String? = null,
  val importantNoteAr: String? = null
)

object ImportantPrayersRepository {
  val prayers: List<ImportantPrayerItem> = listOf(
    // 1. Salatul Istikhara (ইস্তিখারার নামায)
    ImportantPrayerItem(
      id = "istikhara",
      titleEn = "Salatul Istikhara",
      titleBn = "ইস্তিখারার নামায",
      titleAr = "صلاة الاستخارة",
      subtitleEn = "Seeking Divine Guidance for Decisions",
      subtitleBn = "কল্যাণ ও সঠিক সিদ্ধান্ত কামনায় বিশেষ সালাত",
      subtitleAr = "طلب الخير والتوفيق من الله في الأمور",
      rakahs = "2 Rak'ahs (Nafl)",
      rakahsBn = "২ রাকা'আত (নফল)",
      rakahsAr = "ركعتان نافلة",
      virtuesEn = "When deciding on an important matter where the outcome is uncertain, performing Salatul Istikhara asks Allah, the All-Knowing and All-Powerful, to decree and facilitate the path of ultimate good and protect from harm.",
      virtuesBn = "যখন এমন কোনো কাজ করিবার ইচ্ছা করিবে, যাহা ভালো মন্দ শুভ-অশুভ নিশ্চিত জানা নাই, তখন দুই রাকা'আত নামায পড়িয়া আল্লাহ্‌র কাছে ভালোই ও মঙ্গল, খাইর ও বরকত কামনা করিয়া প্রার্থনা করাকে ইস্তিখারা বলে।",
      virtuesAr = "عندما يهمّ المسلم بأمر مباح لا يدري عاقبته أخير هو أم شر، يصلي ركعتين من غير الفريضة ثم يدعو بدعاء الاستخارة طلباً للخير والبركة وتفويض الأمر إلى علم الله وحكمته.",
      methodEn = "1. Perform fresh Wudu (ablution) with sincerity.\n2. Pray 2 Rak'ahs of voluntary prayer (preferably after Isha or at any non-forbidden time).\n3. After completing the prayer, praise Allah and send Salawat (Durud) upon Prophet Muhammad ﷺ.\n4. Recite the authentic Istikhara Dua with deep concentration, mentioning your specific decision or affair at the words 'Hathal-Amr' (هَذَا الْأَمْرَ).\n5. Sleep in a state of ritual purity according to Sunnah. If the heart does not settle immediately, repeat for up to 7 days.",
      methodBn = "'ইশার নামাযের পর (অথবা যে কোনো বৈধ সময়ে) নতুন অজু করিয়া দুই রাকা'আত নামায খুব ভক্তির সাথে একাগ্রতার সহিত পড়িবে। তারপর নিম্নের দু'আটি অর্থের দিকে খেয়াল করিয়া আল্লাহ্ তা'আলাকে হাজির নাজির জানিয়া একাগ্রচিত্তে পড়িবে। অতঃপর পবিত্রতার সাথে সুন্নত তরীকা মুতাবিক ঘুমাইবে। আল্লাহ্র ফজলে মন কোনো একদিকে স্থির হইয়া যাইবে। যদি ১ম দিনে মন স্থির না হয়, তবে পরপর ৭দিন করিবে। ইন্‌শাআল্লাহ্ ভালোমন্দ বুঝা যাইবে। বুঝা না গেলে মন যে দিকে টানে, তাহা করিয়া যাইবে। ইন্‌শাআল্লাহ্ তাহাতে মঙ্গলই হইবে।",
      methodAr = "يتوضأ المسلم ويصلي ركعتين نافلة بخشوع وتضرع، وبعد الفراغ من الصلاة يحمد الله ويثني عليه ويصلي على النبي ﷺ ثم يقرأ دعاء الاستخارة مستحضراً حاجته وأمره عند قوله (هذا الأمر). ثم ينام على طهارة ويكررها حتى ٧ أيام حتى ينشرح صدره لما فيه الخير بإذن الله.",
      duaArabic = "اللَّهُمَّ إِنِّيْ أَسْتَخِيْرُكَ بِعِلْمِكَ ، وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ ، وَأَسْأَلُكَ مِنْ فَضْلِكَ الْعَظِيْمِ ، فَإِنَّكَ تَقْدِرُ وَلَا أَقْدِرُ ، وَتَعْلَمُ وَلَا أَعْلَمُ ، وَإِنَّكَ أَنْتَ عَلَّامُ الْغُيُوْبِ ، اللَّهُمَّ إِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الْأَمْرَ خَيْرٌ لِيْ فِيْ دِيْنِيْ وَمَعَاشِيْ وَعَاقِبَةِ أَمْرِيْ (عَاجِلِهِ وَآجِلِهِ) فَاقْدِرْهُ لِيْ وَيَسِّرْهُ لِيْ ثُمَّ بَارِكْ لِيْ فِيْهِ ، وَإِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الْأَمْرَ شَرٌّ لِيْ فِيْ دِيْنِيْ وَمَعَاشِيْ وَعَاقِبَةِ أَمْرِيْ (عَاجِلِهِ وَآجِلِهِ) فَاصْرِفْهُ عَنِّيْ وَاصْرِفْنِيْ عَنْهُ وَاقْدِرْ لِيَ الْخَيْرَ حَيْثُ كَانَ ثُمَّ ارْضِنِيْ بِهِ",
      duaPronunciationBn = "আল্লাহুম্মা ইন্নী আসতাখীরুকা বি'ইলমিকা, ওয়া আসতাক্বদিরুকা বিক্বুদরাতিকা, ওয়া আস'আলুকা মিন ফাদলিকাল 'আযীম, ফাইন্নাকা তাক্বদিরু ওয়ালা আক্বদির, ওয়া তা'লামু ওয়ালা আ'লাম, ওয়া আনতা 'আল্লামুল গুয়ূব। আল্লাহুম্মা ইন কুনতা তা'লামু আন্না হাযাল আমরা খাইরুল লী ফী দীনী ওয়া মা'আশী ওয়া 'আক্বিবাতি আমরী (ফা'আঝিলি আমরী ওয়া আজিলিহী), ফাক্বদুরহু লী ওয়া ইয়াসসিরহু লী সুম্মা বারিক লী ফীহ। ওয়া ইন কুনতা তা'লামু আন্না হাযাল আমরা শাররুল লী ফী দীনী ওয়া মা'আশী ওয়া 'আক্বিবাতি আমরী (ফা'আঝিলি আমরী ওয়া আজিলিহী), ফাসরিফহু 'আন্নী ওয়াসরিফনী 'আনহু, ওয়াক্বদুর লিয়াল খাইরা হাইসু কানা সুম্মারদিনী বিহ্।",
      duaPronunciationEn = "Allahumma inni astakhiruka bi'ilmika, wa astaqdiruka biqudratika, wa as'aluka min fadlikal-'azim, fa'innaka taqdiru wa la aqdir, wa ta'lamu wa la a'lam, wa Anta 'Allamul-ghuyub. Allahumma in kunta ta'lamu anna hadhal-amra khayrun li fi deeni wa ma'ashi wa 'aqibati amri (fa'ajili amri wa ajilihi), faqdurhu li wa yassirhu li thumma barik li feeh. Wa in kunta ta'lamu anna hadhal-amra sharrun li fi deeni wa ma'ashi wa 'aqibati amri (fa'ajili amri wa ajilihi), fasrifhu 'anni wasrifni 'anhu waqdur liyal-khayra haythu kana thummar-dini bih.",
      duaMeaningEn = "O Allah, I seek Your counsel through Your knowledge, and I seek strength through Your power, and I ask of Your great bounty. For You are capable and I am not, You know and I do not, and You are the Knower of the unseen. O Allah, if You know that this matter is good for me in my religion, my livelihood, and the outcome of my affairs, then decree it for me, make it easy for me, and bless me in it. And if You know that this matter is harmful for me in my religion, my livelihood, and the outcome of my affairs, then turn it away from me, and turn me away from it, and decree for me good wherever it may be, and then make me pleased with it.",
      duaMeaningBn = "হে আল্লাহ! আমি আপনার জ্ঞানের মাধ্যমে আপনার কাছে কল্যাণ প্রার্থনা করছি, আপনার কুদরতের মাধ্যমে শক্তি প্রার্থনা করছি এবং আপনার মহা অনুগ্রহ প্রার্থনা করছি। নিশ্চয় আপনি সক্ষম, আমি অক্ষম; আপনি জানেন, আমি জানি না; এবং আপনিই সমস্ত অদৃশ্য বিষয়ে সম্যক অবগত। হে আল্লাহ! যদি আপনি জানেন যে এই বিষয়টি আমার দ্বীন, আমার জীবিকা ও আমার পরিণতির জন্য উত্তম, তবে তা আমার জন্য নির্ধারণ করুন, তা আমার জন্য সহজ করে দিন এবং তাতে আমার জন্য বরকত দান করুন। আর যদি আপনি জানেন যে এই বিষয়টি আমার দ্বীন, আমার জীবিকা ও আমার পরিণতির জন্য ক্ষতিকর, তবে তা আমার থেকে ফিরিয়ে নিন এবং আমাকেও তা থেকে ফিরিয়ে রাখুন, আর আমার জন্য যেখানেই কল্যাণ রয়েছে তা নির্ধারণ করুন, অতঃপর তাতে আমাকে সন্তুষ্ট রাখুন।",
      duaMeaningAr = "اللهم إني أطلب منك الخيرة بعلمك، وأستعين بقدرتك على نيل مرادي، وأسألك من فضلك العظيم؛ فإنك تقدر ولا أقدر، وتعلم ولا أعلم، وأنت علام الغيوب. اللهم إن كنت تعلم أن هذا الأمر خير لي في ديني ودنياي وعاقبة أمري فاقدره لي ويسره لي ثم بارك لي فيه، وإن كنت تعلم أن هذا الأمر شر لي فاصرفه عني واصرفني عنه واقدر لي الخير حيث كان ثم ارضني به.",
      importantNoteEn = "Tip: When reciting the words 'Hadhal-Amr' (هَذَا الْأَمْرَ / this matter), keep your specific desire or decision clearly in mind.",
      importantNoteBn = "বিশেষ দ্রষ্টব্য: দু'আ পড়িবার সময় [هَذَا الْأَمْرَ / হাযাল আমরা / এই বিষয়টি] বলার সময় মনে মনে নির্দিষ্ট কাজের খেয়াল করিবে।",
      importantNoteAr = "ملاحظة: عند قول (هذا الأمر) يستحضر الداعي في قلبه حاجته أو يذكرها صراحة."
    ),

    // 2. Salatul Hajat (সালাতুল হাজাত)
    ImportantPrayerItem(
      id = "hajat",
      titleEn = "Salatul Hajat",
      titleBn = "সালাতুল হাজাত",
      titleAr = "صلاة الحاجة",
      subtitleEn = "Prayer for Relief in Need and Hardship",
      subtitleBn = "অভাব, সংকট ও প্রয়োজন পূরণের বিশেষ সালাত",
      subtitleAr = "الصلاة عند نزول الكرب أو قضاء الحاجات",
      rakahs = "2 Rak'ahs (Nafl)",
      rakahsBn = "২ রাকা'আত (নফল)",
      rakahsAr = "ركعتان نافلة",
      virtuesEn = "When facing any difficulty, adversity, worry, or legitimate need in worldly or religious affairs, offering 2 Rak'ahs followed by praise of Allah, Salawat upon the Prophet ﷺ, and the Dua of Need brings divine assistance and relief.",
      virtuesBn = "যখন কোনো অভাব-অনটন, বিপদাপদ বা কোনো আশংকা কিংবা প্রয়োজন দেখা দেয়, তখন অজু করিয়া মনোযোগের সহিত একাগ্রচিত্তে দুই রাক'আত নামায পড়িয়া, ইস্তেগফার করিয়া, আল্লাহ্ তা'আলার প্রশংসা করিয়া দুরুদ শরীফ পাঠ করিয়া কায়মনোবাক্যে আল্লাহ্ তা'আলার কাছে দু'আ করিবে। ইন্‌শাআল্লাহ্ মনোবাঞ্ছা পূর্ণ হইবে।",
      virtuesAr = "من كانت له حاجة إلى الله تعالى أو إلى أحد من بني آدم فليتوضأ وليحسن الوضوء ثم ليصل ركعتين ثم يثن على الله وليصل على النبي ﷺ ثم يدعو بهذا الدعاء المبارك لتفريج همه وقضاء حاجته.",
      methodEn = "1. Perform a thorough, proper Wudu (ablution).\n2. Offer 2 Rak'ahs of voluntary (Nafl) prayer with sincerity and concentration.\n3. After the final Salam, glorify and praise Allah (Tahmid & Tasbih) and make Istighfar (seek forgiveness).\n4. Send Salawat (Durud) upon the Prophet Muhammad ﷺ.\n5. Recite the authentic Dua of Salatul Hajat and pour your heart out to Allah for your specific need.",
      methodBn = "১. উত্তমরূপে অজু সম্পন্ন করুন।\n২. অত্যন্ত বিনম্রতা ও একাগ্রতার সাথে দুই রাক'আত নফল নামায আদায় করুন।\n৩. নামায শেষে আল্লাহ্ তা'আলার হামদ ও প্রশংসা (সুবহানাল্লাহ, আলহামদুলিল্লাহ) এবং তাওবা-ইস্তেগফার করুন।\n৪. রাসূলুল্লাহ (সা.)-এর উপর দুরুদ শরীফ পাঠ করুন।\n৫. অতঃপর সালাতুল হাজাতের বিশেষ দু'আ পাঠ করে কায়মনোবাক্যে নিজের মনের আকুতি ও প্রয়োজনের কথা আল্লাহ্‌র দরবারে পেশ করুন।",
      methodAr = "١. إسباغ الوضوء على أكمل وجه.\n٢. صلاة ركعتين نافلة بنية قضاء الحاجة بخشوع وتذلل.\n٣. الثناء على الله وحمده وتسبيحه والاستغفار بعد السلام.\n٤. الصلاة والسلام على رسول الله ﷺ.\n٥. قراءة دعاء الحاجة والتضرع بالمسألة والإلحاح في الدعاء.",
      duaArabic = "لَا إِلَهَ إِلَّا اللَّهُ الْحَلِيمُ الْكَرِيمُ ، سُبْحَانَ اللَّهِ رَبِّ الْعَرْشِ الْعَظِيمِ ، الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ، أَسْأَلُكَ مُوجِبَاتِ رَحْمَتِكَ ، وَعَزَائِمَ مَغْفِرَتِكَ ، وَالْغَنِيمَةَ مِنْ كُلِّ بِرٍّ ، وَالسَّلَامَةَ مِنْ كُلِّ إِثْمٍ ، لَا تَدَعْ لِي ذَنْبًا إِلَّا غَفَرْتَهُ ، وَلَا هَمًّا إِلَّا فَرَّجْتَهُ ، وَلَا حَاجَةً هِيَ لَكَ رِضًا إِلَّا قَضَيْتَهَا يَا أَرْحَمَ الرَّاحِمِينَ",
      duaPronunciationBn = "লা ইলাহা ইল্লাল্লাহুল হালীমুল কারীম, সুবহানাল্লাহি রাব্বিল 'আরশিল 'আযীম, আলহামদুলিল্লাহি রাব্বিল 'আলামীন। আস'আলুকা মূজিবাতি রাহমাতিকা, ওয়া 'আযা-ইমা মাগফিরাতিকা, ওয়াল গানিমাতা মিন কুল্লি বিররিন, ওয়াস-সালামাতা মিন কুল্লি ইসমিন। লা তাদা' লী যাম্বান ইল্লা গাফারতাহু, ওয়ালা হাম্মান ইল্লা ফাররাজতাহু, ওয়ালা হাজাতান হিয়া লাকা রিদান ইল্লা ক্বাদাইতাহা ইয়া আরহামার রাহিমীন।",
      duaPronunciationEn = "La ilaha illallahul-Haleemul-Kareem, Subhanallahi Rabbil-'Arshil-'Azeem, Al-hamdu lillahi Rabbil-'Alameen. As'aluka moojibati rahmatika, wa 'aza'ima maghfiratika, wal-ghaneemata min kulli birrin, was-salamata min kulli ithmin. La tada' li dhanban illa ghafartahu, wa la hamman illa farrajtahu, wa la hajatan hiya laka ridan illa qadaytaha ya Arhamar-Rahimeen.",
      duaMeaningEn = "There is no deity except Allah, the Forbearing, the Generous. Glory be to Allah, Lord of the Magnificent Throne. Praise be to Allah, Lord of all the worlds. I ask You for that which necessitates Your mercy, the resolves of Your forgiveness, the spoils of every righteous deed, and safety from every sin. Do not leave for me any sin but that You forgive it, nor any anxiety/distress but that You relieve it, nor any need that is pleasing to You but that You fulfill it, O Most Merciful of those who show mercy!",
      duaMeaningBn = "আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই, তিনি পরম ধৈর্যশীল ও মহানুভব। মহান আরশের প্রতিপালক আল্লাহ অতি পবিত্র। সমস্ত প্রশংসা বিশ্বজগতের প্রতিপালক আল্লাহর জন্য। হে আল্লাহ! আমি আপনার রহমত আকর্ষণকারী বিষয়সমূহ, আপনার ক্ষমা লাভের সুনিশ্চিত মাধ্যমসমূহ, প্রত্যেক নেক কাজের অংশ এবং প্রত্যেক গুনাহ থেকে নিরাপত্তা কামনা করছি। আমার কোনো পাপ আপনি ক্ষমা ছাড়া রাখবেন না, কোনো দুশ্চিন্তা আপনি দূর করা ছাড়া রাখবেন না, এবং এমন কোনো প্রয়োজন যাতে আপনার সন্তুষ্টি রয়েছে তা পূরণ করা ছাড়া রাখবেন না, হে পরম দয়ালু!",
      duaMeaningAr = "لا إله إلا الله الحليم الكريم، تنزه الله رب العرش العظيم عن كل نقص، والحمد لله رب العالمين. أسألك أسباب رحمتك وموجبات مغفرتك، والسلامة من كل معصية، والغنيمة من كل طاعة، لا تترك لي ذنباً إلا غفرته، ولا هماً إلا فرجته، ولا حاجة لك فيها رضا إلا قضيتها يا أرحم الراحمين.",
      importantNoteEn = "Source: Hadith recorded in Sunan al-Tirmidhi (479) & Sunan Ibn Majah (1384).",
      importantNoteBn = "হাদীস সূত্র: সুনানে তিরমিযী (৪৭৯), সুনানে ইবনে মাজাহ (১৩৮৪)।",
      importantNoteAr = "الحديث الشريف: رواه الترمذي (٤٧٩) وابن ماجه (١٣٨٤)."
    ),

    // 3. Salatut Tasbih (সালাতুত তাসবীহ)
    ImportantPrayerItem(
      id = "tasbih",
      titleEn = "Salatut Tasbih",
      titleBn = "সালাতুত তাসবীহ",
      titleAr = "صلاة التسابيح",
      subtitleEn = "The Prayer of Glorification (300 Tasbihs)",
      subtitleBn = "জীবনের সকল গুনাহ মাফের শ্রেষ্ঠ সালাত (৩০০ তাসবীহ)",
      subtitleAr = "صلاة التكفير عن الذنوب (٣٠٠ تسبيحة)",
      rakahs = "4 Rak'ahs + 300 Tasbihs",
      rakahsBn = "৪ রাকা'আত + ৩০০ তাসবীহ",
      rakahsAr = "٤ ركعات + ٣٠٠ تسبيحة",
      virtuesEn = "The Prophet Muhammad ﷺ taught this prayer to his beloved uncle Hazrat Abbas (R.A.) saying: 'O uncle! If you can pray it daily, do so; if not, once a week; if not, once a month; if not, once a year; if not, then at least once in your lifetime.' Through it, Allah forgives all past and future sins, minor and major, intentional and accidental.",
      virtuesBn = "এই নামায রাসূলে আকরাম (সা.) স্বীয় প্রাণপ্রিয় চাচা হযরত আব্বাস (রা.)-কে অত্যন্ত গুরুত্ব সহকারে শিক্ষা দিয়াছিলেন এবং তাঁহাকে বলিয়াছেন— 'চাচা! যদি পারেন, দৈনিক একবার; তাহা না হয়, সপ্তাহে একবার; তাহা না হয়, মাসে একবার; তাহা না হয়, বৎসরে ১বার; তাহা না হয়, জীবনে ১বার হইলেও পড়িবেন।' ইহা দ্বারা সারা জীবনের সমস্ত পূর্বের ও পরের, ছোট ও বড়, জানা ও অজানা গুনাহ মাফ হইয়া যায়।",
      virtuesAr = "علّمها النبي ﷺ لعمه العباس بن عبد المطلب رضي الله عنه وقال له: (إن استطعت أن تصليها في كل يوم مرة فافعل، فإن لم تفعل ففي كل جمعة مرة، فإن لم تفعل ففي كل شهر مرة، فإن لم تفعل ففي كل سنة مرة، فإن لم تفعل ففي عمرك مرة). وتغفر بها الذنوب والخطايا كلها أولها وآخرها، قديمها وحديثها، خطأها وعمدها، صغيرها وكبيرها، سرها وعلانيتها.",
      methodEn = "4 Rak'ahs are offered. In each Rak'ah, the specific Tasbih is recited 75 times in various postures (Total 300 times across 4 Rak'ahs):\n\nTasbih Formula:\n'Subhanallahi wal-hamdulillahi wa la ilaha illallahu wallahu akbar'\n\nStep Breakdown per Rak'ah:\n1. After Takbiratul Ihram & Sana (Subhanaka...): Recite 15 times.\n2. After Surah Al-Fatiha & another Surah: Recite 10 times before Ruku.\n3. In Ruku (after Subhana Rabbiyal Azeem): Recite 10 times.\n4. Standing upright from Ruku (Qawmah): Recite 10 times.\n5. In the 1st Sujood (after Subhana Rabbiyal A'la): Recite 10 times.\n6. Sitting between the two Sujoods (Jalsah): Recite 10 times.\n7. In the 2nd Sujood (after Subhana Rabbiyal A'la): Recite 10 times.\n[Total for 1st Rak'ah = 75 times]\n\nIn the 2nd, 3rd, and 4th Rak'ahs: Stand up, recite 15 times before Surah Al-Fatiha, and repeat the exact sequence (75 times per Rak'ah = 300 total). Conclude with Tashahhud, Salawat, and Salam.",
      methodBn = "৪ রাক'আত নামায যে কোনো সূরা দিয়ে পড়া যায়। প্রতি রাক'আতে ৭৫ বার করে ৪ রাক'আতে মোট ৩০০ বার উক্ত তাসবীহ পড়তে হয়।\n\nতাসবীহের মূল বাক্য:\n'সুবহানাল্লাহি ওয়ালহামদু লিল্লাহি ওয়া লা ইলাহা ইল্লাল্লাহু ওয়াল্লাহু আকবার'\n\nরাকা'আত অনুযায়ী নিয়ম:\n১. তাকবীরে তাহরীমা বলে ছানা (সুবহানাকা...) পড়ার পর তাসবীহটি ১৫ বার পড়ুন।\n২. সূরা ফাতিহা ও অন্য সূরা পড়ার পর রুকূতে যাওয়ার পূর্বে ১০ বার পড়ুন।\n৩. রুকূতে গিয়ে রুকূর তাসবীহ (সুবহানা রাব্বিয়াল আযীম) পড়ার পর ১০ বার পড়ুন।\n৪. রুকূ থেকে সোজা হয়ে দাঁড়িয়ে (সামিআল্লাহু লিমান হামীদাহ, রাব্বানা লাকাল হামদ) ১০ বার পড়ুন।\n৫. ১ম সিজদায় গিয়ে সিজদার তাসবীহের পর ১০ বার পড়ুন।\n৬. দুই সিজদার মাঝে সোজা হয়ে বসে ১০ বার পড়ুন।\n৭. ২য় সিজদায় গিয়ে সিজদার তাসবীহের পর ১০ বার পড়ুন।\n(১ম রাক'আতে মোট ৭৫ বার সম্পন্ন হলো)\n\nপরবর্তী রাকা'আতসমূহে:\n২য়, ৩য় ও ৪র্থ রাকা'আতে দাঁড়িয়ে সূরা ফাতিহার পূর্বে ১৫ বার পড়ুন, অতঃপর বাকি নিয়ম পালন করুন (প্রতি রাকা'আতে ৭৫ বার)। সর্বমোট ৩০০ বার সমাপ্ত করে তাশাহহুদ, দুরুদ ও দু'আয়ে মাসূরা পড়ে সালাম ফিরিয়ে নামায শেষ করুন।",
      methodAr = "تصلى ٤ ركعات يسبح فيها العبد ٣٠٠ تسبيحة (٧٥ تسبيحة في كل ركعة):\nصيغة التسبيح:\n(سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ)\n\nتوزيع التسبيحات في كل ركعة:\n١. بعد دعاء الاستفتاح وقبل الفاتحة: ١٥ مرة.\n٢. بعد قراءة الفاتحة والسورة: ١٠ مرات.\n٣. في الركوع بعد تسبيح الركوع: ١٠ مرات.\n٤. بعد الرفع والاعتدال من الركوع: ١٠ مرات.\n٥. في السجدة الأولى بعد تسبيح السجود: ١٠ مرات.\n٦. في الجلوس بين السجدتين: ١٠ مرات.\n٧. في السجدة الثانية بعد تسبيح السجود: ١٠ مرات.\n(المجموع ٧٥ مرة في كل ركعة، وتكرر في الأربع ركعات ليكتمل ٣٠٠ تسبيحة).",
      duaArabic = "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ",
      duaPronunciationBn = "সুবহানাল্লাহি ওয়ালহামদু লিল্লাহি ওয়া লা ইলাহা ইল্লাল্লাহু ওয়াল্লাহু আকবার",
      duaPronunciationEn = "Subhanallahi wal-hamdulillahi wa la ilaha illallahu wallahu akbar",
      duaMeaningEn = "Glory be to Allah, all praise is due to Allah, there is no deity worthy of worship except Allah, and Allah is the Greatest.",
      duaMeaningBn = "আল্লাহ অতি পবিত্র, সমস্ত প্রশংসা আল্লাহর জন্য, আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই এবং আল্লাহ সর্বশ্রেষ্ঠ।",
      duaMeaningAr = "تسبيح وتحمিদ وتوحيد وتكبير لله العلي العظيم.",
      steps = listOf(
        PrayerStep(
          stepNumber = 1,
          positionTitleEn = "1. After Sana (Before Fatiha)",
          positionTitleBn = "১. ছানা পড়ার পর (ফাতিহার পূর্বে)",
          positionTitleAr = "١. بعد الاستفتاح وقبل الفاتحة",
          count = 15,
          instructionEn = "Recite the Tasbih 15 times after 'Subhanakallahumma...'",
          instructionBn = "তাকবীরে তাহরীমার পর ছানা পড়ে তাসবীহটি ১৫ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٥ مرة بعد دعاء الاستفتاح"
        ),
        PrayerStep(
          stepNumber = 2,
          positionTitleEn = "2. After Surah (Before Ruku)",
          positionTitleBn = "২. সূরা কিরাআতের পর (রুকূর পূর্বে)",
          positionTitleAr = "٢. بعد الفاتحة والسورة",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times before bowing into Ruku",
          instructionBn = "সূরা ফাতিহা ও অন্য সূরা পড়ার পর দাঁড়িয়ে ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات قبل الركوع"
        ),
        PrayerStep(
          stepNumber = 3,
          positionTitleEn = "3. In Ruku (Bowing)",
          positionTitleBn = "৩. রুকূতে (মাথা ঝুঁকিয়ে)",
          positionTitleAr = "٣. في الركوع",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times after 'Subhana Rabbiyal Azeem'",
          instructionBn = "রুকূর তাসবীহ পাঠ করার পর ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات بعد تسبيح الركوع"
        ),
        PrayerStep(
          stepNumber = 4,
          positionTitleEn = "4. Standing after Ruku (Qawmah)",
          positionTitleBn = "৪. রুকূ থেকে সোজা হয়ে দাঁড়িয়ে",
          positionTitleAr = "٤. بعد الرفع من الركوع",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times standing upright after 'Rabbana lakal hamd'",
          instructionBn = "সোজা হয়ে দাঁড়িয়ে 'রাব্বানা লাকাল হামদ' বলার পর ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات في الاعتدال"
        ),
        PrayerStep(
          stepNumber = 5,
          positionTitleEn = "5. In 1st Sujood (Prostration)",
          positionTitleBn = "৫. ১ম সিজদায়",
          positionTitleAr = "٥. في السجدة الأولى",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times after 'Subhana Rabbiyal A'la'",
          instructionBn = "১ম সিজদায় সিজদার তাসবীহ পাঠ করার পর ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات بعد تسبيح السجود"
        ),
        PrayerStep(
          stepNumber = 6,
          positionTitleEn = "6. Sitting Between 2 Sujoods (Jalsah)",
          positionTitleBn = "৬. দুই সিজদার মাঝে বসে",
          positionTitleAr = "٦. في الجلوس بين السجدتين",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times sitting comfortably upright",
          instructionBn = "দুই সিজদার মাঝখানে সোজা হয়ে বসে ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات أثناء الجلوس"
        ),
        PrayerStep(
          stepNumber = 7,
          positionTitleEn = "7. In 2nd Sujood (Prostration)",
          positionTitleBn = "৭. ২য় সিজদায়",
          positionTitleAr = "٧. في السجدة الثانية",
          count = 10,
          instructionEn = "Recite the Tasbih 10 times after 'Subhana Rabbiyal A'la'",
          instructionBn = "২য় সিজদায় সিজদার তাসবীহ পাঠ করার পর ১০ বার পড়ুন",
          instructionAr = "اقرأ التسبيح ١٠ مرات بعد تسبيح السجود"
        )
      ),
      importantNoteEn = "Total per Rak'ah: 15 + 10 + 10 + 10 + 10 + 10 + 10 = 75 times. In 4 Rak'ahs, total = 300 times.",
      importantNoteBn = "হিসাব: প্রতি রাকা'আতে ১৫ + ১০ + ১০ + ১০ + ১০ + ১০ + ১০ = ৭৫ বার। ৪ রাকা'আতে মোট = ৩০০ বার সম্পন্ন হবে।",
      importantNoteAr = "المجموع في الركعة: ١٥ + ١٠ + ١٠ + ١٠ + ١٠ + ١٠ + ١٠ = ٧٥ تسبيحة. وفي الأربع ركعات = ٣٠٠ تسبيحة."
    )
  )
}
