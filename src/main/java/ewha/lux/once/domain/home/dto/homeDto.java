package ewha.lux.once.domain.home.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class homeDto {
    private String nickname;
    private List<String> keywordList;
}
