package todolist.logic;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import todolist.logic.commands.HelpCommand;
import todolist.model.ToDoList;
import todolist.model.task.Task;

//@@author A0122017Y
public class HelpCommandTest extends LogicManagerTest {

    @Test
    public void executeHelp() {
        assertCommandSuccess("help", HelpCommand.showingHelpMessage,
                new ToDoList(), Collections.emptyList(), Task.ALL_CHAR);
        assertTrue(helpShown);
    }

}
