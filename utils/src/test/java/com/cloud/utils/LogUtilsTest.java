package com.cloud.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class LogUtilsTest {

    @Test
    public void logGsonWithoutExceptionTestLogCorrectlyPrimitives() {
        String expected = "test primitives: int [1], double [1.11], float [1.2222], boolean [true], null [], char [\"c\"].";
        String log = LogUtils.logGsonWithoutException("test primitives: int [%s], double [%s], float [%s], boolean [%s], null [%s], char [%s].",
                1, 1.11d, 1.2222f, true, null, 'c');
        assertEquals(expected, log);
    }

    @Test
    public void logGsonWithoutExceptionTestPassWrongNumberOfArgs() {
        String expected = "Failed to log objects using GSON due to: [Format specifier '%s'].";
        String result = LogUtils.logGsonWithoutException("teste wrong [%s] %s args.", "blablabla");
        assertEquals(expected, result);
    }
}