package ewha.lux.once.domain.card.entity;

import ewha.lux.once.domain.card.entity.Card;
import ewha.lux.once.domain.user.entity.Users;
import ewha.lux.once.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="OwnedCard")
@Getter
@Setter
@Builder
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
public class OwnedCard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ownedCardId")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private Users users;

    @ManyToOne
    @JoinColumn(name = "cardId")
    private Card card;

    @Column(name = "isMain")
    private boolean isMain;

    @Column(name = "performanceCondition")
    private Integer performanceCondition;

    @Column(name = "currentPerformance")
    private Integer currentPerformance;
}