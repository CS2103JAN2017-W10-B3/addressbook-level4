package todolist.logic.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import todolist.commons.core.EventsCenter;
import todolist.commons.core.Messages;
import todolist.commons.core.UnmodifiableObservableList;
import todolist.commons.events.ui.SelectMultipleTargetEvent;
import todolist.commons.util.CollectionUtil;
import todolist.logic.commands.exceptions.CommandException;
import todolist.model.ReadOnlyToDoList;
import todolist.model.ToDoList;
import todolist.model.tag.UniqueTagList;
import todolist.model.task.Description;
import todolist.model.task.EndTime;
import todolist.model.task.ReadOnlyTask;
import todolist.model.task.StartTime;
import todolist.model.task.Task;
import todolist.model.task.TaskIndex;
import todolist.model.task.Title;
import todolist.model.task.UniqueTaskList;
import todolist.model.task.UrgencyLevel;
import todolist.model.task.Venue;

/**
 * Edits the details of an existing task in the address book.
 */
public class EditCommand extends UndoableCommand {

    public static final String COMMAND_WORD = "edit";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the task identified "
            + "by the index number used in the last task listing. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX (must be a positive integer)[TITLE][place/VENUE][from/STARTTIME]"
            + "[level/URGENCYLEVEL][des/DESCRIPTION][to/ENDTIME][#TAG]..\n" + "Example: " + COMMAND_WORD
            + " 1 place/Toilet";

    public static final String MESSAGE_EDIT_TASK_SUCCESS = "Edited Task: %1$s";
    public static final String MESSAGE_NOT_EDITED = "At least one field to edit must be provided.";
    public static final String MESSAGE_DUPLICATE_TASK = "This task already exists in the address book.";
    // @@ A0143648Y
    private final ArrayList<TaskIndex> filteredTaskListIndexes;
    private final EditTaskDescriptor editTaskDescriptor;
    private ReadOnlyToDoList originalToDoList;
    private CommandResult commandResultToUndo;

    /**
     * @param filteredTaskListIndex
     *            the index of the task in the filtered task list to edit
     * @param editTaskDescriptor
     *            details to edit the task with
     */
    public EditCommand(ArrayList<TaskIndex> filteredTaskListIndexes, EditTaskDescriptor editTaskDescriptor) {
        assert filteredTaskListIndexes != null;
        assert editTaskDescriptor != null;

        this.filteredTaskListIndexes = filteredTaskListIndexes;
        this.editTaskDescriptor = new EditTaskDescriptor(editTaskDescriptor);
    }

    @Override
    public CommandResult execute() throws CommandException {
        originalToDoList = new ToDoList(model.getToDoList());
        ArrayList<Task> listOfEditedTasks = new ArrayList<Task>();
        for (int count = 0; count < filteredTaskListIndexes.size(); count++) {
            List<ReadOnlyTask> lastShownList = model.getListFromChar(filteredTaskListIndexes.get(count).getTaskChar());
            int filteredTaskListIndex = filteredTaskListIndexes.get(count).getTaskNumber() - 1;

            if (filteredTaskListIndex >= lastShownList.size()) {
                throw new CommandException(Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX);
            }

            ReadOnlyTask taskToEdit = lastShownList.get(filteredTaskListIndex);
            Task editedTask = createEditedTask(taskToEdit, editTaskDescriptor);

            try {
                model.updateTask(taskToEdit, editedTask);
            } catch (UniqueTaskList.DuplicateTaskException dpe) {
                throw new CommandException(MESSAGE_DUPLICATE_TASK);
            }
            listOfEditedTasks.add(editedTask);
        }

        model.updateFilteredListToShowAll();
        filteredTaskListIndexes.clear();
        for (int count = 0; count < listOfEditedTasks.size(); count++) {
            UnmodifiableObservableList<ReadOnlyTask> listOfTask = model
                    .getListFromChar(listOfEditedTasks.get(count).getTaskChar());
            filteredTaskListIndexes.add(new TaskIndex(listOfEditedTasks.get(count).getTaskChar(),
                    listOfTask.indexOf(listOfEditedTasks.get(count))));
        }
        EventsCenter.getInstance().post(new SelectMultipleTargetEvent(filteredTaskListIndexes));
        commandResultToUndo = new CommandResult(MESSAGE_EDIT_TASK_SUCCESS);
        updateUndoLists();
        return new CommandResult(MESSAGE_EDIT_TASK_SUCCESS);
    }

    @Override
    public void updateUndoLists() {
        if (previousToDoLists == null) {
            previousToDoLists = new ArrayList<ReadOnlyToDoList>(3);
            previousCommandResults = new ArrayList<CommandResult>(3);
        }
        if (previousToDoLists.size() >= 3) {
            previousToDoLists.remove(0);
            previousCommandResults.remove(0);
            previousToDoLists.add(originalToDoList);
            previousCommandResults.add(commandResultToUndo);
        } else {
            previousToDoLists.add(originalToDoList);
            previousCommandResults.add(commandResultToUndo);
        }
    }
    // @@

