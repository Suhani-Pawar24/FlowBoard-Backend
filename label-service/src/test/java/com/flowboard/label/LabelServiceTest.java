package com.flowboard.label;

import com.flowboard.label.entity.Label;
import com.flowboard.label.repository.LabelRepository;
import com.flowboard.label.service.impl.LabelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;

    @InjectMocks
    private LabelServiceImpl labelService;

    private Label label;

    @BeforeEach
    void setUp() {
        label = new Label();
        label.setLabelId(1L);
        label.setBoardId(10L);
        label.setName("Bug");
        label.setColor("#FF0000");
    }

    @Test
    @DisplayName("LB-01: createLabel saves label with boardId")
    void createLabel_savesLabel() {
        when(labelRepository.save(any())).thenReturn(label);
        Label result = labelService.createLabel(10L, new Label());
        assertEquals(10L, result.getBoardId());
        verify(labelRepository).save(any());
    }

    @Test
    @DisplayName("LB-02: updateLabel updates fields")
    void updateLabel_updatesFields() {
        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Label update = new Label();
        update.setName("Feature");
        update.setColor("#00FF00");

        Label result = labelService.updateLabel(1L, update);
        assertEquals("Feature", result.getName());
        assertEquals("#00FF00", result.getColor());
    }
}
