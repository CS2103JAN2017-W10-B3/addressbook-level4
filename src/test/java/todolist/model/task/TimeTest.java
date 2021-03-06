package todolist.model.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import todolist.commons.exceptions.IllegalValueException;
import todolist.commons.util.TimeUtil;

//@@author A0122017Y
public class TimeTest {

    @Test
    public void isValidTime() {
        // blank EndTime
        assertFalse(Time.isValidTime("")); // empty string

        // valid Time
        assertTrue(Time.isValidTime("March Fifteenth")); //alphabets
        assertTrue(Time.isValidTime("20170315")); // number
        assertTrue(Time.isValidTime("By end of March")); // alphabets with capital
        assertTrue(Time.isValidTime("March 15 2017")); // numeric and alphabet and domain name
        assertTrue(Time.isValidTime("March 15, 2017")); // mixture of alphanumeric and comma characters
    }

    @Test
    public void isValidDuration() throws IllegalValueException {
        StartTime start1 = new StartTime("April 30");
        StartTime start2 = new StartTime("April 30 2017");
        StartTime start3 = new StartTime("Tomorrow");

        EndTime end1 = new EndTime("May 30");
        EndTime end2 = new EndTime("Today");

        assertTrue(TimeUtil.isValidDuration(start1, end1));
        assertTrue(start1.isSameDay(start2));
        assertFalse(TimeUtil.isValidDuration(start3, end2));
    }

}
