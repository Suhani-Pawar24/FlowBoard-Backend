package com.flowboard.card;

import com.flowboard.card.entity.Card;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardActivityRepository activityRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card();
        card.setCardId(1L);
        card.setListId(10L);
        card.setBoardId(5L);
        card.setTitle("Bug Fix");
        card.setPosition(0);
        card.setIsArchived(false);
    }

    @Test
    @DisplayName("CS-01: getCardById returns card when found")
    void getCardById_returnsCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        Card result = cardService.getCardById(1L);
        assertEquals("Bug Fix", result.getTitle());
    }

    @Test
    @DisplayName("CS-02: archiveCard updates isArchived flag")
    void archiveCard_updatesFlag() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cardService.archiveCard(1L);
        assertTrue(card.getIsArchived());
    }

    @Test
    @DisplayName("CS-03: createCard logs activity")
    void createCard_logsActivity() {
        when(cardRepository.findByListIdOrderByPositionAsc(10L)).thenReturn(java.util.List.of());
        when(cardRepository.save(any())).thenReturn(card);

        Card result = cardService.createCard(card);
        verify(activityRepository).save(any());
    }
}
