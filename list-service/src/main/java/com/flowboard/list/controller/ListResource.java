package com.flowboard.list.controller;

import com.flowboard.list.entity.TaskList;
import com.flowboard.list.service.ListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lists")
public class ListResource {

    @Autowired
    private ListService listService;

    @PostMapping
    public ResponseEntity<TaskList> createList(@RequestBody TaskList list) {
        return ResponseEntity.ok(listService.createList(list));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<TaskList> getListById(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.getListById(listId));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<TaskList>> getListsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(listService.getListsByBoard(boardId));
    }

    @PutMapping("/{listId}")
    public ResponseEntity<TaskList> updateList(@PathVariable Long listId, @RequestBody TaskList list) {
        return ResponseEntity.ok(listService.updateList(listId, list));
    }

    @PutMapping("/reorder/{boardId}")
    public ResponseEntity<Void> reorderLists(@PathVariable Long boardId, @RequestBody List<Long> listIds) {
        listService.reorderLists(boardId, listIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{listId}/archive")
    public ResponseEntity<Void> archiveList(@PathVariable Long listId) {
        listService.archiveList(listId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{listId}/unarchive")
    public ResponseEntity<Void> unarchiveList(@PathVariable Long listId) {
        listService.unarchiveList(listId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable Long listId) {
        listService.deleteList(listId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{listId}/move/{boardId}")
    public ResponseEntity<Void> moveList(@PathVariable Long listId, @PathVariable Long boardId) {
        listService.moveList(listId, boardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<TaskList>> getArchivedLists(@PathVariable Long boardId) {
        return ResponseEntity.ok(listService.getArchivedLists(boardId));
    }
}
