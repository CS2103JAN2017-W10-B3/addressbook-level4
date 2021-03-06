package todolist.logic;

import static todolist.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;

import todolist.commons.core.LogsCenter;
import todolist.logic.commands.Command;
import todolist.logic.commands.FindCommand;
import todolist.logic.commands.ListCommand;
import todolist.model.ToDoList;
import todolist.model.task.ReadOnlyTask;
import todolist.model.task.Task;

//@@author A0122017Y
public class ListCommandTest extends LogicManagerTest {
    private static final Logger logger = LogsCenter.getLogger(ListCommandTest.class);

    @Test
    public void executeFindInvalidArgsFormat() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertCommandFailure("find ", expectedMessage);
    }

    @Test
    public void executeListWithinAnInterval() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task target1 = helper.generateEventTaskWithStartTime("13 April 2017");
        Task target2 = helper.generateEventTaskWithStartTime("14 April 2017");
        Task p1 = helper.generateEventTaskWithEndTime("30 April 2017");
        Task p2 = helper.generateEventTaskWithEndTime("29 April 2017");

        List<Task> fourTasks = helper.generateTaskList(p1, target1, p2, target2);
        ToDoList expectedAB = helper.generateToDoList(fourTasks);
        List<Task> expectedList = helper.generateTaskList(target1, target2);
        helper.addToModel(model, fourTasks);

        assertCommandSuccess("list /from 13 April 2017",
                Command.getMessageForTaskListShownSummary(expectedList.size()),
                expectedAB,
                expectedList, Task.EVENT_CHAR);
    }

    @Test
    public void executeFindIsNotCaseSensitive() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task target1 = helper.generateDeadlineTaskWithEndTime("13 April 2017");
        Task target2 = helper.generateDeadlineTaskWithEndTime("14 April 2017");
        Task p1 = helper.generateDeadlineTaskWithEndTime("30 April 2017");
        Task p2 = helper.generateDeadlineTaskWithEndTime("29 April 2017");

        List<Task> fourTasks = helper.generateTaskList(p1, target1, p2, target2);
        ToDoList expectedAB = helper.generateToDoList(fourTasks);
        List<Task> expectedList1 = helper.generateTaskList(p1, target1, p2, target2);
        List<Task> expectedList2 = helper.generateTaskList(target1);
        helper.addToModel(model, fourTasks);

        assertCommandSuccess("list /by 30 April 2017",
                Command.getMessageForTaskListShownSummary(expectedList1.size()),
                expectedAB,
                expectedList1, Task.EVENT_CHAR);

        assertCommandSuccess("list /to 13 April 2017",
                Command.getMessageForTaskListShownSummary(expectedList2.size()),
                expectedAB,
                expectedList2, Task.EVENT_CHAR);

    }

    @Test
    public void execute_list_showsAllTasks() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        ToDoList expectedAB = helper.generateToDoList(2);
        List<? extends ReadOnlyTask> expectedList = expectedAB.getTaskList();

        // prepare to-do list state
        helper.addToModel(model, 2);

        assertCommandSuccess("list",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                expectedList, Task.ALL_CHAR);
    }

}
