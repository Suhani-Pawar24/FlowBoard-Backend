package com.flowboard.list.service.impl;

import com.flowboard.list.entity.TaskList;
import com.flowboard.list.repository.ListRepository;
import com.flowboard.list.service.ListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private ListRepository listRepository;

    @Override
    public TaskList createList(TaskList list) {
        if (list.getPosition() == null) {
            int maxPos = listRepository.findMaxPositionByBoardId(list.getBoardId()).orElse(-1);
            list.setPosition(maxPos + 1);
        }
        return listRepository.save(list);
    }

    @Override
    public TaskList getListById(Long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + listId));
    }

    @Override
    public List<TaskList> getListsByBoard(Long boardId) {
        return listRepository.findByBoardIdOrderByPositionAsc(boardId).stream()
                .filter(l -> !l.getIsArchived())
                .toList();
    }

    @Override
    public TaskList updateList(Long listId, TaskList list) {
        TaskList existing = getListById(listId);
        existing.setName(list.getName());
        if (list.getPosition() != null) existing.setPosition(list.getPosition());
        if (list.getColor() != null) existing.setColor(list.getColor());
        return listRepository.save(existing);
    }

    @Override
    @Transactional
    public void reorderLists(Long boardId, List<Long> listIds) {
        for (int i = 0; i < listIds.size(); i++) {
            TaskList list = listRepository.findById(listIds.get(i)).orElse(null);
            if (list != null && list.getBoardId().equals(boardId)) {
                list.setPosition(i);
                listRepository.save(list);
            }
        }
    }

    @Override
    public void archiveList(Long listId) {
        TaskList list = getListById(listId);
        list.setIsArchived(true);
        listRepository.save(list);
    }

    @Override
    public void unarchiveList(Long listId) {
        TaskList list = getListById(listId);
        list.setIsArchived(false);
        listRepository.save(list);
    }

    @Override
    @Transactional
    public void deleteList(Long listId) {
        listRepository.deleteById(listId);
        // Note: In a distributed system, we might need to send a message to card-service
        // to delete cards associated with this list.
    }

    @Override
    @Transactional
    public void moveList(Long listId, Long targetBoardId) {
        TaskList list = getListById(listId);
        int maxPos = listRepository.findMaxPositionByBoardId(targetBoardId).orElse(-1);
        list.setBoardId(targetBoardId);
        list.setPosition(maxPos + 1);
        listRepository.save(list);
    }

    @Override
    public List<TaskList> getArchivedLists(Long boardId) {
        return listRepository.findByBoardIdAndIsArchived(boardId, true);
    }
}
