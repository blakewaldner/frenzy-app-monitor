import java.net.*;
import java.util.logging.*;

public class ThreadLocalAuthenticator extends Authenticator
{
    private static final Logger logger;
    private static final ThreadLocal<String> proxy_password;
    private static final ThreadLocal<String> proxy_username;
    private static final ThreadLocal<String> server_password;
    private static final ThreadLocal<String> server_username;
    private static final ThreadLocalAuthenticator threadAuthenticator;
    
    static {
        logger = Logger.getLogger(ThreadLocalAuthenticator.class.getName());
        proxy_password = new ThreadLocal<String>();
        proxy_username = new ThreadLocal<String>();
        server_password = new ThreadLocal<String>();
        server_username = new ThreadLocal<String>();
        threadAuthenticator = new ThreadLocalAuthenticator();
    }
    
    public static ThreadLocalAuthenticator getAuthenticator() {
        return ThreadLocalAuthenticator.threadAuthenticator;
    }
    
    public static void setAsDefault() {
        Authenticator.setDefault(ThreadLocalAuthenticator.threadAuthenticator);
    }
    
    public static void setProxyAuth(final String username, final String password) {
        ThreadLocalAuthenticator.proxy_username.set(username);
        ThreadLocalAuthenticator.proxy_password.set(password);
    }
    
    public static void setServerAuth(final String username, final String password) {
        ThreadLocalAuthenticator.server_username.set(username);
        ThreadLocalAuthenticator.server_password.set(password);
    }
    
    private ThreadLocalAuthenticator() {
    }
    
    public PasswordAuthentication getPasswordAuthentication() {
        String username = null;
        String password = null;
        if (this.getRequestorType() == RequestorType.PROXY) {
            username = ThreadLocalAuthenticator.proxy_username.get();
            password = ThreadLocalAuthenticator.proxy_password.get();
            final String[] params = { this.getRequestingHost(), username };
            ThreadLocalAuthenticator.logger.log(Level.FINER, "Proxy auth for {0}: username={1}", params);
        }
        else if (this.getRequestorType() == RequestorType.SERVER) {
            username = ThreadLocalAuthenticator.server_username.get();
            password = ThreadLocalAuthenticator.server_password.get();
            final String[] params = { this.getRequestingHost(), username };
            ThreadLocalAuthenticator.logger.log(Level.FINER, "Server auth for {0}: username={1}", params);
        }
        if (username == null || password == null) {
            return null;
        }
        return new PasswordAuthentication(username, password.toCharArray());
    }
}
