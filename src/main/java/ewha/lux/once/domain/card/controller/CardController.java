package ewha.lux.once.domain.card.controller;

import ewha.lux.once.domain.card.dto.CardGoalRequestDto;
import ewha.lux.once.domain.card.dto.CardPerformanceRequestDto;
import ewha.lux.once.domain.card.dto.CodefCardListRequestDto;
import ewha.lux.once.domain.card.service.CardService;
import ewha.lux.once.domain.card.dto.MainCardRequestDto;
import ewha.lux.once.domain.card.service.CrawlingService;
import ewha.lux.once.global.common.CommonResponse;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.common.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("card")
public class CardController {

    private final CardService cardService;
    private final CrawlingService crawlingService;

    @GetMapping("test")
    @ResponseBody
    public CommonResponse<?> testtest(@AuthenticationPrincipal UserAccount user) {
        try {
            crawlingService.cardCrawlingg();
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 마이월렛 조회
    @GetMapping("")
    @ResponseBody
    public CommonResponse<?> myWalletInfo(@AuthenticationPrincipal UserAccount user) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, cardService.getMyWalletInfo(user.getUsers()));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] 주카드 아닌 카드 실적 입력
    @PostMapping("/performance")
    @ResponseBody
    public CommonResponse<?> cardPerformance(@AuthenticationPrincipal UserAccount user, @RequestBody CardPerformanceRequestDto cardPerformanceRequestDto) {
        try {
            cardService.postCardPerformance(user.getUsers(), cardPerformanceRequestDto);
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 월별 혜택 조회
    @GetMapping("/benefit")
    @ResponseBody
    public CommonResponse<?> montlyBenefitInfo(@AuthenticationPrincipal UserAccount user, @RequestParam int month) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, cardService.getMontlyBenefitInfo(user.getUsers(), month));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] 카드 목표 혜택 금액 입력
    @PostMapping("/benefitgoal")
    @ResponseBody
    public CommonResponse<?> cardGoal(@AuthenticationPrincipal UserAccount user, @RequestBody CardGoalRequestDto cardGoalRequestDto) {
        try {
            cardService.postBenefitGoal(user.getUsers(), cardGoalRequestDto);
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }
    // [Get] CODEF 보유 카드 조회
    @GetMapping("/list")
    @ResponseBody
    public CommonResponse<?> codefCardList (@AuthenticationPrincipal UserAccount user, @RequestBody CodefCardListRequestDto codefCardListRequestDto) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS,cardService.getCodefCardList(user.getUsers(), codefCardListRequestDto));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] 주카드 등록
    @PostMapping("/main")
    @ResponseBody
    public CommonResponse<?> registerMainCard (@AuthenticationPrincipal UserAccount user, @RequestBody MainCardRequestDto mainCardRequestDto) {
        try {
            cardService.postRegisterCard(user.getUsers(), mainCardRequestDto);
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 주카드 실적 업데이트
    @GetMapping("/main/performance")
    @ResponseBody
    public CommonResponse<?> registerMainCard (@AuthenticationPrincipal UserAccount user) {
        try {
            cardService.updateOwnedCardsPerformance(user.getUsers());
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }
}
