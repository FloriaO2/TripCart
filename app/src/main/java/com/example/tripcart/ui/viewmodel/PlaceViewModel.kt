package com.example.tripcart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcart.data.remote.PlacesApiClient
import com.example.tripcart.data.remote.Prediction
import com.example.tripcart.util.EnvUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ISO 국가 코드를 한국어 이름으로 매핑
private val ISO_CODE_TO_KOREAN = mapOf(
    "AF" to "아프가니스탄",
    "AX" to "올란드 제도",
    "AL" to "알바니아",
    "DZ" to "알제리",
    "AS" to "아메리칸 사모아",
    "AD" to "안도라",
    "AO" to "앙골라",
    "AI" to "앵귈라",
    "AQ" to "남극",
    "AG" to "앤티가 바부다",
    "AR" to "아르헨티나",
    "AM" to "아르메니아",
    "AW" to "아루바",
    "AU" to "호주",
    "AT" to "오스트리아",
    "AZ" to "아제르바이잔",
    "BS" to "바하마",
    "BH" to "바레인",
    "BD" to "방글라데시",
    "BB" to "바베이도스",
    "BY" to "벨라루스",
    "BE" to "벨기에",
    "BZ" to "벨리즈",
    "BJ" to "베냉",
    "BM" to "버뮤다",
    "BT" to "부탄",
    "BO" to "볼리비아",
    "BQ" to "카리브 네덜란드",
    "BA" to "보스니아 헤르체고비나",
    "BW" to "보츠와나",
    "BV" to "부베 섬",
    "BR" to "브라질",
    "IO" to "영국령 인도양 지역",
    "BN" to "브루나이",
    "BG" to "불가리아",
    "BF" to "부르키나파소",
    "BI" to "부룬디",
    "KH" to "캄보디아",
    "CM" to "카메룬",
    "CA" to "캐나다",
    "CV" to "카보베르데",
    "KY" to "케이맨 제도",
    "CF" to "중앙아프리카공화국",
    "TD" to "차드",
    "CL" to "칠레",
    "CN" to "중국",
    "CX" to "크리스마스 섬",
    "CC" to "코코스 제도",
    "CO" to "콜롬비아",
    "KM" to "코모로",
    "CG" to "콩고공화국",
    "CD" to "콩고민주공화국",
    "CK" to "쿡 제도",
    "CR" to "코스타리카",
    "CI" to "코트디부아르",
    "HR" to "크로아티아",
    "CU" to "쿠바",
    "CW" to "퀴라소",
    "CY" to "키프로스",
    "CZ" to "체코",
    "DK" to "덴마크",
    "DJ" to "지부티",
    "DM" to "도미니카 연방",
    "DO" to "도미니카 공화국",
    "EC" to "에콰도르",
    "EG" to "이집트",
    "SV" to "엘살바도르",
    "GQ" to "적도기니",
    "ER" to "에리트레아",
    "EE" to "에스토니아",
    "SZ" to "에스와티니",
    "ET" to "에티오피아",
    "FK" to "포클랜드 제도",
    "FO" to "페로 제도",
    "FJ" to "피지",
    "FI" to "핀란드",
    "FR" to "프랑스",
    "GF" to "프랑스령 기아나",
    "PF" to "프랑스령 폴리네시아",
    "TF" to "프랑스 남부와 남극 지역",
    "GA" to "가봉",
    "GM" to "감비아",
    "GE" to "조지아",
    "DE" to "독일",
    "GH" to "가나",
    "GI" to "지브롤터",
    "GR" to "그리스",
    "GL" to "그린란드",
    "GD" to "그레나다",
    "GP" to "과들루프",
    "GU" to "괌",
    "GT" to "과테말라",
    "GG" to "건지섬",
    "GN" to "기니",
    "GW" to "기니비사우",
    "GY" to "가이아나",
    "HT" to "아이티",
    "HM" to "허드 맥도널드 제도",
    "VA" to "바티칸 시국",
    "HN" to "온두라스",
    "HK" to "홍콩",
    "HU" to "헝가리",
    "IS" to "아이슬란드",
    "IN" to "인도",
    "ID" to "인도네시아",
    "IR" to "이란",
    "IQ" to "이라크",
    "IE" to "아일랜드",
    "IM" to "맨섬",
    "IL" to "이스라엘",
    "IT" to "이탈리아",
    "JM" to "자메이카",
    "JP" to "일본",
    "JE" to "저지섬",
    "JO" to "요르단",
    "KZ" to "카자흐스탄",
    "KE" to "케냐",
    "KI" to "키리바시",
    "KP" to "조선민주주의인민공화국",
    "KR" to "대한민국",
    "KW" to "쿠웨이트",
    "KG" to "키르기스스탄",
    "LA" to "라오스",
    "LV" to "라트비아",
    "LB" to "레바논",
    "LS" to "레소토",
    "LR" to "라이베리아",
    "LY" to "리비아",
    "LI" to "리히텐슈타인",
    "LT" to "리투아니아",
    "LU" to "룩셈부르크",
    "MO" to "마카오",
    "MG" to "마다가스카르",
    "MW" to "말라위",
    "MY" to "말레이시아",
    "MV" to "몰디브",
    "ML" to "말리",
    "MT" to "몰타",
    "MH" to "마셜 제도",
    "MQ" to "마르티니크",
    "MR" to "모리타니",
    "MU" to "모리셔스",
    "YT" to "마요트",
    "MX" to "멕시코",
    "FM" to "미크로네시아 연방",
    "MD" to "몰도바",
    "MC" to "모나코",
    "MN" to "몽골",
    "ME" to "몬테네그로",
    "MS" to "몬트세랫",
    "MA" to "모로코",
    "MZ" to "모잠비크",
    "MM" to "미얀마",
    "NA" to "나미비아",
    "NR" to "나우루",
    "NP" to "네팔",
    "NL" to "네덜란드",
    "NC" to "뉴칼레도니아",
    "NZ" to "뉴질랜드",
    "NI" to "니카라과",
    "NE" to "니제르",
    "NG" to "나이지리아",
    "NU" to "니우에",
    "NF" to "노퍽섬",
    "MK" to "북마케도니아",
    "MP" to "북마리아나 제도",
    "NO" to "노르웨이",
    "OM" to "오만",
    "PK" to "파키스탄",
    "PW" to "팔라우",
    "PS" to "팔레스타인",
    "PA" to "파나마",
    "PG" to "파푸아뉴기니",
    "PY" to "파라과이",
    "PE" to "페루",
    "PH" to "필리핀",
    "PN" to "핏케언 제도",
    "PL" to "폴란드",
    "PT" to "포르투갈",
    "PR" to "푸에르토리코",
    "QA" to "카타르",
    "RE" to "레위니옹",
    "RO" to "루마니아",
    "RU" to "러시아",
    "RW" to "르완다",
    "BL" to "생바르텔레미",
    "SH" to "세인트헬레나",
    "KN" to "세인트키츠 네비스",
    "LC" to "세인트루시아",
    "MF" to "생마르탱",
    "PM" to "생피에르 미클롱",
    "VC" to "세인트빈센트 그레나딘",
    "WS" to "사모아",
    "SM" to "산마리노",
    "ST" to "상투메 프린시페",
    "SA" to "사우디아라비아",
    "SN" to "세네갈",
    "RS" to "세르비아",
    "SC" to "세이셸",
    "SL" to "시에라리온",
    "SG" to "싱가포르",
    "SX" to "신트마르턴",
    "SK" to "슬로바키아",
    "SI" to "슬로베니아",
    "SB" to "솔로몬 제도",
    "SO" to "소말리아",
    "ZA" to "남아프리카공화국",
    "GS" to "사우스조지아 사우스샌드위치 제도",
    "ES" to "스페인",
    "LK" to "스리랑카",
    "SD" to "수단",
    "SR" to "수리남",
    "SJ" to "스발바르 얀마옌",
    "SE" to "스웨덴",
    "CH" to "스위스",
    "SY" to "시리아",
    "TW" to "대만",
    "TJ" to "타지키스탄",
    "TZ" to "탄자니아",
    "TH" to "태국",
    "TL" to "동티모르",
    "TG" to "토고",
    "TK" to "토켈라우",
    "TO" to "통가",
    "TT" to "트리니다드 토바고",
    "TN" to "튀니지",
    "TR" to "터키",
    "TM" to "투르크메니스탄",
    "TC" to "터크스 케이커스 제도",
    "TV" to "투발루",
    "UG" to "우간다",
    "UA" to "우크라이나",
    "AE" to "아랍에미리트",
    "GB" to "영국",
    "US" to "미국",
    "UM" to "미국령 군소 제도",
    "UY" to "우루과이",
    "UZ" to "우즈베키스탄",
    "VU" to "바누아투",
    "VE" to "베네수엘라",
    "VN" to "베트남",
    "VG" to "영국령 버진아일랜드",
    "VI" to "미국령 버진아일랜드",
    "WF" to "월리스 푸투나",
    "EH" to "서사하라",
    "YE" to "예멘",
    "ZM" to "잠비아",
    "ZW" to "짐바브웨"
)

