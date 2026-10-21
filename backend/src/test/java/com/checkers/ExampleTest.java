package com.checkers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ExampleTest {

    @Test
    public void testAddition() {
        int expected = 5;
        int actual = 2 + 3;
        
        // Assertions verify if the code works as expected
        assertEquals(expected, actual, "2 + 3 should equal 5");
    }
}