    /**
     * Creates and returns a {@code Task} with the details of {@code taskToEdit}
     * edited with {@code editTaskDescriptor}.
     */
    private Task createEditedTask(ReadOnlyTask taskToEdit, EditTaskDescriptor editTaskDescriptor) {
        assert taskToEdit != null;

        Title updatedTitle = editTitle(taskToEdit);
        Venue updatedVenue = editVenue(taskToEdit);
        StartTime updatedStartTime = editStartTime(taskToEdit);
        EndTime updatedEndTime = editEndTime(taskToEdit);
        UrgencyLevel updatedUrgencyLevel = editUrgencyLevel(taskToEdit);
        Description updatedDescription = editDescription(taskToEdit);
        UniqueTagList updatedTags = editTaskDescriptor.getTags().orElseGet(taskToEdit::getTags);

        return new Task(updatedTitle, updatedVenue, updatedStartTime, updatedEndTime, updatedUrgencyLevel,
                updatedDescription, updatedTags);
    }

    public Title editTitle(ReadOnlyTask taskToEdit) {
        return editTaskDescriptor.getTitle().isPresent() ? editTaskDescriptor.getTitle().get() : taskToEdit.getTitle();
    }

    public Venue editVenue(ReadOnlyTask taskToEdit) {
        if (editTaskDescriptor.getVenue().isPresent()) {
            return editTaskDescriptor.getVenue().get();
        } else if (taskToEdit.getVenue().isPresent()) {
            return taskToEdit.getVenue().get();
        }
        return null;
    }

    public StartTime editStartTime(ReadOnlyTask taskToEdit) {
        if (editTaskDescriptor.getStartTime().isPresent()) {
            return editTaskDescriptor.getStartTime().get();
        } else if (taskToEdit.getStartTime().isPresent()) {
            return taskToEdit.getStartTime().get();
        }
        return null;
    }

    public EndTime editEndTime(ReadOnlyTask taskToEdit) {
        if (editTaskDescriptor.getEndTime().isPresent()) {
            return editTaskDescriptor.getEndTime().get();
        } else if (taskToEdit.getEndTime().isPresent()) {
            return taskToEdit.getEndTime().get();
        }
        return null;
    }

    public UrgencyLevel editUrgencyLevel(ReadOnlyTask taskToEdit) {
        if (editTaskDescriptor.getUrgencyLevel().isPresent()) {
            return editTaskDescriptor.getUrgencyLevel().get();
        } else if (taskToEdit.getUrgencyLevel().isPresent()) {
            return taskToEdit.getUrgencyLevel().get();
        }
        return null;
    }

    public Description editDescription(ReadOnlyTask taskToEdit) {
        if (editTaskDescriptor.getDescription().isPresent()) {
            return editTaskDescriptor.getDescription().get();
        } else if (taskToEdit.getDescription().isPresent()) {
            return taskToEdit.getDescription().get();
        }
        return null;
    }

    /**
     * Stores the details to edit the task with. Each non-empty field value will
     * replace the corresponding field value of the task.
     */
    public static class EditTaskDescriptor {
        private Optional<Title> title = Optional.empty();
        private Optional<Venue> venue = Optional.empty();
        private Optional<StartTime> startTime = Optional.empty();
        private Optional<EndTime> endTime = Optional.empty();
        private Optional<UrgencyLevel> urgencyLevel = Optional.empty();
        private Optional<Description> description = Optional.empty();
        private Optional<UniqueTagList> tags = Optional.empty();

        public EditTaskDescriptor() {
        }

        public EditTaskDescriptor(EditTaskDescriptor toCopy) {
            this.title = toCopy.getTitle();
            this.venue = toCopy.getVenue();
            this.startTime = toCopy.getStartTime();
            this.endTime = toCopy.getEndTime();
            this.urgencyLevel = toCopy.getUrgencyLevel();
            this.description = toCopy.getDescription();
            this.tags = toCopy.getTags();
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            return CollectionUtil.isAnyPresent(this.title, this.venue, this.startTime, this.endTime, this.urgencyLevel,
                    this.description, this.tags);
        }

        public void setTitle(Optional<Title> title) {
            assert title != null;
            this.title = title;
        }

        public Optional<Title> getTitle() {
            return title;
        }

        public void setVenue(Optional<Venue> venue) {
            assert venue != null;
            this.venue = venue;
        }

        public Optional<Venue> getVenue() {
            return venue;
        }

        public void setStartTime(Optional<StartTime> startTime) {
            assert startTime != null;
            this.startTime = startTime;
        }

        public Optional<StartTime> getStartTime() {
            return startTime;
        }

        public void setEndTime(Optional<EndTime> endTime) {
            assert endTime != null;
            this.endTime = endTime;
        }

        public Optional<EndTime> getEndTime() {
            return endTime;
        }

        public void setUrgencyLevel(Optional<UrgencyLevel> urgencyLevel) {
            assert urgencyLevel != null;
            this.urgencyLevel = urgencyLevel;
        }

        public Optional<UrgencyLevel> getUrgencyLevel() {
            return urgencyLevel;
        }

        public void setDescription(Optional<Description> description) {
            assert description != null;
            this.description = description;
        }

        public Optional<Description> getDescription() {
            return description;
        }

        public void setTags(Optional<UniqueTagList> tags) {
            assert tags != null;
            this.tags = tags;
        }

        public Optional<UniqueTagList> getTags() {
            return tags;
        }
    }
}
