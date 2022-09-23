package io.cloudbeat.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Locale;

public class ConsoleOutputWrapper extends PrintStream {
    public ConsoleOutputWrapper() throws FileNotFoundException {
        super(new FileOutputStream("NUL:"));
    }

    @Override
    public void flush() {
        super.flush();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public boolean checkError() {
        return super.checkError();
    }

    @Override
    protected void setError() {
        super.setError();
    }

    @Override
    protected void clearError() {
        super.clearError();
    }

    @Override
    public void write(int b) {
        super.write(b);
    }

    @Override
    public void write(@NotNull byte[] buf, int off, int len) {
        super.write(buf, off, len);
    }

    @Override
    public void print(boolean b) {
        super.print(b);
    }

    @Override
    public void print(char c) {
        super.print(c);
    }

    @Override
    public void print(int i) {
        super.print(i);
    }

    @Override
    public void print(long l) {
        super.print(l);
    }

    @Override
    public void print(float f) {
        super.print(f);
    }

    @Override
    public void print(double d) {
        super.print(d);
    }

    @Override
    public void print(@NotNull char[] s) {
        super.print(s);
    }

    @Override
    public void print(@Nullable String s) {
        super.print(s);
    }

    @Override
    public void print(@Nullable Object obj) {
        super.print(obj);
    }

    @Override
    public void println() {
        super.println();
    }

    @Override
    public void println(boolean x) {
        super.println(x);
    }

    @Override
    public void println(char x) {
        super.println(x);
    }

    @Override
    public void println(int x) {
        super.println(x);
    }

    @Override
    public void println(long x) {
        super.println(x);
    }

    @Override
    public void println(float x) {
        super.println(x);
    }

    @Override
    public void println(double x) {
        super.println(x);
    }

    @Override
    public void println(@NotNull char[] x) {
        super.println(x);
    }

    @Override
    public void println(@Nullable String x) {
        super.println(x);
    }

    @Override
    public void println(@Nullable Object x) {
        super.println(x);
    }

    @Override
    public PrintStream printf(@NotNull String format, Object... args) {
        return super.printf(format, args);
    }

    @Override
    public PrintStream printf(Locale l, @NotNull String format, Object... args) {
        return super.printf(l, format, args);
    }

    @Override
    public PrintStream format(@NotNull String format, Object... args) {
        return super.format(format, args);
    }

    @Override
    public PrintStream format(Locale l, @NotNull String format, Object... args) {
        return super.format(l, format, args);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        return super.append(csq);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        return super.append(csq, start, end);
    }

    @Override
    public PrintStream append(char c) {
        return super.append(c);
    }
}