// ISO 국가 코드를 영어 이름으로 매핑 (주소에서 국가 이름 삭제용)
private val ISO_CODE_TO_ENGLISH = mapOf(
    "AF" to "Afghanistan", "AX" to "Åland Islands", "AL" to "Albania", "DZ" to "Algeria",
    "AS" to "American Samoa", "AD" to "Andorra", "AO" to "Angola", "AI" to "Anguilla",
    "AQ" to "Antarctica", "AG" to "Antigua and Barbuda", "AR" to "Argentina", "AM" to "Armenia",
    "AW" to "Aruba", "AU" to "Australia", "AT" to "Austria", "AZ" to "Azerbaijan",
    "BS" to "Bahamas", "BH" to "Bahrain", "BD" to "Bangladesh", "BB" to "Barbados",
    "BY" to "Belarus", "BE" to "Belgium", "BZ" to "Belize", "BJ" to "Benin",
    "BM" to "Bermuda", "BT" to "Bhutan", "BO" to "Bolivia", "BQ" to "Caribbean Netherlands",
    "BA" to "Bosnia and Herzegovina", "BW" to "Botswana", "BV" to "Bouvet Island", "BR" to "Brazil",
    "IO" to "British Indian Ocean Territory", "BN" to "Brunei", "BG" to "Bulgaria", "BF" to "Burkina Faso",
    "BI" to "Burundi", "KH" to "Cambodia", "CM" to "Cameroon", "CA" to "Canada",
    "CV" to "Cape Verde", "KY" to "Cayman Islands", "CF" to "Central African Republic", "TD" to "Chad",
    "CL" to "Chile", "CN" to "China", "CX" to "Christmas Island", "CC" to "Cocos (Keeling) Islands",
    "CO" to "Colombia", "KM" to "Comoros", "CG" to "Congo", "CD" to "Congo (DRC)",
    "CK" to "Cook Islands", "CR" to "Costa Rica", "CI" to "Côte d'Ivoire", "HR" to "Croatia",
    "CU" to "Cuba", "CW" to "Curaçao", "CY" to "Cyprus", "CZ" to "Czech Republic",
    "DK" to "Denmark", "DJ" to "Djibouti", "DM" to "Dominica", "DO" to "Dominican Republic",
    "EC" to "Ecuador", "EG" to "Egypt", "SV" to "El Salvador", "GQ" to "Equatorial Guinea",
    "ER" to "Eritrea", "EE" to "Estonia", "SZ" to "Eswatini", "ET" to "Ethiopia",
    "FK" to "Falkland Islands", "FO" to "Faroe Islands", "FJ" to "Fiji", "FI" to "Finland",
    "FR" to "France", "GF" to "French Guiana", "PF" to "French Polynesia", "TF" to "French Southern Territories",
    "GA" to "Gabon", "GM" to "Gambia", "GE" to "Georgia", "DE" to "Germany",
    "GH" to "Ghana", "GI" to "Gibraltar", "GR" to "Greece", "GL" to "Greenland",
    "GD" to "Grenada", "GP" to "Guadeloupe", "GU" to "Guam", "GT" to "Guatemala",
    "GG" to "Guernsey", "GN" to "Guinea", "GW" to "Guinea-Bissau", "GY" to "Guyana",
    "HT" to "Haiti", "HM" to "Heard Island and McDonald Islands", "VA" to "Vatican City", "HN" to "Honduras",
    "HK" to "Hong Kong", "HU" to "Hungary", "IS" to "Iceland", "IN" to "India",
    "ID" to "Indonesia", "IR" to "Iran", "IQ" to "Iraq", "IE" to "Ireland",
    "IM" to "Isle of Man", "IL" to "Israel", "IT" to "Italy", "JM" to "Jamaica",
    "JP" to "Japan", "JE" to "Jersey", "JO" to "Jordan", "KZ" to "Kazakhstan",
    "KE" to "Kenya", "KI" to "Kiribati", "KP" to "North Korea", "KR" to "South Korea",
    "KW" to "Kuwait", "KG" to "Kyrgyzstan", "LA" to "Laos", "LV" to "Latvia",
    "LB" to "Lebanon", "LS" to "Lesotho", "LR" to "Liberia", "LY" to "Libya",
    "LI" to "Liechtenstein", "LT" to "Lithuania", "LU" to "Luxembourg", "MO" to "Macao",
    "MG" to "Madagascar", "MW" to "Malawi", "MY" to "Malaysia", "MV" to "Maldives",
    "ML" to "Mali", "MT" to "Malta", "MH" to "Marshall Islands", "MQ" to "Martinique",
    "MR" to "Mauritania", "MU" to "Mauritius", "YT" to "Mayotte", "MX" to "Mexico",
    "FM" to "Micronesia", "MD" to "Moldova", "MC" to "Monaco", "MN" to "Mongolia",
    "ME" to "Montenegro", "MS" to "Montserrat", "MA" to "Morocco", "MZ" to "Mozambique",
    "MM" to "Myanmar", "NA" to "Namibia", "NR" to "Nauru", "NP" to "Nepal",
    "NL" to "Netherlands", "NC" to "New Caledonia", "NZ" to "New Zealand", "NI" to "Nicaragua",
    "NE" to "Niger", "NG" to "Nigeria", "NU" to "Niue", "NF" to "Norfolk Island",
    "MK" to "North Macedonia", "MP" to "Northern Mariana Islands", "NO" to "Norway", "OM" to "Oman",
    "PK" to "Pakistan", "PW" to "Palau", "PS" to "Palestine", "PA" to "Panama",
    "PG" to "Papua New Guinea", "PY" to "Paraguay", "PE" to "Peru", "PH" to "Philippines",
    "PN" to "Pitcairn", "PL" to "Poland", "PT" to "Portugal", "PR" to "Puerto Rico",
    "QA" to "Qatar", "RE" to "Réunion", "RO" to "Romania", "RU" to "Russia",
    "RW" to "Rwanda", "BL" to "Saint Barthélemy", "SH" to "Saint Helena", "KN" to "Saint Kitts and Nevis",
    "LC" to "Saint Lucia", "MF" to "Saint Martin", "PM" to "Saint Pierre and Miquelon",
    "VC" to "Saint Vincent and the Grenadines", "WS" to "Samoa", "SM" to "San Marino",
    "ST" to "São Tomé and Príncipe", "SA" to "Saudi Arabia", "SN" to "Senegal", "RS" to "Serbia",
    "SC" to "Seychelles", "SL" to "Sierra Leone", "SG" to "Singapore", "SX" to "Sint Maarten",
    "SK" to "Slovakia", "SI" to "Slovenia", "SB" to "Solomon Islands", "SO" to "Somalia",
    "ZA" to "South Africa", "GS" to "South Georgia and the South Sandwich Islands", "ES" to "Spain",
    "LK" to "Sri Lanka", "SD" to "Sudan", "SR" to "Suriname", "SJ" to "Svalbard and Jan Mayen",
    "SE" to "Sweden", "CH" to "Switzerland", "SY" to "Syria", "TW" to "Taiwan",
    "TJ" to "Tajikistan", "TZ" to "Tanzania", "TH" to "Thailand", "TL" to "Timor-Leste",
    "TG" to "Togo", "TK" to "Tokelau", "TO" to "Tonga", "TT" to "Trinidad and Tobago",
    "TN" to "Tunisia", "TR" to "Turkey", "TM" to "Turkmenistan", "TC" to "Turks and Caicos Islands",
    "TV" to "Tuvalu", "UG" to "Uganda", "UA" to "Ukraine", "AE" to "United Arab Emirates",
    "GB" to "United Kingdom", "US" to "United States", "UM" to "United States Minor Outlying Islands",
    "UY" to "Uruguay", "UZ" to "Uzbekistan", "VU" to "Vanuatu", "VE" to "Venezuela",
    "VN" to "Vietnam", "VG" to "British Virgin Islands", "VI" to "U.S. Virgin Islands",
    "WF" to "Wallis and Futuna", "EH" to "Western Sahara", "YE" to "Yemen",
    "ZM" to "Zambia", "ZW" to "Zimbabwe"
)

