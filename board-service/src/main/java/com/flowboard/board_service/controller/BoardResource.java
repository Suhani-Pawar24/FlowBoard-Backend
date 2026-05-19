package com.flowboard.board_service.controller;

import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.entity.BoardRole;
import com.flowboard.board_service.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/boards")
public class BoardResource {

    @Autowired
    private BoardService boardService;

    @PostMapping
    public ResponseEntity<Board> createBoard(@RequestParam Long workspaceId, @RequestBody Board board, @RequestParam Long creatorId) {
        return ResponseEntity.ok(boardService.createBoard(workspaceId, board, creatorId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<Board> getBoardById(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoardById(boardId));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<Board>> getBoardsByWorkspace(@PathVariable Long workspaceId,
                                                            @RequestHeader(value = "X-user-id", required = false) Long userId,
                                                            @RequestHeader(value = "X-user-role", required = false) String role) {
        return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId, userId, role));
    }

    @GetMapping("/creator/{userId}")
    public ResponseEntity<List<Board>> getBoardsByCreator(@PathVariable Long userId) {
        return ResponseEntity.ok(boardService.getBoardsByCreator(userId));
    }

    @GetMapping("/member/{userId}")
    public ResponseEntity<List<Board>> getBoardsByMember(@PathVariable Long userId) {
        return ResponseEntity.ok(boardService.getBoardsByMember(userId));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<Board> updateBoard(@PathVariable Long boardId, 
                                            @RequestBody Board board,
                                            @RequestHeader("X-user-id") Long userId,
                                            @RequestHeader("X-user-role") String role) {
        return ResponseEntity.ok(boardService.updateBoard(boardId, board, userId, role));
    }

    @PutMapping("/{boardId}/close")
    public ResponseEntity<Board> closeBoard(@PathVariable Long boardId,
                                           @RequestHeader("X-user-id") Long userId,
                                           @RequestHeader("X-user-role") String role) {
        return ResponseEntity.ok(boardService.closeBoard(boardId, userId, role));
    }

    @PutMapping("/{boardId}/reopen")
    public ResponseEntity<Board> reopenBoard(@PathVariable Long boardId,
                                            @RequestHeader("X-user-id") Long userId,
                                            @RequestHeader("X-user-role") String role) {
        return ResponseEntity.ok(boardService.reopenBoard(boardId, userId, role));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId,
                                          @RequestHeader("X-user-id") Long userId,
                                          @RequestHeader("X-user-role") String role) {
        boardService.deleteBoard(boardId, userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{boardId}/members")
    public ResponseEntity<Void> addBoardMember(@PathVariable Long boardId, 
                                              @RequestParam Long memberUserId, 
                                              @RequestParam BoardRole memberRole,
                                              @RequestHeader("X-user-id") Long userId,
                                              @RequestHeader("X-user-role") String role) {
        boardService.addBoardMember(boardId, memberUserId, memberRole, userId, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{boardId}/members/{memberUserId}")
    public ResponseEntity<Void> removeBoardMember(@PathVariable Long boardId, 
                                                 @PathVariable Long memberUserId,
                                                 @RequestHeader("X-user-id") Long userId,
                                                 @RequestHeader("X-user-role") String role) {
        boardService.removeBoardMember(boardId, memberUserId, userId, role);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{boardId}/members/{memberUserId}/role")
    public ResponseEntity<Void> updateBoardMemberRole(@PathVariable Long boardId, 
                                                     @PathVariable Long memberUserId, 
                                                     @RequestParam BoardRole newRole,
                                                     @RequestHeader("X-user-id") Long userId,
                                                     @RequestHeader("X-user-role") String role) {
        boardService.updateBoardMemberRole(boardId, memberUserId, newRole, userId, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{boardId}/members")
    public ResponseEntity<List<BoardMember>> getBoardMembers(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoardMembers(boardId));
    }

    @GetMapping("/{boardId}/analytics")
    public ResponseEntity<Map<String, Object>> getBoardAnalytics(@PathVariable Long boardId,
                                                               @RequestHeader("X-user-id") Long userId,
                                                               @RequestHeader("X-user-role") String role) {
        return ResponseEntity.ok(boardService.getBoardAnalytics(boardId, userId, role));
    }

    /**
     * GET /boards/all — Admin: returns ALL boards on the platform.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllBoardsAdmin(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body("Access Denied: Platform Admin role required");
        }
        return ResponseEntity.ok(boardService.getAllBoards());
    }
}
