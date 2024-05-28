package ewha.lux.once.domain.user.service;

import ewha.lux.once.domain.card.entity.*;
import ewha.lux.once.domain.home.entity.Announcement;
import ewha.lux.once.domain.home.entity.ChatHistory;
import ewha.lux.once.domain.home.entity.FCMToken;
import ewha.lux.once.domain.home.entity.Favorite;
import ewha.lux.once.domain.user.dto.*;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import ewha.lux.once.global.security.JwtAuthFilter;
import ewha.lux.once.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import ewha.lux.once.global.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final CardRepository cardRepository;
    private final CardCompanyRepository cardCompanyRepository;
    private final OwnedCardRepository ownedCardRepository;

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtProvider jwtProvider;

    private final AnnouncementRepository announcementRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ConnectedCardCompanyRepository connectedCardCompanyRepository;
    private final FavoriteRepository favoriteRepository;
    private final FCMTokenRepository fcmTokenRepository;

    private final S3Uploader s3Uploader;
    private final RedisTemplate redisTemplate;
    private final RedisService redisService;
    private static final String REFRESH_TOKEN_PREFIX = "refreshToken:";

    public LoginResponseDto signup(SignupRequestDto request) throws CustomException, ParseException {
        String loginId = request.getLoginId();
        String username = request.getUsername();
        String password = request.getPassword();
        String nickname = request.getNickname();
        String phone = request.getUserPhoneNum();
        String birth = request.getBirthday();

        if (usersRepository.existsByLoginId(loginId)) {
            throw new CustomException(ResponseCode.DUPLICATED_USER_NAME);
        }

        password = passwordEncoder.encode(password);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String basicProfileImgUrl = "https://once-s3.s3.ap-northeast-2.amazonaws.com/profileImg/basic-profile.png";

        Users.UsersBuilder usersBuilder = Users.builder()
                .loginId(loginId)
                .username(username)
                .nickname(nickname)
                .password(password)
                .lastLogin(now)
                .profileImg(basicProfileImgUrl);

        // phone 값이 존재하는 경우에만 설정
        if (StringUtils.hasText(phone)) {
            usersBuilder.phone(phone);
        }

        // birthday 값이 존재하는 경우에만 설정
        if (StringUtils.hasText(birth)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
            Date birthday = dateFormat.parse(birth);
            usersBuilder.birthday(birthday);
        }

        Users newUser = usersRepository.save(usersBuilder.benefitGoal(100000).build());

        String accessToken = jwtProvider.createAccessToken(newUser.getLoginId());
        String refreshToken = jwtProvider.createRefreshToken(newUser.getLoginId());

        LoginResponseDto loginResponseDto = new LoginResponseDto(newUser.getId(), accessToken, refreshToken);
        redisService.setRefreshValueWithTTL(newUser.getId().toString(), refreshToken, 14L, TimeUnit.DAYS);

        return loginResponseDto;
    }

    public LoginResponseDto authenticate(SignInRequestDto request) throws CustomException {
        String loginId = request.getLoginId();
        String password = request.getPassword();

        Optional<Users> optionalUsers = usersRepository.findByLoginId(loginId);
        Users users = optionalUsers.orElseThrow(() -> new CustomException(ResponseCode.INVALID_USER_ID));

        if (!passwordEncoder.matches(password, users.getPassword())) {
            throw new CustomException(ResponseCode.FAILED_TO_LOGIN);
        }

        users.setLastLogin();
        usersRepository.save(users);

        String accessToken = jwtProvider.createAccessToken(users.getLoginId());
        String refreshToken = jwtProvider.createRefreshToken(users.getLoginId());
        LoginResponseDto loginResponseDto = new LoginResponseDto(users.getId(), accessToken, refreshToken);
        redisService.setRefreshValueWithTTL(users.getId().toString(), refreshToken, 14L, TimeUnit.DAYS);

        return loginResponseDto;
    }

    public void postLogout(HttpServletRequest request, Users nowuser) throws CustomException {
        String accessToken = jwtAuthFilter.resolveToken(request,jwtAuthFilter.HEADER_KEY);
        Long expiration = jwtProvider.getExpiration(accessToken);
        redisService.setAccessBlackValueWithTTL(accessToken,"logout",expiration, TimeUnit.MILLISECONDS);
        // 리프레시 토큰 삭제
        redisService.deleteValue(REFRESH_TOKEN_PREFIX+nowuser.getId().toString());
        System.out.println("왕왕왕왕왕");
    }

    public void deleteUsers(Users nowUser) throws CustomException {
        List <Announcement> announcementList = announcementRepository.findAnnouncementByUsers(nowUser);
        announcementRepository.deleteAll(announcementList);
        List <ChatHistory> chatHistoryList = chatHistoryRepository.findByUsers(nowUser);
        chatHistoryRepository.deleteAll(chatHistoryList);
        List <ConnectedCardCompany> connectedCardCompanyList = connectedCardCompanyRepository.findAllByUsers(nowUser);
        connectedCardCompanyRepository.deleteAll(connectedCardCompanyList);
        List<OwnedCard> ownedCardList = ownedCardRepository.findOwnedCardByUsers(nowUser);
        ownedCardRepository.deleteAll(ownedCardList);
        List <Favorite> favoriteList = favoriteRepository.findAllByUsers(nowUser).get();
        favoriteRepository.deleteAll(favoriteList);
        List <FCMToken> fcmTokenList = fcmTokenRepository.findAllByUsers(nowUser);
        fcmTokenRepository.deleteAll(fcmTokenList);
        usersRepository.delete(nowUser);
        return;
    }

    public UserEditResponseDto getUserEdit(Users nowUser) throws CustomException {
        return UserEditResponseDto.fromEntity(nowUser);
    }

    public List<CardSearchListDto> getSearchCard(String code) throws CustomException {
        String[] codes = code.split(",");
        List<CardSearchListDto> response = new ArrayList<>();

        for (String companycode : codes) {
            Optional<CardCompany> optionalCardCompany = cardCompanyRepository.findByCode(companycode);
            CardCompany cardCompany = optionalCardCompany.orElseThrow(() -> new CustomException(ResponseCode.CARD_COMPANY_NOT_FOUND));
            CardSearchListDto cardSearchListDto = new CardSearchListDto();
            cardSearchListDto.setCompanyName(cardCompany.getName());

            List<Card> cards = cardRepository.findAllByCardCompany(cardCompany);
            List<CardSearchDto> cardSearchDtos = new ArrayList<>();

            for (Card card : cards) {
                CardSearchDto cardSearchDto = new CardSearchDto();
                cardSearchDto.setCardId(card.getId());
                cardSearchDto.setCardName(card.getName());
                cardSearchDto.setCardImg(card.getImgUrl());
                if (card.getType().toString() == "DebitCard") cardSearchDto.setType("체크카드");
                else cardSearchDto.setType("신용카드");
                cardSearchDtos.add(cardSearchDto);
            }
            cardSearchListDto.setCardList(cardSearchDtos);
            response.add(cardSearchListDto);

        }
        return response;
    }

    public List<CardNameSearchDto> getSearchCardName(String name, String code) throws CustomException {
        String[] codes = code.split(",");
        List<CardCompany> cardCompanies = cardCompanyRepository.findByCodeIn(Arrays.asList(codes));
        List<Card> cards = cardRepository.findByNameContainingAndCardCompanyIn(name, cardCompanies);
        if (cards.isEmpty()) {
            throw new CustomException(ResponseCode.NO_SEARCH_RESULTS);
        }
        return cards.stream()
                .map(card -> new CardNameSearchDto(
                        card.getId(),
                        card.getName(),
                        card.getImgUrl(),
                        card.getCardCompany().getName(),
                        getCardTypeName(card.getType())
                ))
                .collect(Collectors.toList());
    }

    private String getCardTypeName(CardType type) {
        return type == CardType.CreditCard ? "신용카드" : "체크카드";
    }

    public void postSearchCard(Users nowUser, postSearchCardListRequestDto requestDto) throws CustomException {
        List<Long> card_list = requestDto.getCardList();
        for (Long cardId : card_list) {
            Optional<Card> optionalCard = cardRepository.findById(cardId);
            Card card = optionalCard.orElseThrow(() -> new CustomException(ResponseCode.CARD_NOT_FOUND));
            OwnedCard ownedCard = OwnedCard.builder()
                    .users(nowUser)
                    .card(card)
                    .isMain(false)
                    .performanceCondition(0)
                    .currentPerformance(0)
                    .build();
            ownedCardRepository.save(ownedCard);
        }
        return;
    }

    public String patchEditProfile(Users nowUser, MultipartFile userProfileImg) throws IOException, CustomException {
        if (!userProfileImg.isEmpty()) {
            String storedFileName = s3Uploader.upload(userProfileImg, nowUser.getLoginId() + "-profile.png");
            nowUser.setProfileImg(storedFileName);
        }
        usersRepository.save(nowUser);
        return nowUser.getProfileImg();
    }

    public boolean getIdDuplicateCheck(String loginId) throws CustomException {
        if (usersRepository.existsByLoginId(loginId)) {
            return false;
        }
        return true;
    }

    public boolean postCheckPassword(Users nowUser, ChangePasswordDto checkPasswordRequestDto) throws CustomException {
        return passwordEncoder.matches(checkPasswordRequestDto.getPassword(), nowUser.getPassword());
    }

    public String patchChangePassword(Users nowUser, ChangePasswordDto changePasswordDto) throws CustomException {
        nowUser.updatePassword(passwordEncoder.encode(changePasswordDto.getPassword()));
        usersRepository.save(nowUser);
        return ResponseCode.CHANGE_PW_SUCCESS.getMessage();
    }

    public String patchEditUserInfo(Users nowUser, EditUserInfoRequestDto editUserInfoRequestDto) throws CustomException {
        nowUser.editUserInfo(editUserInfoRequestDto);
        usersRepository.save(nowUser);
        return ResponseCode.CHANGE_MYPAGE_SUCCESS.getMessage();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Users users = usersRepository.findByLoginId(username).get();
        return new User(users.getLoginId(), users.getPassword(),
                users.getAuthorities());
    }
}