// 모든 가능한 국가 이름(한국어/영어)을 한국어 이름으로 직접 매핑 (주소 파싱용 - 폴백)
// AddressComponent에서 국가 정보 추출 (ISO 코드를 한국어 이름으로 변환)
// 반환값: Triple(한국어 이름, 영어 이름, ISO 코드)
private fun extractCountryFromAddressComponents(addressComponents: List<com.example.tripcart.data.remote.AddressComponent>?): Triple<String?, String?, String?>? {
    if (addressComponents == null) {
        android.util.Log.d("PlaceViewModel", "extractCountryFromAddressComponents - addressComponents is null")
        return null
    }
    
    android.util.Log.d("PlaceViewModel", "extractCountryFromAddressComponents - addressComponents size: ${addressComponents.size}")
    
    // types에 "country"가 포함된 AddressComponent 찾기
    val countryComponent = addressComponents.find { component ->
        component.types.contains("country")
    }
    
    if (countryComponent == null) {
        android.util.Log.d("PlaceViewModel", "extractCountryFromAddressComponents - country component not found")
        return null
    }
    
    return countryComponent.let { component ->
        val isoCode = component.shortName // ISO 코드 (예: "KR", "US", "JP")
        val koreanName = ISO_CODE_TO_KOREAN[isoCode] 
            ?: component.longName // 매핑이 없으면 long_name 사용 (language=ko로 호출했으므로 한국어일 가능성)
        val englishName = ISO_CODE_TO_ENGLISH[isoCode] // 영어 이름 (주소에서 삭제용)
        
        android.util.Log.d("PlaceViewModel", "extractCountryFromAddressComponents - ISO: $isoCode, 한국어: $koreanName, 영어: $englishName")
        
        // 한국어 이름, 영어 이름, ISO 코드 반환
        Triple(koreanName, englishName, isoCode)
    }
}

