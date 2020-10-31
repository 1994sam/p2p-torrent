package org.networks.java.service;

import org.networks.java.helper.Const;

import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

public class P2PLogger {

	private static Logger logger = null;

	public P2PLogger() {}

	public static void setLogger(String peerId) {
		String logFile = Const.LOG_FILE_PATH + peerId + Const.LOG_FILE_EXTENSTION;

		try {
			FileHandler fileHandler = new FileHandler(logFile);
			fileHandler.setFormatter(new SimpleFormatter() {
				private static final String format = "[%1$tF %1$tT] %2$s %n";

				@Override
				public synchronized String format(LogRecord logRecord) {
					return String.format(format, new Date(logRecord.getMillis()), logRecord.getMessage()
					);
				}
			});

			logger = Logger.getLogger("org.networks");
			logger.setUseParentHandlers(false);
			logger.addHandler(fileHandler);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Logger getLogger() {
		return logger;
	}
}
