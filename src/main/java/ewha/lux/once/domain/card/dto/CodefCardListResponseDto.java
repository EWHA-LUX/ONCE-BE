package ewha.lux.once.domain.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CodefCardListResponseDto {
    private String cardName;
    private String cardImg;
}
