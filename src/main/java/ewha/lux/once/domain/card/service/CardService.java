package ewha.lux.once.domain.card.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import ewha.lux.once.domain.card.dto.*;
import ewha.lux.once.domain.card.entity.BenefitSummary;
import ewha.lux.once.domain.card.entity.Card;
import ewha.lux.once.domain.card.entity.ConnectedCardCompany;
import ewha.lux.once.domain.card.entity.OwnedCard;
import ewha.lux.once.domain.home.dto.BenefitDto;
import ewha.lux.once.domain.home.entity.ChatHistory;
import ewha.lux.once.domain.home.service.CODEFAPIService;
import ewha.lux.once.domain.home.service.CODEFAsyncService;
import ewha.lux.once.domain.home.service.OpenaiService;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ewha.lux.once.domain.home.service.HomeService.getCategory;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final OwnedCardRepository ownedCardRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UsersRepository usersRepository;
    private final CardRepository cardRepository;
    private final CardCompanyRepository cardCompanyRepository;
    private final BenefitSummaryRepository benefitSummaryRepository;
    private final ConnectedCardCompanyRepository connectedCardCompanyRepository;
    private final CODEFAPIService codefapi;
    private final CODEFAsyncService codefAsyncService;
    private final OpenaiService openaiService;
    @Value("${google-map.api-key}")
    private String apiKey;

    public MyWalletResponseDto.MyWalletProfileDto getMyWalletInfo(Users nowUser) throws CustomException {
        List<OwnedCard> ownedCards = ownedCardRepository.findOwnedCardByUsers(nowUser);

        if (ownedCards.isEmpty()) {
            throw new CustomException(ResponseCode.OWNED_CARD_NOT_FOUND);
        }

        List<MyWalletResponseDto.OwnedCardListDto> ownedCardList = ownedCards.stream()
                .map(ownedCard -> {
                    List<BenefitSummary> cardSummary = benefitSummaryRepository.findByCard(ownedCard.getCard());
                    List<MyWalletResponseDto.CardBenefitListDto> cardBenefitList = new ArrayList<>();
                    for (BenefitSummary summary : cardSummary) {
                        String category= getCategory(summary.getBenefitField()+summary.getBenefitContents());
                        cardBenefitList.add(new MyWalletResponseDto.CardBenefitListDto(category,summary.getBenefitField(), summary.getBenefitContents()));
                    }
                    return new MyWalletResponseDto.OwnedCardListDto(
                            ownedCard.getId(),
                            ownedCard.getCard().getName(),
                            ownedCard.getCard().getCardCompany().getName(),
                            ownedCard.getCard().getType().ordinal(),
                            ownedCard.getCard().getImgUrl(),
                            ownedCard.isMain(),
                            ownedCard.getPerformanceCondition(),
                            ownedCard.getCurrentPerformance(),
                            Math.max(ownedCard.getPerformanceCondition() - ownedCard.getCurrentPerformance(), 0),
                            cardBenefitList
                    );
                })
                .toList();

        return MyWalletResponseDto.MyWalletProfileDto.builder()
                .nickname(nowUser.getNickname())
                .ownedCardList(ownedCardList)
                .build();
    }

    public void postCardPerformance(Users nowUser, CardPerformanceRequestDto cardPerformanceRequestDto) throws CustomException {
        OwnedCard ownedCard = ownedCardRepository.findOwnedCardByIdAndUsers(cardPerformanceRequestDto.getOwnedCardId(), nowUser);
        if (ownedCard != null) {
            ownedCard.setPerformanceCondition(cardPerformanceRequestDto.getPerformanceCondition());
            ownedCardRepository.save(ownedCard);
        } else {
            throw new CustomException(ResponseCode.INVALID_OWNED_CARD);
        }
        return;
    }

    public MontlyBenefitResponseDto.MontlyBenefitProfileDto getMontlyBenefitInfo(Users nowUser, int month) throws CustomException {
        List<ChatHistory> chatHistories = chatHistoryRepository.findByUsers(nowUser);
        if (chatHistories.isEmpty()) {
            new CustomException(ResponseCode.CHAT_HISTORY_NOT_FOUND);
        }

        int receivedSum = chatHistories.stream()
                .filter(chatHistory -> chatHistory.isHasPaid() && chatHistory.getCreatedAt().getMonthValue() == month)
                .mapToInt(ChatHistory::getDiscount)
                .sum();

        List<String> categories = chatHistories.stream()
                .filter(chatHistory -> chatHistory.isHasPaid() && chatHistory.getCreatedAt().getMonthValue() == month)
                .map(ChatHistory::getCategory)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Integer> categoryGetDiscount = chatHistories.stream()
                .filter(chatHistory -> chatHistory.isHasPaid() && chatHistory.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.groupingBy(ChatHistory::getCategory, Collectors.summingInt(ChatHistory::getDiscount)));

        Map<String, Integer> categoryGetPaymentAmount = chatHistories.stream()
                .filter(chatHistory -> chatHistory.isHasPaid() && chatHistory.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.groupingBy(
                        ChatHistory::getCategory,
                        Collectors.summingInt(chatHistory -> chatHistory.getPaymentAmount() - chatHistory.getDiscount())
                ));

        List<MontlyBenefitResponseDto.BenefitListDto> benefitList = categories.stream()
                .map(category -> new MontlyBenefitResponseDto.BenefitListDto(
                        category,
                        categoryGetDiscount.getOrDefault(category, 0),
                        categoryGetPaymentAmount.getOrDefault(category, 0)
                ))
                .collect(Collectors.toList());


        return MontlyBenefitResponseDto.MontlyBenefitProfileDto.builder()
                .month(month)
                .receivedSum(receivedSum)
                .benefitGoal(nowUser.getBenefitGoal())
                .remainBenefit(nowUser.getBenefitGoal() - receivedSum)
                .benefitList(benefitList)
                .build();
    }

    public void postBenefitGoal(Users nowUser, CardGoalRequestDto cardGoalRequestDto) throws CustomException {
        nowUser.setCardGoal(cardGoalRequestDto.getBenefitGoal());
        usersRepository.save(nowUser);
        return;
    }

    private List<MyWalletResponseDto.CardBenefitListDto> splitCardSummary(String cardSummary) {
        List<MyWalletResponseDto.CardBenefitListDto> cardBenefitList = new ArrayList<>();
        String[] sections = cardSummary.split("###");
        for (String section : sections) {
            String[] parts = section.split("//");
            if (parts.length == 2) {
                String category = parts[0].trim();
                String benefit = parts[1].trim();
                cardBenefitList.add(new MyWalletResponseDto.CardBenefitListDto("카테고리",category, benefit));
            }
        }
        return cardBenefitList;
    }

    public List<CodefCardListResponseDto> getCodefCardList(Users nowUser, CodefCardListRequestDto codefCardListRequestDto) throws CustomException {
        // 커넥티드 아이디
        String connectedId;
        if (nowUser.getConnectedId() == null) {  // 저장되어있지 않은 경우
            // 계정 생성
            connectedId = codefapi.CreateConnectedID(codefCardListRequestDto);
            nowUser.setConnectedId(connectedId);
            usersRepository.save(nowUser);
        } else if (codefapi.IsRegistered(codefCardListRequestDto.getCode(), nowUser.getConnectedId()) == "0") {  // 커넥티드 아이디가 해당 카드사와 연결되어있지 않은 경우
            // 계정 추가
            connectedId = codefapi.AddToConnectedID(nowUser, codefCardListRequestDto);
        } else {
            connectedId = nowUser.getConnectedId();
        }
        JSONArray dataArray = codefapi.GetCardList(codefCardListRequestDto.getCode(), connectedId);

        List<CodefCardListResponseDto> cardDTOList = new ArrayList<>();
        for (Object obj : dataArray) {
            JSONObject dataObject = (JSONObject) obj;
            String resCardName = (String) dataObject.get("resCardName");
            String resImageLink = (String) dataObject.get("resImageLink");

            CodefCardListResponseDto cardDTO = new CodefCardListResponseDto(resCardName, resImageLink);
            cardDTOList.add(cardDTO);
        }
        String companyname= cardCompanyRepository.findByCode(codefCardListRequestDto.getCode()).get().getName();
        Optional<ConnectedCardCompany> existingRecord = connectedCardCompanyRepository.findByUsersAndCardCompany(nowUser, companyname);

        if (existingRecord.isPresent()) {
            ConnectedCardCompany record = existingRecord.get();
            record.setConnectedAt(LocalDateTime.now()); // 연결 일자 업데이트
            connectedCardCompanyRepository.save(record);
        } else {
            ConnectedCardCompany record = ConnectedCardCompany.builder()
                    .users(nowUser)
                    .cardCompany(companyname)
                    .connectedAt(LocalDateTime.now())
                    .build();
            connectedCardCompanyRepository.save(record);
        }
        return cardDTOList;

    }

    // 주카드 등록
    public void postRegisterCard(Users nowUser, MainCardRequestDto mainCardRequestDto) throws CustomException {
        // 카드
        Optional<Card> optionalCard = cardRepository.findCardByName(mainCardRequestDto.getCardName());
        Card card = optionalCard.orElseThrow(() -> new CustomException(ResponseCode.CARD_NOT_FOUND));
        // 보유 카드 가져오기
        Optional<OwnedCard> optionalOwnedCard = ownedCardRepository.findOwnedCardByUsersAndCard(nowUser, card);
        OwnedCard ownedCard;
        if (optionalOwnedCard.isPresent()) {
            // 이미 등록된 경우
            ownedCard = optionalOwnedCard.get();
        } else {
            ownedCard = OwnedCard.builder()
                    .users(nowUser)
                    .card(card)
                    .build();
        }

        String connectedId = nowUser.getConnectedId();

        // 실적 조회
        HashMap<String, Object> performResult = codefapi.Performace(mainCardRequestDto.getCode(), connectedId, mainCardRequestDto.getCardName());
        int performanceCondition = (int) performResult.get("performanceCondition");
        int currentPerformance = (int) performResult.get("currentPerformance");
        String cardNo = (String) performResult.get("resCardNo");

        // 단골가게 저장
        codefAsyncService.saveFavorite(mainCardRequestDto.getCode(), connectedId, ownedCard, nowUser, cardNo);

        ownedCard.setMaincard();
        ownedCard.setPerformanceCondition(performanceCondition);
        ownedCard.setCurrentPerformance(currentPerformance);

        ownedCardRepository.save(ownedCard);
    }

    // 보유 주카드 실적 업데이트
    public void updateOwnedCardsPerformance(Users nowUser) throws CustomException {
        codefAsyncService.updateOwnedCardsPerformanceCodef(nowUser);
    }

    // 매주 월요일 06:00 AM 카드 혜택 정보 요약 작업
    @Scheduled(cron = "0 0 6 ? * 1")
    public void updateBenefitSummary() throws CustomException, JsonProcessingException {

        List<Card> cardList = cardRepository.findAll();

        int index = 1;
        for (Card card : cardList) {
            // 기존의 BenefitSummary 삭제
            List<BenefitSummary> existingSummaries = benefitSummaryRepository.findByCard(card);
            benefitSummaryRepository.deleteAll(existingSummaries);

            log.info("[" + card.getName() + "] - 카드 혜택 요약 중... (" + index + "/" + cardList.size() + ")");
            BenefitDto[] benefitJson = openaiService.gptBenefitSummary(card.getBenefits());

            for (BenefitDto benefit : benefitJson) {
                BenefitSummary benefitSummary = BenefitSummary.builder()
                        .benefitField(benefit.getBenefit_field())
                        .benefitContents(benefit.getContent())
                        .card(card)
                        .build();

                benefitSummaryRepository.save(benefitSummary);
            }
            index++;
        }
        log.info("전체 카드 혜택 요약 완료");
    }

    public List<ConnectedCardCompanyResponseDto> getConnectedCardCompany(Users nowUser)throws CustomException{
        List<ConnectedCardCompany> connectedList = connectedCardCompanyRepository.findAllByUsers(nowUser);
        if (connectedList.isEmpty()) {
            throw new CustomException(ResponseCode.NO_CONNECTED_CARD_COMPANY);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        List<ConnectedCardCompanyResponseDto> responseDtoList = connectedList.stream()
                .map(connected -> {
                    String companyName = connected.getCardCompany();
                    String connectedAt = connected.getConnectedAt().format(formatter);
                    return new ConnectedCardCompanyResponseDto(companyName, connectedAt);
                })
                .collect(Collectors.toList());
        return responseDtoList;
    }

    // ** 추후 삭제해야 함 - 테스트용 ** ==================================
    // GET /card/test/summary : 전체 카드 혜택 요약
    public void updateBenefitSummaryTest(String prompt, String model_name) throws CustomException, JsonProcessingException {

        List<Card> cardList = cardRepository.findAll();

        int index = 1;
        for (Card card : cardList) {
            // 기존의 BenefitSummary 삭제
            List<BenefitSummary> existingSummaries = benefitSummaryRepository.findByCard(card);
            benefitSummaryRepository.deleteAll(existingSummaries);

            log.info("[" + card.getName() + "] - 카드 혜택 요약 중... (" + index + "/" + cardList.size() + ")");

            BenefitDto[] benefitJson = openaiService.gptBenefitSummaryTest(card.getBenefits(), prompt, model_name);

            if (benefitJson == null) {
                System.out.println("===========PASS=========" + card.getName());
//                cardRepository.delete(card);
                continue;
            }
            for (BenefitDto benefit : benefitJson) {
                BenefitSummary benefitSummary = BenefitSummary.builder()
                        .benefitField(benefit.getBenefit_field())
                        .benefitContents(benefit.getContent())
                        .card(card)
                        .build();

                benefitSummaryRepository.save(benefitSummary);
            }
            index++;
        }
        log.info("전체 카드 혜택 요약 완료");
    }

    /*
        GET /card/test/summary/index : 일부 카드 혜택 요약
        "prompt": 프롬프트 내용,
        "model_name": open_ai model,
        "start_index": 시작 인덱스,
        "end_index": 종료 인덱스 (포함)
     */
    public void updateBenefitSummaryTestByIndex(String prompt, String model_name, long start_index, long end_index) throws CustomException, JsonProcessingException {

        List<Card> cardList = cardRepository.findByIdBetween(start_index, end_index);


        int index = 1;
        for (Card card : cardList) {
            // 기존의 BenefitSummary 삭제
            List<BenefitSummary> existingSummaries = benefitSummaryRepository.findByCard(card);
            benefitSummaryRepository.deleteAll(existingSummaries);

            log.info("[" + card.getName() + " (card_id=" + card.getId() + ")] - 카드 혜택 요약 중... (" + index + "/" + cardList.size() + ")");

            BenefitDto[] benefitJson = openaiService.gptBenefitSummaryTest(card.getBenefits(), prompt, model_name);

            if (benefitJson == null) {
                System.out.println("===========PASS=========" + card.getName());
//                cardRepository.delete(card);
                continue;
            }
            for (BenefitDto benefit : benefitJson) {
                BenefitSummary benefitSummary = BenefitSummary.builder()
                        .benefitField(benefit.getBenefit_field())
                        .benefitContents(benefit.getContent())
                        .card(card)
                        .build();

                benefitSummaryRepository.save(benefitSummary);
            }
            index++;
        }
        log.info("일부 카드 혜택 요약 완료");
    }
    // ============================================================

}
