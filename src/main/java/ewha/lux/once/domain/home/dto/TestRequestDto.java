package ewha.lux.once.domain.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestRequestDto {
    private int paymentAmount;
    private String keyword;
    private List<Long> cardList;
    private String model;

}
