package com.inventory.app.domain.model

data class RegionalCuisine(
    val name: String,
    val country: String,
    val continent: String,
    val searchTerms: List<String> = emptyList()
)

object CuisineData {

    val all: List<RegionalCuisine> by lazy { buildList() }

    val continents = listOf("Africa", "Americas", "Asia", "Europe", "Middle East", "Oceania")

    fun countriesForContinent(continent: String): List<String> =
        all.filter { it.continent == continent }.map { it.country }.distinct().sorted()

    fun cuisinesForCountry(country: String): List<RegionalCuisine> =
        all.filter { it.country == country }

    fun search(query: String): List<RegionalCuisine> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase().trim()
        return all.filter { cuisine ->
            cuisine.name.lowercase().contains(q) ||
                cuisine.country.lowercase().contains(q) ||
                cuisine.searchTerms.any { it.contains(q) }
        }
    }

    val popular: List<RegionalCuisine> by lazy {
        listOf(
            find("Indian"), find("Italian"), find("Chinese"),
            find("Mexican"), find("Thai"), find("Japanese"),
            find("Mediterranean"), find("Korean"), find("French"),
            find("American"), find("Turkish"), find("Vietnamese")
        ).filterNotNull()
    }

    private fun find(name: String): RegionalCuisine? =
        all.firstOrNull { it.name == name }

    private fun buildList(): List<RegionalCuisine> = listOf(
        // ══════════════════════════════════════════════════════════════
        // AFRICA
        // ══════════════════════════════════════════════════════════════
        rc("Ethiopian", "Ethiopia", "Africa", "injera", "wot", "doro"),
        rc("Eritrean", "Eritrea", "Africa", "zigni", "tsebhi"),
        rc("Nigerian", "Nigeria", "Africa", "jollof", "suya", "egusi"),
        rc("Yoruba", "Nigeria", "Africa", "amala", "ewedu"),
        rc("Igbo", "Nigeria", "Africa", "ofe", "abacha"),
        rc("Ghanaian", "Ghana", "Africa", "banku", "fufu", "kenkey"),
        rc("Senegalese", "Senegal", "Africa", "thieboudienne", "yassa"),
        rc("Moroccan", "Morocco", "Africa", "tagine", "couscous", "pastilla"),
        rc("Tunisian", "Tunisia", "Africa", "brik", "shakshuka", "harissa"),
        rc("Algerian", "Algeria", "Africa", "couscous", "chakchouka"),
        rc("Egyptian", "Egypt", "Africa", "koshari", "ful medames", "molokhia"),
        rc("South African", "South Africa", "Africa", "braai", "bobotie", "biltong"),
        rc("Cape Malay", "South Africa", "Africa", "bredie", "sosatie"),
        rc("Kenyan", "Kenya", "Africa", "nyama choma", "ugali", "sukuma"),
        rc("Tanzanian", "Tanzania", "Africa", "pilau", "chipsi mayai"),
        rc("Somali", "Somalia", "Africa", "bariis", "suqaar", "canjeero"),
        rc("Congolese", "DR Congo", "Africa", "moambe", "pondu"),
        rc("Cameroon", "Cameroon", "Africa", "ndole", "eru"),
        rc("Ivorian", "Ivory Coast", "Africa", "attieke", "garba"),
        rc("Mozambican", "Mozambique", "Africa", "peri peri", "matapa"),
        rc("Malagasy", "Madagascar", "Africa", "romazava", "ravitoto"),
        rc("Sudanese", "Sudan", "Africa", "ful", "kisra", "mullah"),
        rc("Ugandan", "Uganda", "Africa", "luwombo", "rolex"),
        rc("Rwandan", "Rwanda", "Africa", "isombe", "brochettes"),
        rc("Zimbabwean", "Zimbabwe", "Africa", "sadza", "muriwo"),

        // ══════════════════════════════════════════════════════════════
        // AMERICAS
        // ══════════════════════════════════════════════════════════════
        // North America
        rc("American", "United States", "Americas", "burger", "bbq", "soul food"),
        rc("Southern US", "United States", "Americas", "fried chicken", "grits", "gumbo"),
        rc("Cajun", "United States", "Americas", "jambalaya", "crawfish", "louisiana"),
        rc("Creole", "United States", "Americas", "etouffee", "new orleans", "roux"),
        rc("Tex-Mex", "United States", "Americas", "fajita", "nachos", "queso"),
        rc("New England", "United States", "Americas", "clam chowder", "lobster roll"),
        rc("Hawaiian", "United States", "Americas", "poke", "loco moco", "spam musubi"),
        rc("Soul Food", "United States", "Americas", "collard greens", "cornbread"),
        rc("Canadian", "Canada", "Americas", "poutine", "maple"),
        rc("Quebecois", "Canada", "Americas", "tourtiere", "sugar pie"),
        // Mexico & Central America
        rc("Mexican", "Mexico", "Americas", "taco", "enchilada", "mole"),
        rc("Oaxacan", "Mexico", "Americas", "mole negro", "tlayuda", "mezcal"),
        rc("Yucatecan", "Mexico", "Americas", "cochinita pibil", "papadzules"),
        rc("Baja", "Mexico", "Americas", "fish taco", "baja california"),
        rc("Mexico City", "Mexico", "Americas", "tacos al pastor", "chilaquiles"),
        rc("Guatemalan", "Guatemala", "Americas", "pepian", "tamales"),
        rc("Salvadoran", "El Salvador", "Americas", "pupusa", "curtido"),
        rc("Honduran", "Honduras", "Americas", "baleada", "sopa de caracol"),
        rc("Costa Rican", "Costa Rica", "Americas", "gallo pinto", "casado"),
        rc("Panamanian", "Panama", "Americas", "sancocho", "arroz con pollo"),
        // South America
        rc("Brazilian", "Brazil", "Americas", "feijoada", "churrasco", "acai"),
        rc("Bahian", "Brazil", "Americas", "moqueca", "acaraje", "dende"),
        rc("Mineiro", "Brazil", "Americas", "pao de queijo", "feijao tropeiro"),
        rc("Argentine", "Argentina", "Americas", "asado", "empanada", "chimichurri"),
        rc("Peruvian", "Peru", "Americas", "ceviche", "lomo saltado", "aji"),
        rc("Nikkei", "Peru", "Americas", "tiradito", "japanese-peruvian"),
        rc("Colombian", "Colombia", "Americas", "arepa", "bandeja paisa", "ajiaco"),
        rc("Venezuelan", "Venezuela", "Americas", "arepa", "pabellon", "cachapa"),
        rc("Chilean", "Chile", "Americas", "pastel de choclo", "empanada", "curanto"),
        rc("Ecuadorian", "Ecuador", "Americas", "encebollado", "llapingacho"),
        rc("Bolivian", "Bolivia", "Americas", "salteña", "pique macho"),
        rc("Paraguayan", "Paraguay", "Americas", "sopa paraguaya", "chipa"),
        rc("Uruguayan", "Uruguay", "Americas", "chivito", "asado"),
        // Caribbean
        rc("Jamaican", "Jamaica", "Americas", "jerk", "ackee", "saltfish"),
        rc("Cuban", "Cuba", "Americas", "ropa vieja", "cubano sandwich"),
        rc("Puerto Rican", "Puerto Rico", "Americas", "mofongo", "pernil"),
        rc("Dominican", "Dominican Republic", "Americas", "mangu", "la bandera"),
        rc("Haitian", "Haiti", "Americas", "griot", "diri ak djon djon"),
        rc("Trinidadian", "Trinidad", "Americas", "doubles", "roti", "callaloo"),

        // ══════════════════════════════════════════════════════════════
        // ASIA
        // ══════════════════════════════════════════════════════════════
        // South Asia
        rc("Indian", "India", "Asia", "curry", "dal", "masala"),
        rc("Punjabi", "India", "Asia", "butter chicken", "sarson da saag", "tandoori"),
        rc("Gujarati", "India", "Asia", "dhokla", "thepla", "undhiyu"),
        rc("Tamil", "India", "Asia", "dosa", "sambar", "rasam", "idli"),
        rc("Kerala", "India", "Asia", "appam", "stew", "avial", "sadya"),
        rc("Bengali", "India", "Asia", "macher jhol", "mishti doi", "shorshe"),
        rc("Hyderabadi", "India", "Asia", "biryani", "haleem", "mirchi ka salan"),
        rc("Rajasthani", "India", "Asia", "dal bati", "laal maas", "gatte"),
        rc("Goan", "India", "Asia", "vindaloo", "xacuti", "fish curry"),
        rc("Mughlai", "India", "Asia", "korma", "nihari", "kebab"),
        rc("Kashmiri", "India", "Asia", "rogan josh", "yakhni", "dum aloo"),
        rc("Chettinad", "India", "Asia", "chettinad chicken", "kuzhambu"),
        rc("Lucknowi", "India", "Asia", "tunday kebab", "awadhi biryani"),
        rc("Maharashtrian", "India", "Asia", "vada pav", "misal pav", "puran poli"),
        rc("Konkani", "India", "Asia", "sol kadhi", "kaju usal"),
        rc("Assamese", "India", "Asia", "masor tenga", "khar"),
        rc("Odia", "India", "Asia", "dalma", "pakhala", "chhena poda"),
        // Bangladesh
        rc("Bangladeshi", "Bangladesh", "Asia", "biryani", "hilsa", "pitha"),
        rc("Sylheti", "Bangladesh", "Asia", "shatkora", "hatkora", "sylhet"),
        rc("Chittagonian", "Bangladesh", "Asia", "mezbani", "shutki"),
        rc("Dhaka Street", "Bangladesh", "Asia", "fuchka", "jhalmuri", "chotpoti"),
        // Pakistan
        rc("Pakistani", "Pakistan", "Asia", "biryani", "karahi", "nihari"),
        rc("Sindhi", "Pakistan", "Asia", "sai bhaji", "sindhi biryani"),
        rc("Peshawari", "Pakistan", "Asia", "chapli kebab", "namkeen gosht"),
        rc("Balochi", "Pakistan", "Asia", "sajji", "kaak"),
        rc("Lahori", "Pakistan", "Asia", "lahori chargha", "paya", "halwa puri"),
        // Sri Lanka & Nepal
        rc("Sri Lankan", "Sri Lanka", "Asia", "kottu", "hoppers", "pol sambol"),
        rc("Jaffna Tamil", "Sri Lanka", "Asia", "jaffna crab curry", "odiyal"),
        rc("Nepali", "Nepal", "Asia", "momo", "dal bhat", "gundruk"),
        rc("Newari", "Nepal", "Asia", "choila", "yomari", "bara"),
        // East Asia
        rc("Chinese", "China", "Asia", "stir fry", "dim sum", "wok"),
        rc("Cantonese", "China", "Asia", "char siu", "wonton", "congee"),
        rc("Sichuan", "China", "Asia", "mapo tofu", "kung pao", "hot pot"),
        rc("Hunan", "China", "Asia", "chairman mao chicken", "steamed fish head"),
        rc("Shanghai", "China", "Asia", "xiaolongbao", "red braised pork"),
        rc("Beijing", "China", "Asia", "peking duck", "jianbing", "zhajiangmian"),
        rc("Fujian", "China", "Asia", "buddha jumps wall", "oyster omelette"),
        rc("Yunnan", "China", "Asia", "crossing bridge noodles", "steam pot"),
        rc("Xinjiang", "China", "Asia", "lamb kebab", "big plate chicken", "uyghur"),
        rc("Hakka", "China", "Asia", "salt baked chicken", "stuffed tofu"),
        rc("Japanese", "Japan", "Asia", "sushi", "ramen", "tempura"),
        rc("Osaka", "Japan", "Asia", "takoyaki", "okonomiyaki"),
        rc("Tokyo", "Japan", "Asia", "tsukemen", "monjayaki"),
        rc("Okinawan", "Japan", "Asia", "goya champuru", "soki soba"),
        rc("Hokkaido", "Japan", "Asia", "miso ramen", "genghis khan", "soup curry"),
        rc("Korean", "South Korea", "Asia", "kimchi", "bibimbap", "bulgogi"),
        rc("Jeju", "South Korea", "Asia", "black pork", "abalone porridge"),
        rc("Busan", "South Korea", "Asia", "dwaeji gukbap", "milmyeon"),
        rc("Taiwanese", "Taiwan", "Asia", "beef noodle soup", "bubble tea", "lu rou fan"),
        rc("Mongolian", "Mongolia", "Asia", "buuz", "khuushuur", "airag"),
        // Southeast Asia
        rc("Thai", "Thailand", "Asia", "pad thai", "green curry", "som tam"),
        rc("Northern Thai", "Thailand", "Asia", "khao soi", "sai ua", "laab"),
        rc("Isaan", "Thailand", "Asia", "som tam", "larb", "sticky rice"),
        rc("Southern Thai", "Thailand", "Asia", "massaman", "yellow curry"),
        rc("Vietnamese", "Vietnam", "Asia", "pho", "banh mi", "bun cha"),
        rc("Hanoi", "Vietnam", "Asia", "bun cha", "egg coffee", "pho bo"),
        rc("Hue", "Vietnam", "Asia", "bun bo hue", "banh khoai"),
        rc("Saigon", "Vietnam", "Asia", "com tam", "banh xeo", "pho nam"),
        rc("Malaysian", "Malaysia", "Asia", "nasi lemak", "rendang", "laksa"),
        rc("Peranakan", "Malaysia", "Asia", "nyonya", "laksa lemak", "ayam pongteh"),
        rc("Indonesian", "Indonesia", "Asia", "nasi goreng", "rendang", "satay"),
        rc("Javanese", "Indonesia", "Asia", "gudeg", "rawon", "pecel"),
        rc("Balinese", "Indonesia", "Asia", "babi guling", "lawar", "sate lilit"),
        rc("Padang", "Indonesia", "Asia", "rendang", "nasi padang", "dendeng"),
        rc("Filipino", "Philippines", "Asia", "adobo", "sinigang", "lechon"),
        rc("Visayan", "Philippines", "Asia", "lechon cebu", "kinilaw"),
        rc("Bicolano", "Philippines", "Asia", "laing", "bicol express"),
        rc("Singaporean", "Singapore", "Asia", "chicken rice", "chili crab", "laksa"),
        rc("Burmese", "Myanmar", "Asia", "mohinga", "tea leaf salad", "shan"),
        rc("Cambodian", "Cambodia", "Asia", "amok", "lok lak", "num banh chok"),
        rc("Lao", "Laos", "Asia", "laap", "sticky rice", "or lam"),
        // Central Asia
        rc("Uzbek", "Uzbekistan", "Asia", "plov", "samsa", "lagman"),
        rc("Kazakh", "Kazakhstan", "Asia", "beshbarmak", "kazy", "baursak"),
        rc("Kyrgyz", "Kyrgyzstan", "Asia", "beshbarmak", "kuurdak"),
        rc("Tajik", "Tajikistan", "Asia", "qurutob", "oshi palav"),
        rc("Afghan", "Afghanistan", "Asia", "kabuli pulao", "mantu", "bolani"),
        rc("Tibetan", "Tibet", "Asia", "thukpa", "momo", "tsampa"),

        // ══════════════════════════════════════════════════════════════
        // EUROPE
        // ══════════════════════════════════════════════════════════════
        // Southern Europe
        rc("Italian", "Italy", "Europe", "pasta", "pizza", "risotto"),
        rc("Napoli", "Italy", "Europe", "neapolitan pizza", "ragu", "sfogliatella"),
        rc("Sicilian", "Italy", "Europe", "arancini", "caponata", "cannoli"),
        rc("Tuscan", "Italy", "Europe", "ribollita", "bistecca", "pappa al pomodoro"),
        rc("Roman", "Italy", "Europe", "carbonara", "cacio e pepe", "supplì"),
        rc("Venetian", "Italy", "Europe", "risotto", "baccala", "cicchetti"),
        rc("Milanese", "Italy", "Europe", "cotoletta", "ossobuco", "panettone"),
        rc("Bolognese", "Italy", "Europe", "ragu", "tortellini", "mortadella"),
        rc("Sardinian", "Italy", "Europe", "porceddu", "culurgiones", "fregola"),
        rc("Ligurian", "Italy", "Europe", "pesto", "focaccia", "trofie"),
        rc("Spanish", "Spain", "Europe", "paella", "tapas", "gazpacho"),
        rc("Basque", "Spain", "Europe", "pintxos", "bacalao", "san sebastian"),
        rc("Catalan", "Spain", "Europe", "crema catalana", "escalivada", "fideuà"),
        rc("Andalusian", "Spain", "Europe", "gazpacho", "salmorejo", "flamenquín"),
        rc("Galician", "Spain", "Europe", "pulpo", "empanada gallega", "lacón"),
        rc("Portuguese", "Portugal", "Europe", "bacalhau", "pastel de nata", "caldo verde"),
        rc("Greek", "Greece", "Europe", "moussaka", "souvlaki", "tzatziki"),
        rc("Cretan", "Greece", "Europe", "dakos", "kalitsounia", "gamopilafo"),
        rc("Mediterranean", "Mediterranean", "Europe", "olive oil", "grilled fish", "salad"),
        // Western Europe
        rc("French", "France", "Europe", "coq au vin", "croissant", "ratatouille"),
        rc("Provençal", "France", "Europe", "bouillabaisse", "ratatouille", "tapenade"),
        rc("Parisian", "France", "Europe", "croque monsieur", "steak frites"),
        rc("Alsatian", "France", "Europe", "choucroute", "flammekueche", "baeckeoffe"),
        rc("Lyonnaise", "France", "Europe", "quenelle", "salade lyonnaise"),
        rc("Breton", "France", "Europe", "crêpe", "galette", "far breton"),
        rc("Belgian", "Belgium", "Europe", "moules frites", "waffle", "carbonnade"),
        rc("Dutch", "Netherlands", "Europe", "stamppot", "bitterballen", "stroopwafel"),
        rc("Swiss", "Switzerland", "Europe", "fondue", "raclette", "rösti"),
        rc("German", "Germany", "Europe", "schnitzel", "bratwurst", "pretzel"),
        rc("Bavarian", "Germany", "Europe", "weisswurst", "schweinshaxe", "knödel"),
        rc("Austrian", "Austria", "Europe", "wiener schnitzel", "apfelstrudel", "sachertorte"),
        // British Isles
        rc("British", "United Kingdom", "Europe", "fish and chips", "roast dinner", "pie"),
        rc("London Modern", "United Kingdom", "Europe", "gastro pub", "fusion", "borough market"),
        rc("Scottish", "United Kingdom", "Europe", "haggis", "scotch broth", "cullen skink"),
        rc("Welsh", "United Kingdom", "Europe", "cawl", "welsh rarebit", "bara brith"),
        rc("Irish", "Ireland", "Europe", "stew", "colcannon", "soda bread"),
        // Northern Europe
        rc("Swedish", "Sweden", "Europe", "meatballs", "smörgåsbord", "gravlax"),
        rc("Norwegian", "Norway", "Europe", "lutefisk", "brunost", "fårikål"),
        rc("Danish", "Denmark", "Europe", "smørrebrød", "frikadeller"),
        rc("Finnish", "Finland", "Europe", "karjalanpiirakka", "kalakukko"),
        rc("Icelandic", "Iceland", "Europe", "skyr", "plokkfiskur", "harðfiskur"),
        // Eastern Europe
        rc("Polish", "Poland", "Europe", "pierogi", "bigos", "żurek"),
        rc("Hungarian", "Hungary", "Europe", "goulash", "lángos", "paprikás"),
        rc("Czech", "Czech Republic", "Europe", "svíčková", "trdelník", "knedlíky"),
        rc("Slovak", "Slovakia", "Europe", "bryndzové halušky", "kapustnica"),
        rc("Romanian", "Romania", "Europe", "sarmale", "mici", "mămăligă"),
        rc("Bulgarian", "Bulgaria", "Europe", "shopska salad", "banitsa", "kebapche"),
        rc("Serbian", "Serbia", "Europe", "ćevapi", "ajvar", "pljeskavica"),
        rc("Croatian", "Croatia", "Europe", "peka", "štrukli", "brudet"),
        rc("Bosnian", "Bosnia", "Europe", "burek", "ćevapi", "begova čorba"),
        rc("Slovenian", "Slovenia", "Europe", "potica", "štruklji", "jota"),
        rc("Albanian", "Albania", "Europe", "tavë kosi", "byrek", "fërgesë"),
        rc("Russian", "Russia", "Europe", "borscht", "pelmeni", "blini"),
        rc("Ukrainian", "Ukraine", "Europe", "borscht", "varenyky", "holubtsi"),
        rc("Georgian", "Georgia", "Europe", "khachapuri", "khinkali", "pkhali"),
        rc("Armenian", "Armenia", "Europe", "dolma", "lahmajun", "khorovats"),
        rc("Azerbaijani", "Azerbaijan", "Europe", "plov", "dolma", "qutab"),
        rc("Baltic", "Baltic States", "Europe", "rye bread", "smoked fish", "piragi"),
        rc("Lithuanian", "Lithuania", "Europe", "cepelinai", "šaltibarščiai"),

        // ══════════════════════════════════════════════════════════════
        // MIDDLE EAST
        // ══════════════════════════════════════════════════════════════
        rc("Lebanese", "Lebanon", "Middle East", "hummus", "tabbouleh", "kibbeh"),
        rc("Turkish", "Turkey", "Middle East", "kebab", "baklava", "manti"),
        rc("Istanbul", "Turkey", "Middle East", "lahmacun", "balik ekmek"),
        rc("Anatolian", "Turkey", "Middle East", "manti", "testi kebab"),
        rc("Persian", "Iran", "Middle East", "tahdig", "ghormeh sabzi", "koobideh"),
        rc("Isfahan", "Iran", "Middle East", "beryani", "gaz"),
        rc("Iraqi", "Iraq", "Middle East", "masgouf", "dolma", "kubba"),
        rc("Syrian", "Syria", "Middle East", "kibbeh", "fattoush", "muhammara"),
        rc("Palestinian", "Palestine", "Middle East", "maqluba", "knafeh", "musakhan"),
        rc("Jordanian", "Jordan", "Middle East", "mansaf", "zarb", "maqluba"),
        rc("Yemeni", "Yemen", "Middle East", "mandi", "saltah", "bint al sahn"),
        rc("Saudi", "Saudi Arabia", "Middle East", "kabsa", "jareesh", "saleeg"),
        rc("Emirati", "UAE", "Middle East", "machboos", "luqaimat", "harees"),
        rc("Omani", "Oman", "Middle East", "shuwa", "mashuai"),
        rc("Kuwaiti", "Kuwait", "Middle East", "machboos", "gabout"),
        rc("Bahraini", "Bahrain", "Middle East", "machboos", "muhammar"),
        rc("Israeli", "Israel", "Middle East", "shakshuka", "falafel", "sabich"),
        rc("Kurdish", "Kurdistan", "Middle East", "dolma", "tepsi", "biryani"),

        // ══════════════════════════════════════════════════════════════
        // OCEANIA
        // ══════════════════════════════════════════════════════════════
        rc("Australian", "Australia", "Oceania", "meat pie", "lamington", "vegemite"),
        rc("Modern Australian", "Australia", "Oceania", "fusion", "brunch"),
        rc("New Zealand", "New Zealand", "Oceania", "pavlova", "hangi", "meat pie"),
        rc("Pacific Islander", "Pacific Islands", "Oceania", "poi", "laulau", "taro"),
        rc("Fijian", "Fiji", "Oceania", "kokoda", "lovo", "rourou"),
        rc("Samoan", "Samoa", "Oceania", "palusami", "oka", "sapasui"),
        rc("Tongan", "Tonga", "Oceania", "lu pulu", "ota ika")
    )

    private fun rc(name: String, country: String, continent: String, vararg terms: String) =
        RegionalCuisine(name, country, continent, terms.toList())
}
