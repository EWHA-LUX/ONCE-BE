package ewha.lux.once.domain.card.service;

import ewha.lux.once.domain.card.dto.CardPerformanceRequestDto;
import ewha.lux.once.domain.card.dto.MyWalletResponseDto;
import ewha.lux.once.domain.card.entity.OwnedCard;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.repository.OwnedCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final OwnedCardRepository ownedCardRepository;

    public MyWalletResponseDto.MyWalletProfileDto getMyWalletInfo(Users nowUser) throws CustomException {
        List<OwnedCard> ownedCards = ownedCardRepository.findOwnedCardByUsers(nowUser);

        if (ownedCards.isEmpty()) {
            throw new CustomException(ResponseCode.OWNED_CARD_NOT_FOUND);
        }

        List<MyWalletResponseDto.OwnedCardListDto> ownedCardList = ownedCards.stream()
                .map(ownedCard -> {
                    String cardSummary = ownedCard.getCard().getBenefitSummary();
                    List<MyWalletResponseDto.CardBenefitListDto> cardBenefitList = splitCardSummary(cardSummary);
                    return new MyWalletResponseDto.OwnedCardListDto(
                            ownedCard.getId(),
                            ownedCard.getCard().getName(),
                            ownedCard.getCard().getType().ordinal(),
                            ownedCard.getCard().getImgUrl(),
                            ownedCard.isMain(),
                            ownedCard.getPerformanceCondition(),
                            ownedCard.getCurrentPerformance(),
                            ownedCard.getPerformanceCondition() - ownedCard.getCurrentPerformance(),
                            cardBenefitList
                    );
                })
                .toList();

        return MyWalletResponseDto.MyWalletProfileDto.builder()
                .ownedCardList(ownedCardList)
                .build();
    }

    public void postCardPerformance(Users nowUser, CardPerformanceRequestDto cardPerformanceRequestDto) throws CustomException {
        OwnedCard ownedCard = ownedCardRepository.findOwnedCardByCardIdAndUsers(cardPerformanceRequestDto.getOwnedCardId(), nowUser);
        if(ownedCard != null) {
            ownedCard.setPerformanceCondition(cardPerformanceRequestDto.getPerformanceCondition());
            ownedCardRepository.save(ownedCard);
        } else {
            throw new CustomException(ResponseCode.INVALID_OWNED_CARD);
        }
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
                cardBenefitList.add(new MyWalletResponseDto.CardBenefitListDto(category, benefit));
            }
        }
        return cardBenefitList;
    }
}