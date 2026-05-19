package com.flowboard.label.service.impl;

import com.flowboard.label.entity.CardLabel;
import com.flowboard.label.entity.Checklist;
import com.flowboard.label.entity.ChecklistItem;
import com.flowboard.label.entity.Label;
import com.flowboard.label.repository.CardLabelRepository;
import com.flowboard.label.repository.ChecklistItemRepository;
import com.flowboard.label.repository.ChecklistRepository;
import com.flowboard.label.repository.LabelRepository;
import com.flowboard.label.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabelServiceImpl implements LabelService {

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private ChecklistRepository checklistRepository;

    @Autowired
    private ChecklistItemRepository itemRepository;

    @Autowired
    private CardLabelRepository cardLabelRepository;

    @Override
    public Label createLabel(Long boardId, Label label) {
        label.setBoardId(boardId);
        return labelRepository.save(label);
    }

    @Override
    public List<Label> getLabelsByBoard(Long boardId) {
        return labelRepository.findByBoardId(boardId);
    }

    @Override
    public Label updateLabel(Long labelId, Label label) {
        Label existing = labelRepository.findById(labelId)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        existing.setName(label.getName());
        existing.setColor(label.getColor());
        return labelRepository.save(existing);
    }

    @Override
    public void deleteLabel(Long labelId) {
        labelRepository.deleteById(labelId);
    }

    @Override
    public void addLabelToCard(Long cardId, Long labelId) {
        if (cardLabelRepository.findByCardIdAndLabelId(cardId, labelId).isEmpty()) {
            cardLabelRepository.save(new CardLabel(null, cardId, labelId));
        }
    }

    @Override
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId) {
        cardLabelRepository.deleteByCardIdAndLabelId(cardId, labelId);
    }

    @Override
    public List<Label> getLabelsForCard(Long cardId) {
        List<Long> labelIds = cardLabelRepository.findByCardId(cardId).stream()
                .map(CardLabel::getLabelId)
                .toList();
        return labelRepository.findAllById(labelIds);
    }

    @Override
    public Checklist createChecklist(Checklist checklist) {
        return checklistRepository.save(checklist);
    }

    @Override
    public ChecklistItem addItem(Long checklistId, ChecklistItem item) {
        item.setChecklistId(checklistId);
        return itemRepository.save(item);
    }

    @Override
    public ChecklistItem toggleItem(Long itemId) {
        ChecklistItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.setIsCompleted(!item.getIsCompleted());
        return itemRepository.save(item);
    }

    @Override
    public void deleteChecklist(Long checklistId) {
        checklistRepository.deleteById(checklistId);
    }

    @Override
    public List<Checklist> getChecklistsByCard(Long cardId) {
        return checklistRepository.findByCardIdOrderByPositionAsc(cardId);
    }

    @Override
    public Map<String, Object> getChecklistProgress(Long checklistId) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist not found"));
        List<ChecklistItem> items = checklist.getItems();
        long total = items.size();
        long completed = items.stream().filter(ChecklistItem::getIsCompleted).count();
        double percentage = total == 0 ? 0 : (double) completed / total * 100;
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("completed", completed);
        progress.put("percentage", Math.round(percentage * 100.0) / 100.0);
        return progress;
    }
}
