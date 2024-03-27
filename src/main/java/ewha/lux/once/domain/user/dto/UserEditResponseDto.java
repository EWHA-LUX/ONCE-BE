package ewha.lux.once.domain.user.dto;

import ewha.lux.once.domain.user.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEditResponseDto {
    private String userProfileImg;
    private String username;
    private String nickname;
    private String loginId;
    private String birthday;
    private String userPhoneNum;
    private String createdAt;

    public static UserEditResponseDto fromEntity(Users users) {
        UserEditResponseDto userEditResponseDto = new UserEditResponseDto();
        userEditResponseDto.setUserProfileImg(users.getProfileImg());
        userEditResponseDto.setUsername(users.getUsername());
        userEditResponseDto.setNickname(users.getNickname());
        userEditResponseDto.setLoginId(users.getLoginId());


        LocalDateTime createdAt = users.getCreatedAt();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String formattedDate = createdAt.format(formatter);
        userEditResponseDto.setCreatedAt(formattedDate);

        Date birthday = users.getBirthday();
        if (birthday != null) {
            SimpleDateFormat birthdayFormatter = new SimpleDateFormat("yyyy.MM.dd");
            userEditResponseDto.setBirthday(birthdayFormatter.format(birthday));
        } else {
            userEditResponseDto.setBirthday("");
        }

        String phone = users.getPhone();
        if (phone != null) {
            userEditResponseDto.setUserPhoneNum(phone);
        } else {
            userEditResponseDto.setUserPhoneNum("");
        }


        return userEditResponseDto;
    }

}
