package ewha.lux.once.domain.user.controller;

import ewha.lux.once.domain.home.dto.FCMTokenDto;
import ewha.lux.once.domain.home.service.FirebaseCloudMessageService;
import ewha.lux.once.domain.user.dto.*;
import ewha.lux.once.domain.user.service.UserService;
import ewha.lux.once.global.common.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ewha.lux.once.global.security.JwtProvider;
import java.io.IOException;
import java.text.ParseException;

@Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("user")
public class UserController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final FirebaseCloudMessageService firebaseCloudMessageService;

    // [Post] 회원가입
    @PostMapping("/signup")
    public CommonResponse<?> signup(@RequestBody SignupRequestDto request) throws ParseException {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.signup(request));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] 로그인
    @PostMapping("/login")
    public CommonResponse<?> signin(@RequestBody SignInRequestDto request) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.authenticate(request));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }
    // [Post] 자동로그인
    @PostMapping("/auto")
    public CommonResponse<?> autologinPage() {
        return new CommonResponse<>(ResponseCode.VALID_ACCESS_TOKEN);
    }

    // [Post] 로그아웃
    @PostMapping("/logout")
    @ResponseBody
    public CommonResponse<?> logoutPage(HttpServletRequest request,@AuthenticationPrincipal UserAccount userAccount) {
        try {
            userService.postLogout(request, userAccount.getUsers());
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }



    // [Delete] 회원 탈퇴
    @DeleteMapping("/quit")
    @ResponseBody
    public CommonResponse<?> quitUsers(@AuthenticationPrincipal UserAccount userAccount) {
        try{
            userService.deleteUsers(userAccount.getUsers());
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 회원 정보 조회
    @GetMapping("/edit")
    @ResponseBody
    public CommonResponse<?> userEdit(@AuthenticationPrincipal UserAccount userAccount) {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.getUserEdit(userAccount.getUsers()));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 카드사별 카드 검색
    @GetMapping("/card/search")
    @ResponseBody
    public CommonResponse<?> searchCard(@Param("code") String code) throws CustomException {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.getSearchCard(code));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 카드 이름 검색
    @GetMapping("/card/searchname")
    @ResponseBody
    public CommonResponse<?> searchCardName(@Param("name") String name,@Param("code") String code) {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.getSearchCardName(name, code));
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }


    // [Post] 카드 등록
    @PostMapping("/card")
    @ResponseBody
    public CommonResponse<?> postSearchCard(@AuthenticationPrincipal UserAccount userAccount, @RequestBody postSearchCardListRequestDto request) {
        try {
            userService.postSearchCard(userAccount.getUsers(), request);
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Patch] 프로필 등록
    @PatchMapping(value = "/edit/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public CommonResponse<?> editProfile(@AuthenticationPrincipal UserAccount userAccount, @RequestParam(value = "userProfileImg") MultipartFile userProfileImg) throws IOException {
        try{
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.patchEditProfile(userAccount.getUsers(), userProfileImg));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Get] 아이디 중복 확인
    @GetMapping(value = "/duplicate")
    @ResponseBody
    public CommonResponse<?> idDuplicateCheck(@Param("loginId") String loginId) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.getIdDuplicateCheck(loginId));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] 비밀번호 확인
    @PostMapping(value = "/edit/pw")
    @ResponseBody
    public CommonResponse<?> checkPassword(@AuthenticationPrincipal UserAccount userAccount, @RequestBody ChangePasswordDto checkPasswordRequestDto) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.postCheckPassword(userAccount.getUsers(), checkPasswordRequestDto));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Patch] 비밀번호 변경
    @PatchMapping(value = "/edit/pw")
    @ResponseBody
    public CommonResponse<?> changePassword(@AuthenticationPrincipal UserAccount userAccount, @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.patchChangePassword(userAccount.getUsers(), changePasswordDto));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Patch] 회원 정보 수정
    @PatchMapping(value = "/edit")
    @ResponseBody
    public CommonResponse<?> editUserInfo(@AuthenticationPrincipal UserAccount userAccount, @RequestBody EditUserInfoRequestDto editUserInfoRequestDto) {
        try {
            return new CommonResponse<>(ResponseCode.SUCCESS, userService.patchEditUserInfo(userAccount.getUsers(), editUserInfoRequestDto));
        } catch (CustomException e){
            return new CommonResponse<>(e.getStatus());
        }
    }

    // [Post] FCM Token 등록
    @PostMapping("/token")
    public CommonResponse<?> saveFCMToken(@AuthenticationPrincipal UserAccount userAccount,@RequestBody FCMTokenDto fcmTokenDto) {
        try {
            firebaseCloudMessageService.postFCMToken(userAccount.getUsers(), fcmTokenDto.getToken());
            return new CommonResponse<>(ResponseCode.SUCCESS);
        } catch (CustomException e) {
            return new CommonResponse<>(e.getStatus());
        }
    }

}


