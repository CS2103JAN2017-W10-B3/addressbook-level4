package todolist.logic.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import todolist.commons.core.EventsCenter;
import todolist.commons.core.UnmodifiableObservableList;
import todolist.commons.events.ui.JumpToListRequestEvent;
import todolist.commons.exceptions.IllegalValueException;
import todolist.logic.commands.exceptions.CommandException;
import todolist.model.ReadOnlyToDoList;
import todolist.model.ToDoList;
import todolist.model.tag.Tag;
import todolist.model.tag.UniqueTagList;
import todolist.model.task.Description;
import todolist.model.task.EndTime;
import todolist.model.task.ReadOnlyTask;
import todolist.model.task.StartTime;
import todolist.model.task.Task;
import todolist.model.task.TaskIndex;
import todolist.model.task.Time;
import todolist.model.task.Title;
import todolist.model.task.UniqueTaskList;
import todolist.model.task.UrgencyLevel;
import todolist.model.task.Venue;

/**
 * Adds a Task to the address book.
 */
public class AddCommand extends UndoableCommand {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a Task to the to-do list. \n"
            + "General usage: add [/venue <VENUE>] [/from <STARTTIME>] [/to <ENDTIME>] "
            + "[/level <IMPORTANCE>] [/description <DESCRIPTION>] [#<TAGS>] "
            + "Where the parameters in square brackets are optional; \n"
            + "Date and Time format could be in the format of DD/MM/YY HH:MM, "
            + "or use words like Wed, today, tomorrow \n";

    public static final String MESSAGE_SUCCESS = "New Task added: %1$s";
    public static final String MESSAGE_DUPLICATE_TASK = "This Task already exists in the to-do list";

    private final Task toAdd;
    private ReadOnlyToDoList originalToDoList;
    private CommandResult commandResultToUndo;

    /**
     * Creates an AddCommand using raw values.
     *
     * @throws IllegalValueException
     *             if any of the raw values are invalid
     */
    public AddCommand(String title, Optional<String> venue, Optional<String> starttime, Optional<String> endtime,
            Optional<String> deadline, Optional<String> urgencyLevel, Optional<String> description, Set<String> tags)
            throws IllegalValueException {
        final List<Tag> tagSet = new ArrayList<>();
        for (String tagName : tags) {
            tagSet.add(new Tag(tagName));
        }

        Title tempTitle = new Title(title);
        Venue tempVenue = null;
        StartTime tempStartTime = null;
        EndTime tempEndTime = null;
        UrgencyLevel tempUrgencyLevel = null;
        Description tempDescription = null;

        if (venue.isPresent()) {
            tempVenue = new Venue(venue.get());
        }

        if (starttime.isPresent()) {
            tempStartTime = new StartTime(starttime.get());
        }

        if (endtime.isPresent() && !deadline.isPresent()) {
            tempEndTime = new EndTime(endtime.get());
        }

        if (deadline.isPresent() && !endtime.isPresent()) {
            tempEndTime = new EndTime(deadline.get());
        }

        if (urgencyLevel.isPresent()) {
            tempUrgencyLevel = new UrgencyLevel(urgencyLevel.get());
        }

        if (description.isPresent()) {
            tempDescription = new Description(description.get());
        }

        if (tempStartTime != null && tempEndTime != null) {
            checkValidDuration(tempStartTime, tempEndTime);
        }

        this.toAdd = new Task(tempTitle, tempVenue, tempStartTime, tempEndTime, tempUrgencyLevel, tempDescription,
                new UniqueTagList(tagSet));
    }

    private void checkValidDuration(StartTime tempStartTime, EndTime tempEndTime) throws IllegalValueException {
        if (!tempStartTime.isValidDuration(tempEndTime)) {
            throw new IllegalValueException(Time.MESSAGE_INVALID_DURATION);
        }
    }

    // @@author A0143648Y
    @Override
    public CommandResult execute() throws CommandException {
        assert model != null;
        try {
            originalToDoList = new ToDoList(model.getToDoList());

            model.addTask(toAdd);
            commandResultToUndo = new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
            updateUndoLists();

            UnmodifiableObservableList<ReadOnlyTask> lastShownList = model.getListFromChar(toAdd.getTaskChar());

            TaskIndex indexToBeSelected = new TaskIndex(toAdd.getTaskChar(), lastShownList.indexOf(toAdd));
            EventsCenter.getInstance().post(new JumpToListRequestEvent(indexToBeSelected));
            model.updateSelectedIndexes(indexToBeSelected);

            return new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
        } catch (UniqueTaskList.DuplicateTaskException e) {
            throw new CommandException(MESSAGE_DUPLICATE_TASK);
        }

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

}
