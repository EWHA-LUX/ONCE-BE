package ewha.lux.once.domain.home.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestResponseDto {
    private long cardId;
    private String benefit;
    private int discount;
}
