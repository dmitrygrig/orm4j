/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

import java.util.Date;
import java.util.List;

/**
 *
 * @author grig
 */
public interface ILogger {
    public void log(String line);
    public String getLog();
    public void clearLog();
    public void flush();
    public void flush(String filename);
}
