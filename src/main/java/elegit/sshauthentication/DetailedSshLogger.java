package elegit.sshauthentication;

import static com.jcraft.jsch.Logger.DEBUG;
import static com.jcraft.jsch.Logger.ERROR;
import static com.jcraft.jsch.Logger.FATAL;
import static com.jcraft.jsch.Logger.INFO;
import static com.jcraft.jsch.Logger.WARN;

// http://www.jcraft.com/jsch/examples/Logger.java.html
public class DetailedSshLogger implements com.jcraft.jsch.Logger {

        static java.util.Hashtable<Integer,String> name=new java.util.Hashtable<>();
        static{
            name.put(DEBUG, "DEBUG: ");
            name.put(INFO, "INFO: ");
            name.put(WARN, "WARN: ");
            name.put(ERROR, "ERROR: ");
            name.put(FATAL, "FATAL: ");
        }
        public boolean isEnabled(int level){
            return true;
        }
        public void log(int level, String message){
            System.err.print(name.get(level));
            System.err.println(message);
        }


}
