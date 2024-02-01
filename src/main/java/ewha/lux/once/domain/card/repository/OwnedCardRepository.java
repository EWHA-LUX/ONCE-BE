package ewha.lux.once.domain.card.repository;

import ewha.lux.once.domain.card.entity.Card;
import ewha.lux.once.domain.card.entity.OwnedCard;
import ewha.lux.once.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedCardRepository extends JpaRepository<OwnedCard, Long> {
    int countAllByUsers( Users users );
    OwnedCard findOwnedCardByCardAndUsers(Card card, Users users);
}