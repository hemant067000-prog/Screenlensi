package com.example.domain

data class SceneDialogueLine(
    val topBannerText: String,
    val midContentText: String,
    val bottomSubtitleText: String
)

data class DemoSceneSpec(
    val index: Int,
    val id: String,
    val languageCode: String,
    val languageName: String,
    val categoryTitle: String,
    val accentHex: Long,
    val isDarkScene: Boolean,
    val frames: List<SceneDialogueLine>
)

object DemoScenesCatalog {
    val scenes: List<DemoSceneSpec> = listOf(
        DemoSceneSpec(
            index = 0,
            id = "ja_anime",
            languageCode = "JA",
            languageName = "Japanese",
            categoryTitle = "Japanese Anime Subtitles",
            accentHex = 0xFF22D3EE,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "第12話：蒼き閃光と最終防衛線",
                    midContentText = "シンクロ率 98.4% • 領域展開システム起動",
                    bottomSubtitleText = "諦めないで！夜明け前に必ず君を見つけ出すから！"
                ),
                SceneDialogueLine(
                    topBannerText = "第12話：蒼き閃光と最終防衛線",
                    midContentText = "警告：高エネルギー反応を前方に検知",
                    bottomSubtitleText = "この剣に宿る全ての力を今ここで解放する！"
                ),
                SceneDialogueLine(
                    topBannerText = "第12話：蒼き閃光と最終防衛線",
                    midContentText = "ターゲット捕捉 • 限界突破モード発動",
                    bottomSubtitleText = "私たちの絆はどんな闇にも決して負けない！"
                )
            )
        ),
        DemoSceneSpec(
            index = 1,
            id = "ko_rpg",
            languageCode = "KO",
            languageName = "Korean",
            categoryTitle = "Korean RPG Dialogue",
            accentHex = 0xFF10B981,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "[메인 퀘스트] 마왕성 결전 • 심연의 성채",
                    midContentText = "보스 체력 42,800 / 100,000 • 치명타 확률 증가",
                    bottomSubtitleText = "성문을 열어라! 어둠의 군주가 깨어나기 전에 봉인해야 한다!"
                ),
                SceneDialogueLine(
                    topBannerText = "[메인 퀘스트] 고대 유적의 수호자",
                    midContentText = "파티 버프 활성화 • 마나 회복 속도 2배",
                    bottomSubtitleText = "전설의 성검을 들면 숨겨진 차원의 문이 열릴 것이다."
                ),
                SceneDialogueLine(
                    topBannerText = "[긴급 알림] 레이드 보스 광폭화 시작",
                    midContentText = "방어막 전개 완료 • 모든 대원 집결",
                    bottomSubtitleText = "지금이다! 모든 궁극기를 집중해서 일격에 끝내자!"
                )
            )
        ),
        DemoSceneSpec(
            index = 2,
            id = "zh_ui",
            languageCode = "ZH",
            languageName = "Chinese",
            categoryTitle = "Chinese Cyber OS UI",
            accentHex = 0xFF38BDF8,
            isDarkScene = false, // Light polarity test scene to showcase auto-inversion!
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "星际导航控制台 • 量子核心已就绪",
                    midContentText = "系统状态：离线神经网络全速运行中",
                    bottomSubtitleText = "正在扫描全屏文本区域，实时本地翻译已启动。"
                ),
                SceneDialogueLine(
                    topBannerText = "安全协议 09 • 零云端数据传输",
                    midContentText = "内存占用 14MB • 帧率稳定 60Hz",
                    bottomSubtitleText = "所有机密图像均在本地设备完成识别，绝不上传网络。"
                ),
                SceneDialogueLine(
                    topBannerText = "超算矩阵终端 • 空间差分引擎",
                    midContentText = "检测到 48 个网格中的 12 个活跃变化块",
                    bottomSubtitleText = "能量护盾充能完毕，准备跃迁至下一星系扇区。"
                )
            )
        ),
        DemoSceneSpec(
            index = 3,
            id = "hi_news",
            languageCode = "HI",
            languageName = "Hindi",
            categoryTitle = "Hindi News Ticker",
            accentHex = 0xFFF59E0B,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "ब्रेकिंग न्यूज़ • सुपरफास्ट बुलेटिन लाइव",
                    midContentText = "नई दिल्ली: अंतरिक्ष मिशन ने सफलतापूर्वक कक्षा में प्रवेश किया",
                    bottomSubtitleText = "देशभर में डिजिटल तकनीक और ऑफलाइन अनुवाद का तेजी से विस्तार हो रहा है।"
                ),
                SceneDialogueLine(
                    topBannerText = "विशेष रिपोर्ट • आर्थिक और विज्ञान अपडेट",
                    midContentText = "मौसम विभाग: अगले चौबीस घंटों में भारी बारिश की संभावना",
                    bottomSubtitleText = "वैज्ञानिकों ने कम ऊर्जा में चलने वाले नए स्मार्ट प्रोसेसर का अनावरण किया।"
                )
            )
        ),
        DemoSceneSpec(
            index = 4,
            id = "mr_broadcast",
            languageCode = "MR",
            languageName = "Marathi",
            categoryTitle = "Marathi Live Broadcast",
            accentHex = 0xFFFB923C,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "महाराष्ट्र वृत्तवाहिनी • थेट प्रक्षेपण",
                    midContentText = "मुंबई आणि पुणे मेट्रो प्रकल्पाचे काम वेगाने पूर्णत्वाकडे",
                    bottomSubtitleText = "तंत्रज्ञान क्षेत्रात स्थानिक भाषांमधील तात्काळ भाषांतरामुळे मोठी क्रांती घडत आहे."
                ),
                SceneDialogueLine(
                    topBannerText = "विशेष बातमीपत्र • राज्यभरातील घडामोडी",
                    midContentText = "सह्याद्रीच्या रांगांमध्ये पर्यटन हंगामाला उत्साहात सुरुवात",
                    bottomSubtitleText = "शेतकऱ्यांसाठी नवीन सौर ऊर्जा योजना आजपासून राज्यभर लागू करण्यात आली आहे."
                )
            )
        ),
        DemoSceneSpec(
            index = 5,
            id = "es_stream",
            languageCode = "ES",
            languageName = "Spanish",
            categoryTitle = "Spanish Live Stream",
            accentHex = 0xFFF43F5E,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "EN VIVO • FINAL MUNDIAL DE CIBERDEPORTES",
                    midContentText = "Espectadores: 248.500 • Ronda 24 • Tiempo restante 00:42",
                    bottomSubtitleText = "¡Increíble jugada del equipo campeón para asegurar la victoria en el último segundo!"
                ),
                SceneDialogueLine(
                    topBannerText = "TRANSMISIÓN DIRECTA • ANÁLISIS TÁCTICO",
                    midContentText = "Velocidad de reacción: 140ms • Precisión del 94%",
                    bottomSubtitleText = "La defensa resistió el ataque final y ahora avanzan directo hacia el trofeo."
                )
            )
        ),
        DemoSceneSpec(
            index = 6,
            id = "fr_cinema",
            languageCode = "FR",
            languageName = "French",
            categoryTitle = "French Cinema",
            accentHex = 0xFFA855F7,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "FESTIVAL DU FILM • SÉLECTION OFFICIELLE",
                    midContentText = "Acte III : Les Lumières de Minuit sur la Seine",
                    bottomSubtitleText = "Les souvenirs que nous gardons en silence finissent toujours par éclairer notre chemin."
                ),
                SceneDialogueLine(
                    topBannerText = "CINÉMA INDÉPENDANT • VERSION ORIGINALE",
                    midContentText = "Paris, Automne 1968 • Archives Restaurées",
                    bottomSubtitleText = "Il suffit parfois d'un seul regard pour comprendre ce que les mots ne disent pas."
                )
            )
        ),
        DemoSceneSpec(
            index = 7,
            id = "de_keynote",
            languageCode = "DE",
            languageName = "German",
            categoryTitle = "German Engineering Keynote",
            accentHex = 0xFF34D399,
            isDarkScene = false, // Light polarity presentation slide
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "TECHNOLOGIE GIPFEL • AUTONOME SYSTEME",
                    midContentText = "Latenzzeit unter 2 Millisekunden • 100% Lokale Verarbeitung",
                    bottomSubtitleText = "Unsere neue optische Architektur übersetzt Bildschirminhalte vollkommen ohne Internetverbindung."
                ),
                SceneDialogueLine(
                    topBannerText = "INDUSTRIE 5.0 • ECHTZEIT TELEMETRIE",
                    midContentText = "Energieeffizienz +45% • Maximale Datensicherheit",
                    bottomSubtitleText = "Jedes einzelne Bildsegment wird direkt im lokalen Speicher analysiert und sofort dargestellt."
                )
            )
        ),
        DemoSceneSpec(
            index = 8,
            id = "ru_doc",
            languageCode = "RU",
            languageName = "Russian",
            categoryTitle = "Russian Space Documentary",
            accentHex = 0xFF60A5FA,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "ХРОНИКА КОСМОСА • ОРБИТАЛЬНАЯ СТАНЦИЯ",
                    midContentText = "Высота орбиты: 408 км • Скорость: 7,66 км/с",
                    bottomSubtitleText = "Сквозь иллюминатор открывается бесконечный вид на голубую планету и полярное сияние."
                ),
                SceneDialogueLine(
                    topBannerText = "АРХИВ ЭКСПЕДИЦИИ • СЕКТОР ДАЛЬНЕЙ СВЯЗИ",
                    midContentText = "Телеметрия стабильна • Все бортовые системы в норме",
                    bottomSubtitleText = "Экипаж успешно завершил стыковку исследовательского модуля в автоматическом режиме."
                )
            )
        ),
        DemoSceneSpec(
            index = 9,
            id = "ar_news",
            languageCode = "AR",
            languageName = "Arabic",
            categoryTitle = "Arabic Global News",
            accentHex = 0xFF2DD4BF,
            isDarkScene = true,
            frames = listOf(
                SceneDialogueLine(
                    topBannerText = "نشرة الأخبار العالمية • تغطية خاصة ومباشرة",
                    midContentText = "قمة الابتكار التقني: إطلاق الجيل الجديد من المعالجات المحلية",
                    bottomSubtitleText = "تعتمد الأنظمة الحديثة على الترجمة الفورية بدون اتصال بالإنترنت لحماية خصوصية المستخدم."
                ),
                SceneDialogueLine(
                    topBannerText = "التقرير الاقتصادي • أسواق الطاقة والتكنولوجيا",
                    midContentText = "نمو قياسي في قطاع البرمجيات الذكية بنسبة ٣٨ بالمئة",
                    bottomSubtitleText = "أكد الخبراء أن المعالجة الفورية على الجهاز توفر سرعة فائقة واستقرارا كاملا."
                )
            )
        )
    )
}