// AddressComponent에서 주소 구성
private fun buildAddressFromComponents(addressComponents: List<com.example.tripcart.data.remote.AddressComponent>?): String? {
    if (addressComponents == null || addressComponents.isEmpty()) {
        return null
    }
    
    // country 타입을 제외한 address_components만 필터링하고 long_name 추출
    val addressParts = addressComponents
        .filter { component -> !component.types.contains("country") }
        .map { it.longName }
        .filter { it.isNotEmpty() }
    
    if (addressParts.isEmpty()) {
        return null
    }
    
    // long_name들을 쉼표로 연결
    return addressParts.joinToString(", ")
}

// 주소에서 국가 이름(한국어/영어) 및 ISO 코드 삭제
private fun removeCountryFromAddress(address: String, koreanName: String?, englishName: String?, isoCode: String?): String {
    var result = address
    
    android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 원본 주소: $address")
    android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 한국어 국가명: $koreanName")
    android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 영어 국가명: $englishName")
    android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - ISO 코드: $isoCode")
    
    // 한국어 이름 삭제 (정규식 사용: 앞뒤 쉼표/공백 포함하여 제거)
    koreanName?.let { name ->
        if (name.isNotEmpty()) {
            // 국가 이름 앞뒤의 쉼표, 공백과 함께 제거
            val pattern = Regex("[,，\\s]*${Regex.escape(name)}[,，\\s]*", RegexOption.IGNORE_CASE)
            result = result.replace(pattern, "")
            android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 한국어 제거 후: $result")
        }
    }
    
    // 영어 이름 삭제 (정규식 사용: 앞뒤 쉼표/공백 포함하여 제거)
    englishName?.let { name ->
        if (name.isNotEmpty()) {
            // 국가 이름 앞뒤의 쉼표, 공백과 함께 제거
            val pattern = Regex("[,，\\s]*${Regex.escape(name)}[,，\\s]*", RegexOption.IGNORE_CASE)
            result = result.replace(pattern, "")
            android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 영어 제거 후: $result")
        }
    }
    
    // ISO 코드 삭제 (정규식 사용: 앞뒤 쉼표/공백 포함하여 제거)
    isoCode?.let { code ->
        if (code.isNotEmpty()) {
            // ISO 코드 앞뒤의 쉼표, 공백과 함께 제거
            val pattern = Regex("[,，\\s]*${Regex.escape(code)}[,，\\s]*", RegexOption.IGNORE_CASE)
            result = result.replace(pattern, "")
            android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - ISO 코드 제거 후: $result")
        }
    }
    
    // 앞뒤 쉼표 및 공백 제거
    result = result
        .trim()
        .replace(Regex("^[,，\\s]+"), "")
        .replace(Regex("[,，\\s]+$"), "")
        .trim()
    
    android.util.Log.d("PlaceViewModel", "removeCountryFromAddress - 최종 주소: $result")
    
    return result
}

