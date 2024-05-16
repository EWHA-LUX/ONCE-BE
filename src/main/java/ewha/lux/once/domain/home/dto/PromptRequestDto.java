package ewha.lux.once.domain.home.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequestDto {
    private int paymentAmount;
    private String keyword;
    private List<Long> cardList;
    private long answerCardId;
    private String answerBenefit;
    private int answerDiscount;

}
