package com.flowboard.label.service;

import com.flowboard.label.entity.Label;
import com.flowboard.label.entity.Checklist;
import com.flowboard.label.entity.ChecklistItem;
import java.util.List;
import java.util.Map;

public interface LabelService {
    Label createLabel(Long boardId, Label label);
    List<Label> getLabelsByBoard(Long boardId);
    Label updateLabel(Long labelId, Label label);
    void deleteLabel(Long labelId);
    void addLabelToCard(Long cardId, Long labelId);
    void removeLabelFromCard(Long cardId, Long labelId);
    List<Label> getLabelsForCard(Long cardId);
    
    Checklist createChecklist(Checklist checklist);
    ChecklistItem addItem(Long checklistId, ChecklistItem item);
    ChecklistItem toggleItem(Long itemId);
    void deleteChecklist(Long checklistId);
    List<Checklist> getChecklistsByCard(Long cardId);
    Map<String, Object> getChecklistProgress(Long checklistId);
}
