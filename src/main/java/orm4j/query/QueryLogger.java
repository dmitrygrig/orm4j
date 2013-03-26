/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import orm4j.util.GlobalController;

/**
 *
 * @author grig
 */
public class QueryLogger implements ILogger {

    public class LogData {

        public LogData(Date date, String logText) {
            this.date = date;
            this.logText = logText;
        }
        private Date date;
        private String logText;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getLogText() {
            return logText;
        }

        public void setLogText(String logText) {
            this.logText = logText;
        }
    }
    private List<LogData> logList;
    private static final String logFilename = "orm4j_query.log";

    public QueryLogger() {
        logList = new ArrayList<LogData>();
    }

    public void log(String line) {
        Date date = Calendar.getInstance().getTime();
        logList.add(new LogData(date, line));
    }

    public void clearLog() {
        logList.clear();
    }

    public void flush() {
        flush(logFilename);
    }

    public void flush(String filename) {
        // if log is big,
        // write it by line in order to exclude OutOfMemory exception
        if (logList.size() > 1000) {
            flushBuffered(filename);
        } else {
            try {


                GlobalController.writeFile(filename, getLog());
            } catch (IOException ex) {
                Logger.getLogger(QueryLogger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void flushBuffered(String filename) {
        int bufferSize = 10000;
        List<String> buffer = new ArrayList<String>();;

        try {
            for (LogData data : logList) {
                if (buffer.size() < bufferSize) {
                    String formatLine = getLogLine(data.getDate(), data.getLogText());
                    buffer.add(formatLine);
                } else {
                    String textToWrite = GlobalController.getStringFromLines(buffer);
                    GlobalController.writeFile(filename, textToWrite);
                    buffer.clear();
                }
            }
            String textToWrite = GlobalController.getStringFromLines(buffer);
            GlobalController.writeFile(filename, textToWrite);
        } catch (IOException ex) {
            Logger.getLogger(QueryLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getLog() {
        StringBuilder sb = new StringBuilder();

        for (LogData data : logList) {
            String formatLine = getLogLine(data.getDate(), data.getLogText());
            sb.append(formatLine);
        }

        return sb.toString();
    }

    private String getLogLine(Date date, String logText) {
        Format formatter = GlobalController.getDatetimeFormatter("yyyy-MM-dd HH:mm:ss");
        String formatDate = formatter.format(date);
        return String.format("[%s]: %s\r\n", formatDate, logText);
    }
}
