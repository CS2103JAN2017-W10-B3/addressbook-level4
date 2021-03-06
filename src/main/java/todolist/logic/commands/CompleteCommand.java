package todolist.logic.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import todolist.commons.core.EventsCenter;
import todolist.commons.core.LogsCenter;
import todolist.commons.core.Messages;
import todolist.commons.events.ui.SelectMultipleTargetEvent;
import todolist.logic.commands.exceptions.CommandException;
import todolist.model.ReadOnlyToDoList;
import todolist.model.ToDoList;
import todolist.model.task.ReadOnlyTask;
import todolist.model.task.TaskIndex;


/**
 * Selects a task identified using it's last displayed index from the address
 * book.
 */
public class CompleteCommand extends UndoableCommand {

    private Logger logger = LogsCenter.getLogger(CompleteCommand.class.getName());

    private final ArrayList<TaskIndex> filteredTaskListIndexes;

    public static final String COMMAND_WORD = "done";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Completes the task identified by the index number used in the last task listing.\n"
            + "Parameters: CHAR(d, e or f) + INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " e1 \n";

    public static final String MESSAGE_COMPLETE_TASK_SUCCESS = "Completed Task: ";

    private ReadOnlyToDoList originalToDoList;
    private CommandResult commandResultToUndo;
    private String messageSuccessful;

    public CompleteCommand(ArrayList<TaskIndex> filteredTaskListIndexes) {
        this.filteredTaskListIndexes = filteredTaskListIndexes;
    }

    @Override
    public CommandResult execute() throws CommandException {
        logger.info("-------[Executing EditCommand]");

        if (filteredTaskListIndexes.isEmpty()) {
            filteredTaskListIndexes.addAll(model.getSelectedIndexes());
            if (filteredTaskListIndexes.isEmpty()) {
                logger.info("-------[Execution Of CompleteCommand Failed]");
                throw new CommandException(Messages.MESSAGE_NO_TASK_SELECTED);
            }
        }
        originalToDoList = new ToDoList(model.getToDoList());
        ArrayList<ReadOnlyTask> tasksToComplete = new ArrayList<ReadOnlyTask>();
        ArrayList<TaskIndex> selectedIndexes = new ArrayList<TaskIndex>();
        for (int count = 0; count < filteredTaskListIndexes.size(); count++) {
            List<ReadOnlyTask> lastShownList = model.getListFromChar(filteredTaskListIndexes.get(count).getTaskChar());
            int filteredTaskListIndex = filteredTaskListIndexes.get(count).getTaskNumber() - 1;

            if (lastShownList.size() < filteredTaskListIndex) {
                logger.info("-------[Execution Of CompleteCommand Failed]");
                throw new CommandException(Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX);
            }

            tasksToComplete.add(lastShownList.get(filteredTaskListIndex));
        }

        StringBuilder sb = new StringBuilder();
        for (int count = 0; count < tasksToComplete.size(); count++) {
            model.completeTask(tasksToComplete.get(count));
            sb.append(tasksToComplete.get(count).getTitleFormattedString());
        }
        messageSuccessful = sb.toString();

        commandResultToUndo = new CommandResult(MESSAGE_COMPLETE_TASK_SUCCESS + messageSuccessful);
        updateUndoLists();

        List<ReadOnlyTask> completedList = model.getCompletedList();
        for (int count = 0; count < tasksToComplete.size(); count++) {
            selectedIndexes.add(new TaskIndex('c', completedList.indexOf(tasksToComplete.get(count)) + 1));
        }

        logger.info("-------[Executed CompleteCommand]");

        model.updateSelectedIndexes(selectedIndexes);
        EventsCenter.getInstance().post(new SelectMultipleTargetEvent(selectedIndexes));

        return new CommandResult(MESSAGE_COMPLETE_TASK_SUCCESS + messageSuccessful);
    }

    /**
     * Update {@code previousToDoLists} with the todolist before last edition
     * and {@code previousCommand} with the command just executed
     */
    @Override
    public void updateUndoLists() {
        if (previousToDoLists == null) {
            previousToDoLists = new ArrayList<ReadOnlyToDoList>(UNDO_HISTORY_SIZE);
            previousCommandResults = new ArrayList<CommandResult>(UNDO_HISTORY_SIZE);
        }
        if (previousToDoLists.size() >= UNDO_HISTORY_SIZE) {
            previousToDoLists.remove(ITEM_TO_BE_REMOVED_FROM_HISTORY);
            previousCommandResults.remove(ITEM_TO_BE_REMOVED_FROM_HISTORY);
            previousToDoLists.add(originalToDoList);
            previousCommandResults.add(commandResultToUndo);
        } else {
            previousToDoLists.add(originalToDoList);
            previousCommandResults.add(commandResultToUndo);
        }
    }


}
