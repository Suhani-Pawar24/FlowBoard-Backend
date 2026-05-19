package com.flowboard.label.controller;

import com.flowboard.label.entity.Checklist;
import com.flowboard.label.entity.ChecklistItem;
import com.flowboard.label.entity.Label;
import com.flowboard.label.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class LabelResource {

    @Autowired
    private LabelService labelService;

    @PostMapping("/labels")
    public ResponseEntity<Label> createLabel(@RequestParam Long boardId, @RequestBody Label label) {
        return ResponseEntity.ok(labelService.createLabel(boardId, label));
    }

    @GetMapping("/labels/board/{boardId}")
    public ResponseEntity<List<Label>> getLabelsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(labelService.getLabelsByBoard(boardId));
    }

    @PutMapping("/labels/{labelId}")
    public ResponseEntity<Label> updateLabel(@PathVariable Long labelId, @RequestBody Label label) {
        return ResponseEntity.ok(labelService.updateLabel(labelId, label));
    }

    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long labelId) {
        labelService.deleteLabel(labelId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cards/{cardId}/labels/{labelId}")
    public ResponseEntity<Void> addLabelToCard(@PathVariable Long cardId, @PathVariable Long labelId) {
        labelService.addLabelToCard(cardId, labelId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    public ResponseEntity<Void> removeLabelFromCard(@PathVariable Long cardId, @PathVariable Long labelId) {
        labelService.removeLabelFromCard(cardId, labelId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cards/{cardId}/labels")
    public ResponseEntity<List<Label>> getLabelsForCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getLabelsForCard(cardId));
    }

    @PostMapping("/checklists")
    public ResponseEntity<Checklist> createChecklist(@RequestBody Checklist checklist) {
        return ResponseEntity.ok(labelService.createChecklist(checklist));
    }

    @PostMapping("/checklists/{checklistId}/items")
    public ResponseEntity<ChecklistItem> addItem(@PathVariable Long checklistId, @RequestBody ChecklistItem item) {
        return ResponseEntity.ok(labelService.addItem(checklistId, item));
    }

    @PutMapping("/checklists/items/{itemId}/toggle")
    public ResponseEntity<ChecklistItem> toggleItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(labelService.toggleItem(itemId));
    }

    @DeleteMapping("/checklists/{checklistId}")
    public ResponseEntity<Void> deleteChecklist(@PathVariable Long checklistId) {
        labelService.deleteChecklist(checklistId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cards/{cardId}/checklists")
    public ResponseEntity<List<Checklist>> getChecklistsByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getChecklistsByCard(cardId));
    }

    @GetMapping("/checklists/{checklistId}/progress")
    public ResponseEntity<Map<String, Object>> getChecklistProgress(@PathVariable Long checklistId) {
        return ResponseEntity.ok(labelService.getChecklistProgress(checklistId));
    }
}
