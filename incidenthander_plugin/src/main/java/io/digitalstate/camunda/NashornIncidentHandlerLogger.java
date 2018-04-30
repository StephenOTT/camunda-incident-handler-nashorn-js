package io.digitalstate.camunda;

import org.camunda.commons.logging.BaseLogger;

public class NashornIncidentHandlerLogger extends BaseLogger {

  public static NashornIncidentHandlerLogger LOG = createLogger(
    NashornIncidentHandlerLogger.class, "NashornIncidentHandlerLogger", "io.digitalstate.camunda", "NashornIncidentHandler"
  );

  public void debug(String debugId, String message) {
    logDebug(debugId, message);
  }

  public void info(String infoId, String message) {
    logInfo(infoId, message);
  }

  public void error(String errorId, String message) {
    logError(errorId, message);
  }
}