data class PlaceDetails(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val country: String?,
    val phoneNumber: String?,
    val websiteUri: String?,
    val openingHours: List<String>?,
    val photoUrl: String?,
    val businessStatus: String? = null, // OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY
    val isOpenNow: Boolean? = null, // 현재 영업 중인지 여부
    val currentOpeningHours: List<String>? = null // 오늘의 운영 시간
)

// PlaceDetails를 포함해 UI와 관련된 모든 요소들을 다룸
data class PlaceUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val predictions: List<Prediction> = emptyList(),
    val searchError: String? = null,
    val selectedPlace: PlaceDetails? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class PlaceViewModel(application: Application) : AndroidViewModel(application) {
    
    // Autocomplete 결과의 주소에서 국가 이름 제거 (모든 가능한 국가 이름 체크)
    fun removeCountryFromAutocompleteAddress(address: String?): String {
        if (address == null || address.isEmpty()) return address ?: ""
        
        var result: String = address
        
        // 모든 한국어 국가 이름 제거
        ISO_CODE_TO_KOREAN.values.forEach { koreanName ->
            if (koreanName.isNotEmpty()) {
                val pattern = Regex("[,，\\s]*${Regex.escape(koreanName)}[,，\\s]*", RegexOption.IGNORE_CASE)
                result = result.replace(pattern, "")
            }
        }
        
        // 모든 영어 국가 이름 제거
        ISO_CODE_TO_ENGLISH.values.forEach { englishName ->
            if (englishName.isNotEmpty()) {
                val pattern = Regex("[,，\\s]*${Regex.escape(englishName)}[,，\\s]*", RegexOption.IGNORE_CASE)
                result = result.replace(pattern, "")
            }
        }
        
        // 모든 ISO 코드 제거 (2자리 대문자 코드)
        ISO_CODE_TO_KOREAN.keys.forEach { isoCode ->
            if (isoCode.isNotEmpty()) {
                val pattern = Regex("[,，\\s]*${Regex.escape(isoCode)}[,，\\s]*", RegexOption.IGNORE_CASE)
                result = result.replace(pattern, "")
            }
        }
        
        // 앞뒤 쉼표 및 공백 제거
        return result
            .trim()
            .replace(Regex("^[,，\\s]+"), "")
            .replace(Regex("[,，\\s]+$"), "")
            .trim()
    }
    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()
    
    private val placesApiService = PlacesApiClient.service
    private val apiKey = PlacesApiClient.getApiKey(application)
    private val db = FirebaseFirestore.getInstance()
    
    // 장소 검색 (Autocomplete 사용)
    fun searchPlaces(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                searchError = null
            )
            
            // 최소 2글자 이상 입력해야 검색 가능
            if (query.length < 2) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false, // 실제 검색 안 함!
                    predictions = emptyList()
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                searchError = null
            )
            
            try {
                val response = placesApiService.autocomplete(
                    input = query,
                    key = apiKey,
                    language = "ko"  // 항상 한국어 응답
                )
                
                if (response.status == "OK" || response.status == "ZERO_RESULTS") {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        predictions = response.predictions ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchError = "검색 중 오류가 발생했습니다: ${response.status}",
                        predictions = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = "검색 중 오류가 발생했습니다: ${e.message}",
                    predictions = emptyList()
                )
            }
        }
    }
    
    // 선택한 장소의 상세 정보 가져오기
    fun fetchPlaceDetails(placeId: String, autocompleteAddress: String? = null, autocompleteName: String? = null) {
        if (placeId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "장소 ID가 비어있습니다."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                selectedPlace = null
            )
            
            try {
                android.util.Log.d("PlaceViewModel", "Fetching place details for: $placeId")
                
                // 한국어, 영어, 자국어 순서로 이름 가져오기
                val koResponse = placesApiService.placeDetails(
                    placeId = placeId,
                    key = apiKey,
                    language = "ko"
                )
                
                if (koResponse.status != "OK") {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "장소 정보를 가져오는데 실패했습니다: ${koResponse.status}"
                    )
                    return@launch
                }
                
                val koPlace = koResponse.result ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "장소 정보를 찾을 수 없습니다."
                    )
                    return@launch
                }
                
                // 영어 이름 가져오기
                val enResponse = try {
                    placesApiService.placeDetails(
                        placeId = placeId,
                        key = apiKey,
                        language = "en"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PlaceViewModel", "Error fetching English name", e)
                    null
                }
                val enPlace = if (enResponse?.status == "OK") enResponse.result else null
                
                // 일단 한국어와 영어를 가져오고,
                // 둘 다 없으면 autocompleteName (자국어 이름) 사용
                
                // 이름 우선순위: 한국어 > 영어 > autocompleteName
                val koName = koPlace.name?.takeIf { it.isNotEmpty() }
                val enName = enPlace?.name?.takeIf { it.isNotEmpty() }
                
                // 한국어 이름이 실제로 한글을 포함하는지 확인
                val isKoNameKorean = koName?.let { name ->
                    name.any { char -> char.code in 0xAC00..0xD7A3 } // 한글 유니코드 범위
                } ?: false
                
                val finalName = when {
                    isKoNameKorean && koName != null -> koName // 한국어 이름이 한글을 포함하면 사용
                    enName != null -> enName // 영어 이름 사용
                    koName != null -> koName // 한국어 응답이지만 한글이 아닐 수 있음
                    autocompleteName != null && autocompleteName.isNotEmpty() -> autocompleteName
                    else -> ""
                }
                
                android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - 이름 결정: 한국어=$koName (한글포함=$isKoNameKorean), 영어=$enName, autocomplete=$autocompleteName, 최종=$finalName")
                
                // 주소와 기타 정보는 한국어 응답 사용
                val place = koPlace
                
                // AddressComponent에서 주소 구성 (country 제외하고 long_name만 순서대로 나열)
                val addressFromComponents = buildAddressFromComponents(place.addressComponents)
                android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - addressFromComponents: $addressFromComponents")
                
                // AddressComponent에서 주소를 만들 수 있으면 사용, 없으면 formattedAddress 사용
                val finalAddress = addressFromComponents ?: place.formattedAddress ?: autocompleteAddress ?: ""
                android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - finalAddress: $finalAddress")
                android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - addressComponents: ${place.addressComponents?.size ?: 0}")
                
                // AddressComponent에서 국가 추출
                val countryInfo = extractCountryFromAddressComponents(place.addressComponents)
                
                // AddressComponent에서 주소를 만들었으면 이미 country가 제외되어 있으므로 그대로 사용
                // formattedAddress를 사용한 경우에만 국가 제거 필요 (하지만 addressFromComponents를 우선 사용하므로 거의 발생하지 않음)
                val (countryName, addressWithoutCountry) = if (countryInfo != null) {
                    val (koreanName, englishName, isoCode) = countryInfo
                    android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - 국가 정보 찾음: 한국어=$koreanName, 영어=$englishName, ISO=$isoCode")
                    // AddressComponent에서 주소를 만들었으면 이미 country가 제외되어 있으므로 그대로 사용
                    val addressWithoutCountry = if (addressFromComponents != null) {
                        addressFromComponents
                    } else {
                        // formattedAddress를 사용한 경우에만 국가 제거
                        removeCountryFromAddress(finalAddress, koreanName, englishName, isoCode)
                    }
                    android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - 최종 주소: $addressWithoutCountry")
                    Pair(koreanName, addressWithoutCountry)
                } else {
                    android.util.Log.d("PlaceViewModel", "fetchPlaceDetails - 국가 정보를 찾지 못함, 원본 주소 사용")
                    // AddressComponent에서 국가를 찾지 못한 경우
                    Pair(null, finalAddress)
                }
                
                // 영업시간 추출
                val openingHours = place.openingHours?.weekdayText
                
                // 오늘의 운영 시간 추출 (currentOpeningHours가 있으면 우선 사용)
                val currentOpeningHours = place.currentOpeningHours?.weekdayText ?: place.openingHours?.weekdayText
                
                // 현재 영업 중인지 여부 (currentOpeningHours의 openNow가 있으면 우선 사용)
                val isOpenNow = place.currentOpeningHours?.openNow ?: place.openingHours?.openNow
                
                // 영업 상태 (OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY)
                val businessStatus = place.businessStatus
                
                // 대표 이미지 URL 생성 (이미지가 있을 경우)
                val photoUrl = place.photos?.firstOrNull()?.let { photo ->
                    PlacesApiClient.getPhotoUrl(
                        photoReference = photo.photoReference,
                        maxWidth = 800,
                        maxHeight = 800,
                        apiKey = apiKey
                    )
                }
                
                val placeDetails = PlaceDetails(
                    placeId = place.placeId ?: "",
                    name = finalName,
                    latitude = place.geometry?.location?.lat ?: 0.0,
                    longitude = place.geometry?.location?.lng ?: 0.0,
                    address = addressWithoutCountry,
                    country = countryName,
                    phoneNumber = place.formattedPhoneNumber,
                    websiteUri = place.website,
                    openingHours = openingHours,
                    photoUrl = photoUrl,
                    businessStatus = businessStatus,
                    isOpenNow = isOpenNow,
                    currentOpeningHours = currentOpeningHours
                )
                
                android.util.Log.d("PlaceViewModel", "Place details fetched successfully: ${placeDetails.name}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedPlace = placeDetails
                )
            } catch (e: Exception) {
                android.util.Log.e("PlaceViewModel", "Error fetching place details", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "장소 정보를 가져오는데 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 장소 중복 체크 후 리스트 선택 화면으로 이동
    // Firestore에 이미 존재하면 그 데이터를 사용하고, 없으면 저장 후 사용
    fun checkPlaceAndNavigate(placeDetails: PlaceDetails, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false
            )
            
            try {
                // placeId로 Firestore에서 조회
                val placeDoc = try {
                    db.collection("places").document(placeDetails.placeId).get().await()
                } catch (e: Exception) {
                    android.util.Log.e("PlaceViewModel", "Error checking duplicate place", e)
                    null
                }
                
                val finalPlaceDetails: PlaceDetails = if (placeDoc?.exists() == true) {
                    // Firestore에 이미 존재하는 경우, Firestore의 데이터를 사용
                    val data = placeDoc.data
                    PlaceDetails(
                        placeId = data?.get("placeId") as? String ?: placeDetails.placeId,
                        name = data?.get("name") as? String ?: placeDetails.name,
                        latitude = (data?.get("latitude") as? Double) ?: placeDetails.latitude,
                        longitude = (data?.get("longitude") as? Double) ?: placeDetails.longitude,
                        address = data?.get("address") as? String ?: placeDetails.address,
                        country = data?.get("country") as? String ?: placeDetails.country,
                        phoneNumber = data?.get("phoneNumber") as? String ?: placeDetails.phoneNumber,
                        websiteUri = data?.get("websiteUri") as? String ?: placeDetails.websiteUri,
                        openingHours = (data?.get("openingHours") as? List<*>)?.mapNotNull { it as? String } 
                            ?: placeDetails.openingHours,
                        photoUrl = data?.get("photoUrl") as? String ?: placeDetails.photoUrl,
                        businessStatus = data?.get("businessStatus") as? String ?: placeDetails.businessStatus,
                        isOpenNow = (data?.get("isOpenNow") as? Boolean) ?: placeDetails.isOpenNow,
                        currentOpeningHours = (data?.get("currentOpeningHours") as? List<*>)?.mapNotNull { it as? String }
                            ?: placeDetails.currentOpeningHours
                    )
                } else {
                    // Firestore에 존재하지 않는 경우, Google Places API 데이터를 Firestore에 저장
                    val placeData = hashMapOf(
                        "placeId" to placeDetails.placeId,
                        "name" to placeDetails.name,
                        "latitude" to placeDetails.latitude,
                        "longitude" to placeDetails.longitude,
                        "address" to (placeDetails.address ?: ""),
                        "country" to (placeDetails.country ?: ""),
                        "phoneNumber" to (placeDetails.phoneNumber ?: ""),
                        "websiteUri" to (placeDetails.websiteUri ?: ""),
                        "openingHours" to (placeDetails.openingHours ?: emptyList<String>()),
                        "photoUrl" to (placeDetails.photoUrl ?: "")
                    )
                    
                    db.collection("places")
                        .document(placeDetails.placeId)
                        .set(placeData)
                        .await()
                    
                    // 저장한 데이터를 그대로 사용
                    placeDetails
                }
                
                // 선택한 장소를 ViewModel에 저장 (Firestore의 데이터 또는 새로 저장한 데이터)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    selectedPlace = finalPlaceDetails
                )
                onResult(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "장소 확인에 실패했습니다: ${e.message}"
                )
                onResult(false)
            }
        }
    }
    
    // Firebase Firestore에 장소 저장 (placeId로 중복 체크)
    fun savePlaceToFirestore(placeDetails: PlaceDetails) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false
            )
            
            try {
                // placeId로 중복 체크
                val placeDoc = try {
                    db.collection("places").document(placeDetails.placeId).get().await()
                } catch (e: Exception) {
                    android.util.Log.e("PlaceViewModel", "Error checking duplicate place", e)
                    null
                }
                
                if (placeDoc?.exists() == true) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "이미 등록된 장소입니다."
                    )
                    return@launch
                }
                
                // Firestore에 저장
                val placeData = hashMapOf(
                    "placeId" to placeDetails.placeId,
                    "name" to placeDetails.name,
                    "latitude" to placeDetails.latitude,
                    "longitude" to placeDetails.longitude,
                    "address" to (placeDetails.address ?: ""),
                    "country" to (placeDetails.country ?: ""),
                    "phoneNumber" to (placeDetails.phoneNumber ?: ""),
                    "websiteUri" to (placeDetails.websiteUri ?: ""),
                    "openingHours" to (placeDetails.openingHours ?: emptyList<String>()),
                    "photoUrl" to (placeDetails.photoUrl ?: "")
                )
                
                db.collection("places")
                    .document(placeDetails.placeId)
                    .set(placeData)
                    .await()
                
                android.util.Log.d("PlaceViewModel", "Place saved successfully: ${placeDetails.placeId}")
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "장소 저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
    
    fun clearSelectedPlace() {
        _uiState.value = _uiState.value.copy(selectedPlace = null)
    }
    
    // 장소의 현재 운영 상태만 가져오기 (경량 API 호출)
    suspend fun fetchPlaceBusinessStatus(placeId: String): PlaceDetails? {
        if (placeId.isEmpty()) return null
        
        return try {
            // 최소한의 필드(운영 상태)만 요청하여 API 호출 비용 절감
            val response = placesApiService.placeDetails(
                placeId = placeId,
                key = apiKey,
                language = "ko",
                fields = "place_id,current_opening_hours,business_status"
            )
            
            if (response.status != "OK") {
                android.util.Log.w("PlaceViewModel", "Failed to fetch business status: ${response.status}")
                return null
            }
            
            val place = response.result ?: return null
            
            // 운영 상태 정보만 포함한 PlaceDetails 반환 (나머지는 null)
            PlaceDetails(
                placeId = place.placeId ?: placeId,
                name = "", // Firestore에서 가져올 예정
                latitude = 0.0,
                longitude = 0.0,
                address = null,
                country = null,
                phoneNumber = null,
                websiteUri = null,
                openingHours = null,
                photoUrl = null,
                businessStatus = place.businessStatus,
                isOpenNow = place.currentOpeningHours?.openNow,
                currentOpeningHours = place.currentOpeningHours?.weekdayText
            )
        } catch (e: Exception) {
            android.util.Log.e("PlaceViewModel", "Error fetching business status", e)
            null
        }
    }
    
    // 여러 장소의 현재 운영 상태를 불러오기
    // 각 placeId에 대해 순차적으로 API 호출 (Places API는 한 번에 하나만 조회 가능)
    suspend fun fetchPlaceBusinessStatusBatch(placeIds: List<String>): Map<String, PlaceDetails> {
        val result = mutableMapOf<String, PlaceDetails>()
        
        placeIds.forEach { placeId ->
            val status = fetchPlaceBusinessStatus(placeId)
            if (status != null) {
                result[placeId] = status
            }
            // API 호출 제한을 고려한 짧은 딜레이 추가 (선택사항)
            kotlinx.coroutines.delay(100)
        }
        
        return result
    }
}
