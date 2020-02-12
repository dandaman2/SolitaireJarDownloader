package jarloader;
/* From http://java.sun.com/docs/books/tutorial/index.html */
/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;

/**
 * Runs a jar application from any url. Usage is 'java JarRunner url [args..]'
 * where url is the url of the jar file and args is optional arguments to be
 * passed to the application's main method.
 */
public class JarRunner{

    private String url;

    public JarRunner(String u){
        url = u;
        getAndRun(u);
    }

    public void getAndRun(String s){
        String[] a = new String[1];
        a[0] = s;
        run1(a);
    }

    private void run1(String[] args) {
        if (args.length < 1) {
            usage();
        }
        URL url = null;
        try {
            url = new URL(args[0]);
        } catch (MalformedURLException e) {
            fatal("Invalid URL: " + args[0]);
        }
        // Create the class loader for the application jar file
        JarClassLoader cl = new JarClassLoader(url);
        // Get the application's main class name
        String name = null;
        try {
            name = cl.getMainClassName();
        } catch (IOException e) {
            System.err.println("I/O error while loading JAR file:");
            e.printStackTrace();
            System.exit(1);
        }
        if (name == null) {
            fatal("Specified jar file does not contain a 'Main-Class'"
                    + " manifest attribute");
        }
        // Get arguments for the application
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        // Invoke application's main class
        try {
            cl.invokeClass(name, newArgs);
        } catch (ClassNotFoundException e) {
            fatal("Class not found: " + name);
        } catch (NoSuchMethodException e) {
            fatal("Class does not define a 'main' method: " + name);
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            System.exit(1);
        }
    }

    private static void fatal(String s) {
        System.err.println(s);
        System.exit(1);
    }

    private static void usage() {
        fatal("Usage: java JarRunner url [args..]");
    }

}

/**
 * A class loader for loading jar files, both local and remote.
 */

class JarClassLoader extends URLClassLoader {
    private URL url;

    /**
     * Creates a new JarClassLoader for the specified url.
     *
     * @param url
     *            the url of the jar file
     */
    public JarClassLoader(URL url) {
        super(new URL[] { url });
        this.url = url;
    }

    //returns always-true for class loading
//    public boolean getTrust(Class c){
//        return true;
//    }

    /**
     * Returns the name of the jar file main class, or null if no "Main-Class"
     * manifest attributes was defined.
     */
    public String getMainClassName() throws IOException {
        URL u = new URL("jar", "", url + "!/");
        JarURLConnection uc = (JarURLConnection) u.openConnection();
        Attributes attr = uc.getMainAttributes();
        return attr != null ? attr.getValue(Attributes.Name.MAIN_CLASS) : null;
    }

    /**
     * Invokes the application in this jar file given the name of the main class
     * and an array of arguments. The class must define a static method "main"
     * which takes an array of String arguemtns and is of return type "void".
     *
     * @param name
     *            the name of the main class
     * @param args
     *            the arguments for the application
     * @exception ClassNotFoundException
     *                if the specified class could not be found
     * @exception NoSuchMethodException
     *                if the specified class does not contain a "main" method
     * @exception InvocationTargetException
     *                if the application raised an exception
     */
    public void invokeClass(String name, String[] args)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException {
        Class c = loadClass(name);
        Method m = c.getMethod("main", new Class[] { args.getClass() });
        m.setAccessible(true);
        int mods = m.getModifiers();
        if (m.getReturnType() != void.class || !Modifier.isStatic(mods)
                || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("main");
        }
        try {
            m.invoke(null, new Object[] { args });
        } catch (IllegalAccessException e) {
            // This should not happen, as we have disabled access checks
        }
    }

}



