package todolist.logic.parser;

import static todolist.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.ArrayList;
import java.util.Optional;

import todolist.logic.commands.Command;
import todolist.logic.commands.CompleteCommand;
import todolist.logic.commands.DeleteCommand;
import todolist.logic.commands.IncorrectCommand;
import todolist.model.task.TaskIndex;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class CompleteCommandParser {

    private static Optional<ArrayList<TaskIndex>> indexes;

    /**
     * Parses the given {@code String} of arguments in the context of the
     * DeleteCommand and returns an DeleteCommand object for execution.
     */
    public Command parse(String args) {

        Optional<ArrayList<TaskIndex>> indexes = ParserUtil.parseIndex(args);
        if (!indexes.isPresent()) {
            indexes = CompleteCommandParser.indexes;
            if (!indexes.isPresent()) {
                return new IncorrectCommand(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
            }
        }

        return new CompleteCommand(indexes.get());
    }

    public static void setIndex(ArrayList<TaskIndex> indexes) {
        CompleteCommandParser.indexes = Optional.of(indexes);
    }

}
