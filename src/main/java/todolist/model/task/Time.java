package todolist.model.task;

import java.time.LocalDateTime;

//@@author A0122017Y
public interface Time extends Comparable<Time> {

    public static final String TIME_VALIDATION_REGEX = ".+";
    public static final String MESSAGE_INVALID_TIME = "Time format is invalid!";
    public static final String MESSAGE_INVALID_DURATION = "Starting time must be before ending time!";
    public static final String MESSAGE_TIME_CONSTRAINTS = "Task start time should be in the form of "
            + "DD/MM/YYYY HH:MM, e.g 20/03/2017 4:18 \n"
            + "Or name of the day, e.g Wed 4:18 \n"
            + "Or relative days, e.g tomorrow 4:18 \n"
            + "Notice that no abbreviation is accepted for relatives. e.g tmrw is invalid. ";

    static Boolean isValidTime(String test) {
        return test.matches(TIME_VALIDATION_REGEX);
    }

    LocalDateTime getTimeValue();

    @Override
    int compareTo(Time time);
}
