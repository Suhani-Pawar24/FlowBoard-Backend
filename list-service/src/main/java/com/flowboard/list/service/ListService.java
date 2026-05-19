package com.flowboard.list.service;

import com.flowboard.list.entity.TaskList;
import java.util.List;

public interface ListService {
    TaskList createList(TaskList list);
    TaskList getListById(Long listId);
    List<TaskList> getListsByBoard(Long boardId);
    TaskList updateList(Long listId, TaskList list);
    void reorderLists(Long boardId, List<Long> listIds);
    void archiveList(Long listId);
    void unarchiveList(Long listId);
    void deleteList(Long listId);
    void moveList(Long listId, Long targetBoardId);
    List<TaskList> getArchivedLists(Long boardId);
}
