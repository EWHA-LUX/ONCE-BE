package ewha.lux.once.domain.home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ewha.lux.once.domain.card.dto.GoogleMapPlaceResponseDto;
import ewha.lux.once.domain.card.dto.Place;
import ewha.lux.once.domain.card.dto.SearchStoresRequestDto;
import ewha.lux.once.domain.card.dto.SearchStoresResponseDto;
import ewha.lux.once.domain.card.entity.Card;
import ewha.lux.once.domain.card.entity.OwnedCard;
import ewha.lux.once.domain.home.dto.*;
import ewha.lux.once.domain.home.entity.*;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {
    @Value("${google-map.api-key}")
    private String apiKey;
    private final RestTemplate restTemplate;
    private final FavoriteRepository favoriteRepository;
    private final CardRepository cardRepository;
    private final OwnedCardRepository ownedCardRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final AnnouncementRepository announcementRepository;

    private final GeminiService geminiService;
    private final OpenaiService openaiService;
    private final FCMTokenRepository fcmTokenRepository;
    private final FirebaseCloudMessageService firebaseCloudMessageService;
    private final BeaconRepository beaconRepository;
    private final CODEFAsyncService codefAsyncService;

    // 챗봇 카드 추천
    public ChatDto getHomeChat(Users nowUser, String keyword, int paymentAmount) throws CustomException {

        // 1. Gemini 사용하는 경우
//        String response = geminiService.gemini(nowUser, keyword, paymentAmount);

        // 2. GPT 사용하는 경우
        String response = openaiService.cardRecommend(nowUser, keyword, paymentAmount);
        ObjectMapper objectMapper = new ObjectMapper();
        Integer cardId;
        Card card;
        String benefit;
        Integer discount;
        try {
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            cardId = (Integer) map.get("카드번호");
            card = cardRepository.findById(Long.valueOf(cardId)).orElse(null);
            benefit = (String) map.get("혜택 정보");
            discount = (Integer) map.get("할인 금액");

        } catch (JsonProcessingException e) {
            throw new CustomException(ResponseCode.FAILED_TO_OPENAI_RECOMMEND);
        }

        // 챗봇 대화 기록
        ChatHistory chat = ChatHistory.builder()
                .users(nowUser)
                .keyword(keyword)
                .paymentAmount(paymentAmount)
                .cardName(card.getName())
                .benefit(benefit)
                .discount(discount)
                .hasPaid(false)
                .category(getCategory(keyword))
                .build();

        // Chat 객체 저장
        ChatHistory savedChat = chatHistoryRepository.save(chat);

        OwnedCard ownedCard = ownedCardRepository.findOwnedCardByCardIdAndUsers(Long.valueOf(cardId),nowUser);

        // 챗봇 응답
        ChatDto chatDto = ChatDto.builder()
                .nickname(nowUser.getNickname())
                .chatId(savedChat.getId())
                .cardName(card.getName())
                .cardCompany(card.getCardCompany().getName())
                .cardImg(card.getImgUrl())
                .benefit(benefit)
                .discount(discount)
                .isMain(ownedCard.isMain())
                .build();

        return chatDto;
    }

    /*
     *  카테고리 처리 함수
     *  @param keyword
     */
    public static String getCategory(String keyword) {

        String[] convenienceStoreKeywords = {"편의점", "CU", "씨유", "GS25", "지에스", "세븐일레븐", "이마트24", "미니스톱"};
        String[] culturalKeywords = {"문화", "영화", "CGV", "씨지브이", "씨지비", "메가박스", "megabox", "롯데시네마", "OTT", "오티티", "넷플릭스", "netflix", "티빙", "tving", "디즈니플러스", "disney", "웨이브", "wavve", "왓챠", "watcha", "쿠팡플레이", "스포츠","놀이공원","에버랜드","롯데월드"};
        String[] cafeKeywords = {"카페", "커피", "cafe", "coffee", "스타벅스", "starbucks", "빽다방", "폴바셋", "커피빈", "투썸플레이스", "컴포즈", "매머드커피", "메가커피", "카페봄봄", "공차", "이디야"};
        String[] transportationKeywords = {"교통", "지하철", "택시", "버스", "bus", "기차", "티머니", "KTX", "무궁화호"};
        String[] shoppingKeywords = {"쇼핑", "백화점", "현대백화점", "롯데백화점", "신세계백화점", "롯데마트", "이마트", "emart", "홈플러스", "homeplus", "롯데몰", "스타필드", "아울렛", "쿠팡", "coupang", "G마켓", "11번가", "네이버쇼핑", "마켓컬리", "배달의민족", "요기요", "배달","서점","올리브영"};
        String[] bakeryKeywords = {"베이커리", "빵", "bread", "bakery", "파리바게트", "뚜레쥬르", "성심당", "앤티앤스", "홍종흔베이커리", "아우어베이커리"};

        for (String key : convenienceStoreKeywords) {
            if (keyword.contains(key)) {
                return "편의점";
            }
        }
        for (String key : culturalKeywords) {
            if (keyword.contains(key)) {
                return "문화생활";
            }
        }
        for (String key : cafeKeywords) {
            if (keyword.contains(key)) {
                return "카페";
            }
        }
        for (String key : transportationKeywords) {
            if (keyword.contains(key)) {
                return "교통";
            }
        }
        for (String key : shoppingKeywords) {
            if (keyword.contains(key)) {
                return "쇼핑";
            }
        }
        for (String key : bakeryKeywords) {
            if (keyword.contains(key)) {
                return "베이커리";
            }
        }
        return "기타";
    }

    // 홈 화면 기본정보 조회
    public HomeDto getHome(Users nowUser) throws CustomException {
        // 사용자별 맞춤형 키워드 조회
        List<ChatHistory> allChatHistory = chatHistoryRepository.findByUsers(nowUser);

        Map<String, Integer> keywordFrequencyMap = new HashMap<>();
        for (ChatHistory chatHistory : allChatHistory) {
            String keyword = chatHistory.getKeyword();
            keywordFrequencyMap.put(keyword, keywordFrequencyMap.getOrDefault(keyword, 0) + 1);
        }

        int ownedCardCount = ownedCardRepository.countAllByUsers(nowUser);


        // 빈도수가 높은 순서로 정렬
        List<String> topKeywords = keywordFrequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3) // 상위 3개 키워드
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<String> defaultKeywords = List.of("배달의 민족", "스타벅스", "GS25"); // 고정 키워드
        defaultKeywords.stream()
                .filter(keyword -> !topKeywords.contains(keyword))
                .limit(3 - topKeywords.size())
                .forEach(topKeywords::add);

        return new HomeDto(nowUser.getNickname(), ownedCardCount, topKeywords);

    }

    // 결제 여부 변경
    public void getPayCardHistory(Users nowUser, Long chatId) throws CustomException {
        Optional<ChatHistory> optionalChatHistory = chatHistoryRepository.findById(chatId);
        ChatHistory chatHistory = optionalChatHistory.orElseThrow(() -> new CustomException(ResponseCode.CHAT_HISTORY_NOT_FOUND));
        int paymentAmount = chatHistory.getPaymentAmount();

        String cardName = chatHistory.getCardName();
        Optional<Card> optionalCard = cardRepository.findByName(cardName);
        Card card = optionalCard.orElseThrow(() -> new CustomException(ResponseCode.CARD_NOT_FOUND));
        OwnedCard ownedCard = ownedCardRepository.findOwnedCardByCardAndUsers(card, nowUser);
        boolean isMain = ownedCard.isMain(); // 주카드인 경우 실제 실적을 불러옴


        if (chatHistory.isHasPaid() == true) {
            chatHistory.setHasPaid(false);
            if (isMain == false) {
                ownedCard.setCurrentPerformance(ownedCard.getCurrentPerformance() - paymentAmount);
            }
        } else {
            chatHistory.setHasPaid(true);
            if (isMain == false) {
                ownedCard.setCurrentPerformance(ownedCard.getCurrentPerformance() + paymentAmount);
            }
        }

        chatHistoryRepository.save(chatHistory);
        ownedCardRepository.save(ownedCard);

        return;
    }

    // 알림 리스트 조회
    public AnnounceListDto getAnnounce(Users nowUser) throws CustomException {
        String nickname = nowUser.getNickname();

        LocalDate today = LocalDate.now();
        LocalDate thisWeek = today.minusDays(7);

        List<Announcement> announcementList = announcementRepository.findAnnouncementByUsers(nowUser);

        // 오늘 생성된 알림
        List<AnnounceDto> todayAnnounceDto = announcementList.stream()
                .filter(announcement -> announcement.getCreatedAt().toLocalDate().isEqual(today))
                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed())
                .map(AnnounceDto::new)
                .collect(Collectors.toList());

        // 7일 이내에 생성된 알림 (오늘 제외)
        List<AnnounceDto> recentAnnounceDto = announcementList.stream()
                .filter(announcement -> !announcement.getCreatedAt().toLocalDate().isEqual(today)
                        && announcement.getCreatedAt().toLocalDate().isAfter(thisWeek))
                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed())
                .map(AnnounceDto::new)
                .collect(Collectors.toList());

        long uncheckedcnt = announcementList.stream()
                .filter(announcement -> !announcement.isHasCheck()
                        && announcement.getCreatedAt().toLocalDate().isAfter(thisWeek))
                .count();

        return new AnnounceListDto(nickname, uncheckedcnt, todayAnnounceDto, recentAnnounceDto);
    }

    // 알림 상세 조회
    public AnnounceDetailDto getAnnounceDetail(Long announceId) throws CustomException {
        Optional<Announcement> optionalAnnouncement = announcementRepository.findById(announceId);
        Announcement announcement = optionalAnnouncement.orElseThrow(() -> new CustomException(ResponseCode.ANNOUNCEMENT_NOT_FOUND));
        announcement.setHasCheck(true);
        announcementRepository.save(announcement);
        return new AnnounceDetailDto(announcement);
    }
    public List<SearchStoresResponseDto> searchStores(SearchStoresRequestDto dto, Users nowuser) throws CustomException {
            // 단골가게 가져오기
            List<Favorite> favorites = favoriteRepository.findAllByUsers(nowuser).get();
            System.out.println(favorites);
            if(favorites.isEmpty()){
                throw new CustomException(ResponseCode.NO_FAVORITE_STORE);
            }

            double latitude = dto.getLatitude();
            double longitude = dto.getLongitude();

            // 단골 가게 검색 시작
            String url = "https://places.googleapis.com/v1/places:searchText";
            // headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("accept", "application/json");
            headers.add("X-Goog-Api-Key", apiKey);
            headers.add("X-Goog-FieldMask", "places.formattedAddress,places.location,places.displayName");

            List<SearchStoresResponseDto> resultList = new ArrayList<>();

            for (Favorite favorite : favorites) {
                // request body
                Map<String, Object> requestBody = new HashMap<String, Object>();
                requestBody.put("textQuery", favorite.getName());
                requestBody.put("maxResultCount", 10);
                requestBody.put("languageCode", "ko");
                Map<String, Object> locationBias = new HashMap<String, Object>();
                Map<String, Object> circle = new HashMap<String, Object>();
                Map<String, Object> center = new HashMap<String, Object>();
                center.put("latitude", latitude);
                center.put("longitude", longitude);
                circle.put("center", center);
                circle.put("radius", 500.0);
                locationBias.put("circle", circle);
                requestBody.put("locationBias", locationBias);

                GoogleMapPlaceResponseDto responsebody;
                try {

                    HttpEntity<Map<String, Object>> requestData = new HttpEntity<>(requestBody, headers);
                    ResponseEntity<GoogleMapPlaceResponseDto> responseEntity = restTemplate.postForEntity(url, requestData, GoogleMapPlaceResponseDto.class);
                    responsebody = responseEntity.getBody();
                } catch (Exception e){
                    throw new CustomException(ResponseCode.GOOGLE_MAP_SEARCH_PLACE_FAIL);
                }

                if (responsebody.getPlaces() == null){
                    throw new CustomException(ResponseCode.NO_SEARCHED_FAVORITE_STORE);
                }

                List<Place> placeList = responsebody.getPlaces();
                for( Place place : placeList){
                    SearchStoresResponseDto searchedResult = new SearchStoresResponseDto();

                    searchedResult.setStore(favorite.getName());
                    searchedResult.setStoreName(place.getDisplayName().getText());
                    searchedResult.setLatitude(place.getLocation().getLatitude());
                    searchedResult.setLongitude(place.getLocation().getLongitude());

                    resultList.add(searchedResult);
                }
            }
            return resultList;
    }
    public void postAnnounceFavorite(AnnounceFavoriteRequestDto dto, Users nowUser) throws CustomException {
        List<FCMToken> fcmTokens = fcmTokenRepository.findAllByUsers(nowUser);
        String keyword = dto.getStore();
        int paymentAmount=10000;
        String response = openaiService.cardRecommend(nowUser, keyword, paymentAmount);
        ObjectMapper objectMapper = new ObjectMapper();
        Integer cardId;
        Card card;
        String benefit;
        Integer discount;
        try {
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            cardId = (Integer) map.get("카드번호");
            card = cardRepository.findById(Long.valueOf(cardId)).orElse(null);
            benefit = (String) map.get("혜택 정보");
            discount = (Integer) map.get("할인 금액");

        } catch ( JsonProcessingException e) {
            throw new CustomException(ResponseCode.FAILED_TO_OPENAI_RECOMMEND);
        }

        String contents = dto.getStoreName()+" 근처시군요.\n"+card.getName()+" 사용해 보세요!";
        String title = dto.getStoreName()+" 근처시군요";
        String content = card.getName()+" 사용해 보세요!";
        String moreInfo = dto.getLatitude()+", "+dto.getLongitude();
        Announcement announcement = Announcement.builder()
                .users(nowUser)
                .type(0)
                .content(contents)
                .moreInfo(moreInfo)
                .hasCheck(false)
                .build();
        announcementRepository.save(announcement);
        for ( FCMToken fcmToken : fcmTokens){

            String token = fcmToken.getToken();
            firebaseCloudMessageService.sendNotification(new AnnouncementRequestDto(token,title,content));
        }
    }
    public void postBeaconAnnouncement(BeaconRequestDto dto, Users nowUser)throws CustomException {
        List<FCMToken> fcmTokens = fcmTokenRepository.findAllByUsers(nowUser);

        Beacon beacon = beaconRepository.findAllByProximityUUIDAndMajorAndMinor(dto.getProximityUUID(),dto.getMajor(),dto.getMinor());

        String keyword = beacon.getName();
        int paymentAmount=10000;
        String response = openaiService.cardRecommend(nowUser, keyword, paymentAmount);
        ObjectMapper objectMapper = new ObjectMapper();
        Integer cardId;
        Card card;
        String benefit;
        Integer discount;
        try {
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            cardId = (Integer) map.get("카드번호");
            card = cardRepository.findById(Long.valueOf(cardId)).orElse(null);
            benefit = (String) map.get("혜택 정보");
            discount = (Integer) map.get("할인 금액");

        } catch ( JsonProcessingException e) {
            throw new CustomException(ResponseCode.FAILED_TO_OPENAI_RECOMMEND);
        }

        String title = beacon.getStore()+" 근처시군요";
        String content = card.getName()+" 사용해 보세요!";
        String contents = beacon.getStore()+" 근처시군요.\n"+card.getName()+" 사용해 보세요!";

        String moreInfo = beacon.getLatitude()+", "+beacon.getLongitude();
        Announcement announcement = Announcement.builder()
                .users(nowUser)
                .type(0)
                .content(contents)
                .moreInfo(moreInfo)
                .hasCheck(false)
                .build();
        announcementRepository.save(announcement);
        for ( FCMToken fcmToken : fcmTokens){
            String token = fcmToken.getToken();
            firebaseCloudMessageService.sendNotification(new AnnouncementRequestDto(token,title,content));
        }

    }
}