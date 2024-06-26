package ewha.lux.once.domain.home.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeDto {
    private String nickname;
    private int ownedCardCount;
    private List<String> keywordList;
}
