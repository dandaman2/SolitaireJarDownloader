package sample;

import java.security.Permission;

class PreventExitSecurityManager extends SecurityManager {
    @Override
    public void checkExit(int status) {
        throw new SecurityException("Stopping JAR from exiting entire application");
    }

    @Override
    public void checkCreateClassLoader(){}

    @Override
    public void checkConnect(String host, int port){}

    @Override
    public void checkPermission(Permission perm,
                                Object context){

    }

    @Override
    public void checkPermission(Permission perm){}
}
