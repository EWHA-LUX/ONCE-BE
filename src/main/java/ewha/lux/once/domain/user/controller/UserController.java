package ewha.lux.once.domain.user.controller;

import ewha.lux.once.domain.user.dto.LoginResponseDto;
import ewha.lux.once.domain.user.dto.SignInRequestDto;
import ewha.lux.once.domain.user.dto.SignupRequestDto;
import ewha.lux.once.domain.user.dto.postSearchCardListRequestDto;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.domain.user.service.UserService;
import ewha.lux.once.global.common.CommonResponse;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.common.UserAccount;
import ewha.lux.once.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;

@Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("user")
public class UserController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    // [Post] 회원가입
    @PostMapping("/signup")
    public CommonResponse<?> signup(@RequestBody SignupRequestDto request) throws ParseException {
        try {
            Users users = userService.signup(request);

            String accessToken = jwtProvider.generateAccessToken(users.getLoginId());
            String refreshToken = jwtProvider.generateRefreshToken(users.getLoginId());

            LoginResponseDto loginResponseDto = new LoginResponseDto(users.getId(), accessToken, refreshToken);
            return new CommonResponse<>(ResponseCode.SUCCESS, loginResponseDto);
        } catch (Exception e) {
            return new CommonResponse<>(ResponseCode.SUCCESS); // ** 예외 처리 필요 **
        }
    }

    // [Post] 로그인
    @PostMapping("/login")
    public CommonResponse<?> signin(@RequestBody SignInRequestDto request) {
        try {
            Users user = userService.authenticate(request);

            String accessToken = jwtProvider.generateAccessToken(user.getLoginId());
            String refreshToken = jwtProvider.generateRefreshToken(user.getLoginId());

            LoginResponseDto loginResponseDto = new LoginResponseDto(user.getId(), accessToken, refreshToken);

            return new CommonResponse<>(ResponseCode.SUCCESS, loginResponseDto);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Delete] 회원 탈퇴
    @DeleteMapping("/quit")
    @ResponseBody
    public CommonResponse<?> quitUsers(@AuthenticationPrincipal UserAccount userAccount) {
        userService.deleteUsers(userAccount.getUsers());
        return new CommonResponse<>(ResponseCode.SUCCESS);
    }

    // [Get] 회원 정보 조회
    @GetMapping("/edit")
    @ResponseBody
    public CommonResponse<?> userEdit(@AuthenticationPrincipal UserAccount userAccount) {
        return new CommonResponse<>(ResponseCode.SUCCESS, userService.getUserEdit(userAccount.getUsers()));
    }

    // [Get] 카드사별 카드 검색
    @GetMapping("/card/search")
    @ResponseBody
    public CommonResponse<?> searchCard(@Param("code") String code) {
        return new CommonResponse<>(ResponseCode.SUCCESS, userService.getSearchCard(code));
    }

    // [Post] 카드 등록
    @PostMapping("/card")
    @ResponseBody
    public CommonResponse<?> postSearchCard(@AuthenticationPrincipal UserAccount userAccount, @RequestBody postSearchCardListRequestDto request) {
        userService.postSearchCard(userAccount.getUsers(), request);
        return new CommonResponse<>(ResponseCode.SUCCESS);
    }

    // [Patch] 프로필 등록
    @PatchMapping(value = "/edit/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public CommonResponse<?> editProfile(@AuthenticationPrincipal UserAccount userAccount, HttpServletRequest request, @RequestParam(value = "userProfileImg") MultipartFile userProfileImg) throws IOException {
        return new CommonResponse<>(ResponseCode.SUCCESS, userService.patchEditProfile(userAccount.getUsers(), userProfileImg));
    }
}


