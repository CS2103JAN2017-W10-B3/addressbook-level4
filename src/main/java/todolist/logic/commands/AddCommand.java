package todolist.logic.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import todolist.commons.core.EventsCenter;
import todolist.commons.core.LogsCenter;
import todolist.commons.core.UnmodifiableObservableList;
import todolist.commons.events.ui.JumpToListRequestEvent;
import todolist.commons.exceptions.IllegalValueException;
import todolist.commons.util.TimeUtil;
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

    private Logger logger = LogsCenter.getLogger(AddCommand.class.getName());

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a Task to the to-do list. \n"
            + "General usage: add TITLE [/venue <VENUE>] [/from <STARTTIME>] [/to <ENDTIME>] "
            + "[/by <DEADLINE>] [/level <IMPORTANCE>] [/description <DESCRIPTION>] [#<TAGS...>] "
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
    public AddCommand(String title, Optional<String> venue, Optional<String> starttime,
            Optional<String> beginningtime, Optional<String> endtime,
            Optional<String> deadline, Optional<String> urgencyLevel,
            Optional<String> description, Set<String> tags)
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

        TimeUtil.checkTimeDuplicated(starttime, beginningtime, endtime, deadline);

        if (starttime.isPresent() && !beginningtime.isPresent()) {
            tempStartTime = new StartTime(starttime.get());
        }

        if (!starttime.isPresent() && beginningtime.isPresent()) {
            tempStartTime = new StartTime(beginningtime.get());
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

        if (tempStartTime == null && tempEndTime != null) {
            checkValidDuration(tempEndTime);
        }

        this.toAdd = new Task(tempTitle, tempVenue, tempStartTime, tempEndTime, tempUrgencyLevel, tempDescription,
                new UniqueTagList(tagSet));
    }

    private void checkValidDuration(StartTime tempStartTime, EndTime tempEndTime) throws IllegalValueException {
        if (!TimeUtil.isValidDuration(tempStartTime, tempEndTime)) {
            throw new IllegalValueException(Time.MESSAGE_INVALID_DURATION);
        }
    }

    private void checkValidDuration(EndTime tempEndTime) throws IllegalValueException {
        if (!TimeUtil.isValidDeadline(tempEndTime)) {
            throw new IllegalValueException(Time.MESSAGE_INVALID_DURATION);
        }
    }

  //@@author A0143648Y
    @Override
    public CommandResult execute() throws CommandException {
        logger.info("-------[Executing AddCommand]");

        assert model != null;
        try {
            originalToDoList = new ToDoList(model.getToDoList());

            model.addTask(toAdd);
            commandResultToUndo = new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
            updateUndoLists();

            UnmodifiableObservableList<ReadOnlyTask> lastShownList = model.getListFromChar(toAdd.getTaskChar());

            TaskIndex indexToBeSelected = new TaskIndex(toAdd.getTaskChar(), lastShownList.indexOf(toAdd));
            EventsCenter.getInstance().post(new JumpToListRequestEvent(indexToBeSelected));

            logger.info("-------[Executed AddCommand]");

            model.updateSelectedIndexes(indexToBeSelected);

            return new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
        } catch (UniqueTaskList.DuplicateTaskException e) {
            logger.info("-------[Execution Of AddCommand Failed]");
            throw new CommandException(MESSAGE_DUPLICATE_TASK);
        }

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
