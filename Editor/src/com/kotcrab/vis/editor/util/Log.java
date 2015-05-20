/*
 * Copyright 2014-2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotcrab.vis.editor.util;

import com.kotcrab.vis.editor.App;

import javax.swing.JOptionPane;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log utility, log events are redirected to listener and printed to standard output
 * @author Pawel Pastuszak
 */
public class Log {
	public static final int OFF = 5;
	public static final int DEBUG = 4;
	public static final int INFO = 3;
	public static final int WARN = 2;
	public static final int ERROR = 1;
	public static final int FATAL = 0;

	private static final boolean DEBUG_INTERRUPTED = false;

	private static int logLevel = DEBUG;

	private static File logFile;
	private static PrintWriter logFileWriter;
	private static SimpleDateFormat msgDateFormat = new SimpleDateFormat("[HH:mm]");

	public static void init () {
		System.setErr(new ErrorStreamInterceptor(System.err));

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			Log.exception(e);
			Log.fatal("Uncaught exception occurred, error report will be saved");

			logFileWriter.flush();

			boolean openGlCrash = false;
			if (e.getMessage().contains("No OpenGL context found in the current thread."))
				openGlCrash = true;

			try {
				new CrashReporter(logFile).processReport();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			if (openGlCrash) {
				if (App.OPENGL_CRASH_BEFORE_EXIT_MESSAGE)
					JOptionPane.showMessageDialog(null, "An unexpected error occurred and editor had to shutdown, please check log: " + logFile.getParent());

				System.exit(-1);
			}
		});

		prepareLogFile();
	}

	public static void dispose () {
		info("Exiting");
		logFileWriter.close();
	}

	private static void prepareLogFile () {
		File logDirectory = new File(App.APP_FOLDER_PATH, "logs");
		logDirectory.mkdir();

		SimpleDateFormat fileDateFormat = new SimpleDateFormat("yy-MM-dd");
		String fileName = fileDateFormat.format(new Date());
		String fileNameYearMonth = fileName.substring(0, 5);

		File[] files = logDirectory.listFiles();

		//we are deleting files that are not from current month
		for (File f : files)
			if (f.getName().contains(fileNameYearMonth) == false) f.delete();

		try {
			logFile = new File(logDirectory, "viseditor " + fileName + ".txt");
			logFile.createNewFile();
			logFileWriter = new PrintWriter(new FileWriter(logFile, true));
		} catch (IOException e) {
			exception(e);
		}

		logFileWriter.println();
		info("VisEditor " + App.VERSION);
		info("Started: " + fileName);
	}

	// Standard log

	public static void debug (String msg) {
		if (logLevel >= DEBUG) print("[Debug] " + msg);
	}

	public static void info (String msg) {
		if (logLevel >= INFO) print("[Info] " + msg);
	}

	public static void warn (String msg) {
		if (logLevel >= WARN) print("[Warning] " + msg);
	}

	public static void error (String msg) {
		if (logLevel >= ERROR) printErr("[Error] " + msg);
	}

	public static void fatal (String msg) {
		if (logLevel >= FATAL) printErr("[Fatal] " + msg);
	}

	//Log with tag

	public static void debug (String tag, String msg) {
		if (logLevel >= DEBUG) print("[Debug][" + tag + "] " + msg);
	}

	public static void info (String tag, String msg) {
		if (logLevel >= INFO) print("[Info][" + tag + "] " + msg);
	}

	public static void warn (String tag, String msg) {
		if (logLevel >= WARN) print("[Warning][" + tag + "] " + msg);
	}

	public static void error (String tag, String msg) {
		if (logLevel >= ERROR) printErr("[Error][" + tag + "] " + msg);
	}

	public static void fatal (String tag, String msg) {
		if (logLevel >= FATAL) printErr("[Fatal][" + tag + "] " + msg);
	}

	private static void print (String msg) {
		msg = getTimestamp() + msg;
		logFileWriter.println(msg);
		System.out.println(msg);
	}

	private static void printErr (String msg) {
		msg = getTimestamp() + msg;
		System.err.println(msg);
	}

	public static void exception (Throwable e) {
		if (e instanceof InterruptedException && DEBUG_INTERRUPTED == false) return;

		String stack = ExceptionUtils.getStackTrace(e);
		fatal(stack);
	}

	private static String getTimestamp () {
		return msgDateFormat.format(new Date());
	}

	private static class ErrorStreamInterceptor extends PrintStream {
		public ErrorStreamInterceptor (OutputStream out) {
			super(out, true);
		}

		@Override
		public void print (String s) {
			super.print(s);
			if (logFileWriter != null) logFileWriter.println(s);
		}
	}
}


