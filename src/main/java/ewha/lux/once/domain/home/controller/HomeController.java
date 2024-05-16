package ewha.lux.once.domain.home.controller;

import ewha.lux.once.domain.card.dto.SearchStoresRequestDto;
import ewha.lux.once.domain.home.dto.PromptRequestDto;
import ewha.lux.once.domain.home.dto.TestRequestDto;
import ewha.lux.once.domain.home.dto.TestResponseDto;
import ewha.lux.once.domain.home.service.HomeService;
import ewha.lux.once.global.common.CommonResponse;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.common.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("home")
public class HomeController {

    private final HomeService homeService;

    // [Get] 챗봇 카드 추천
    @GetMapping()
    public CommonResponse<?> homeChat(@AuthenticationPrincipal UserAccount userAccount, @RequestParam(name = "keyword") String keyword, @RequestParam(name = "paymentAmount") int paymentAmount) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, homeService.getHomeChat(userAccount.getUsers(), keyword, paymentAmount));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 홈 화면 기본 정보 조회
    @GetMapping("/basic")
    public CommonResponse<?> home(@AuthenticationPrincipal UserAccount userAccount) {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, homeService.getHome(userAccount.getUsers()));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Patch] 결제 여부 변경
    @PatchMapping("/{chat_id}")
    public CommonResponse<?> payCardHistory(@AuthenticationPrincipal UserAccount userAccount, @PathVariable Long chat_id) {
        try {
            homeService.getPayCardHistory(userAccount.getUsers(), chat_id);
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 알림 list 조회
    @GetMapping("/announcement")
    public CommonResponse<?> announce(@AuthenticationPrincipal UserAccount userAccount) {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, homeService.getAnnounce(userAccount.getUsers()));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 알림 상세 조회
    @GetMapping("/announcement/{announceId}")
    public CommonResponse<?> announcedetail(@PathVariable Long announceId) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, homeService.getAnnounceDetail(announceId));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 사용자 근처 단골가게 조회
    @GetMapping("/gps")
    @ResponseBody
    public CommonResponse<?> nearFavorite(@AuthenticationPrincipal UserAccount user, @RequestBody SearchStoresRequestDto nearFavoriteRequestDto){
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, homeService.searchStores(nearFavoriteRequestDto, user.getUsers()));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // ** 추후 삭제해야 함 - 테스트용 ** ==================================
    // [Get] 프롬프트 생성
    @GetMapping("/prompt")
    @ResponseBody
    public String generatePrompt(@RequestBody PromptRequestDto promptRequestDto){
        try{
            return homeService.getPrompt(promptRequestDto);
        } catch (CustomException e){
            return "실패";
        }
    }

    // [Get] 카드 추천 test
    @GetMapping("/test")
    @ResponseBody
    public TestResponseDto testrecommend (@RequestBody TestRequestDto testRequestDto){
        try{
            return homeService.testRecommend(testRequestDto);
        } catch (CustomException e){
            TestResponseDto responseDto = TestResponseDto.builder()
                    .cardId(0)
                    .benefit("")
                    .discount(0)
                    .build();
            return responseDto;
        }
    }

    // ============================================================

}
