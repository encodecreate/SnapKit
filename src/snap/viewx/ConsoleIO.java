/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.viewx;
import java.util.Scanner;

/**
 * This is an interface to the console class to provide static convenience methods.
 */
public class ConsoleIO {

    /**
     * Maps to System.out.print.
     */
    public static void print(Object anObj)  { System.out.print(anObj); }

    /**
     * Maps to System.out.println.
     */
    public static void println()  { System.out.println(); }

    /**
     * Maps to System.out.println.
     */
    public static void println(Object anObj)  { System.out.println(anObj); }

    /**
     * Maps to System.err.println.
     */
    public static void printErr(Object anObj)  { System.err.println(anObj); }

    /**
     * Maps to Scanner(System.in).nextLine().
     */
    public static String readln(String prompt)
    {
        print(prompt);
        if (prompt != null && !prompt.isEmpty() && !Character.isWhitespace(prompt.charAt(prompt.length()-1)))
            print(' ');
        String value = new Scanner(System.in).nextLine();
        println(value);
        return value;
    }

    /**
     * Maps to Scanner(System.in).nextInt().
     */
    public static int readInt(String prompt)
    {
        print(prompt);
        if (prompt != null && !prompt.isEmpty() && !Character.isWhitespace(prompt.charAt(prompt.length()-1)))
            print(' ');
        int value = new Scanner(System.in).nextInt();
        println(value);
        return value;
    }

    /**
     * Maps to Scanner(System.in).nextDouble().
     */
    public static double readDouble(String prompt)
    {
        print(prompt);
        if (prompt != null && !prompt.isEmpty() && !Character.isWhitespace(prompt.charAt(prompt.length()-1)))
            print(' ');
        double value = new Scanner(System.in).nextDouble();
        println(value);
        return value;
    }

    /**
     * Maps to Math.random() * limit.
     */
    public static double random()  { return Math.random(); }

    /**
     * Maps to Math.random() * limit.
     */
    public static int randomInt(int limit)  { return (int) (Math.random() * limit); }

    /**
     * Maps to Math.random * limit.
     */
    public static double randomDouble(double limit)  { return (int) (Math.random() * limit); }

    /**
     * Show object.
     */
    public static void show(Object anObj)
    {
        Console console = Console.getShared();
        console.show(anObj);
    }
}
