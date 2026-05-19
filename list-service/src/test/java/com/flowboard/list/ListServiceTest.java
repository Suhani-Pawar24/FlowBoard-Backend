package com.flowboard.list;

import com.flowboard.list.entity.TaskList;
import com.flowboard.list.repository.ListRepository;
import com.flowboard.list.service.impl.ListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceTest {

    @Mock
    private ListRepository listRepository;

    @InjectMocks
    private ListServiceImpl listService;

    private TaskList taskList;

    @BeforeEach
    void setUp() {
        taskList = new TaskList();
        taskList.setListId(1L);
        taskList.setBoardId(10L);
        taskList.setName("To Do");
        taskList.setPosition(0);
        taskList.setIsArchived(false);
    }

    @Test
    @DisplayName("LS-01: createList saves list with correct position")
    void createList_savesWithPosition() {
        when(listRepository.findMaxPositionByBoardId(10L)).thenReturn(Optional.of(5));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList input = new TaskList();
        input.setBoardId(10L);
        input.setName("New List");

        TaskList result = listService.createList(input);
        assertEquals(6, result.getPosition());
        verify(listRepository).save(any());
    }

    @Test
    @DisplayName("LS-02: getListsByBoard returns unarchived lists")
    void getListsByBoard_returnsUnarchived() {
        TaskList archived = new TaskList();
        archived.setIsArchived(true);
        when(listRepository.findByBoardIdOrderByPositionAsc(10L)).thenReturn(List.of(taskList, archived));

        List<TaskList> results = listService.getListsByBoard(10L);
        assertEquals(1, results.size());
        assertFalse(results.get(0).getIsArchived());
    }

    @Test
    @DisplayName("LS-03: archiveList sets isArchived to true")
    void archiveList_updatesFlag() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listService.archiveList(1L);
        assertTrue(taskList.getIsArchived());
    }

    @Test
    @DisplayName("LS-04: reorderLists updates multiple positions")
    void reorderLists_updatesPositions() {
        TaskList l2 = new TaskList();
        l2.setListId(2L);
        l2.setBoardId(10L);
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));
        when(listRepository.findById(2L)).thenReturn(Optional.of(l2));

        listService.reorderLists(10L, List.of(2L, 1L));
        assertEquals(1, taskList.getPosition());
        assertEquals(0, l2.getPosition());
        verify(listRepository, times(2)).save(any());
    }
}
