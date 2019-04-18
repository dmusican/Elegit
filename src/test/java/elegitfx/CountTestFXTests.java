package elegitfx;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CountTestFXTests {

    @Test
    public void assertTestCountIsLessThanOrEqualsOne() {
        Package pkg = Package.getPackage("elegitfx");
        List<Class> classes = getClassesForPackage(pkg);
        try {
            for (Class aClass : classes) {
                assertTrue("There is more than one test case in " + aClass.getName(),countTestCases(aClass) <= 1);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private int countTestCases(Class<?> testFXClass) throws ClassNotFoundException {
        testFXClass.getClass();
        Class<?> className = Class.forName(testFXClass.getName());
        Method[] methods = className.getMethods();

        // Filter out the methods that contain test annotation and return length
        return  Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Test.class)).toArray().length;
    }

    private static List<Class> getClassesForPackage(Package pkg) {
        String pkgname = pkg.getName();

        List<Class> classes = new ArrayList<>();

        // Get a File object for the package
        File directory;
        String fullPath;
        String relPath = pkgname.replace('.', '/');

        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);

        if (resource == null) {
            throw new RuntimeException("No resource for " + relPath);
        }
        fullPath = resource.getFile();

        try {
            directory = new File(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(pkgname + " (" + resource + ") does not appear to be a valid URL / URI.  Strange, since we got it from the system...", e);
        } catch (IllegalArgumentException e) {
            directory = null;
        }

        if (directory != null && directory.exists()) {

            // Get the list of the files contained in the package
            String[] files = directory.list();
            for (String file : files) {

                // we are only interested in .class files
                if (file.endsWith(".class")) {

                    // removes the .class extension
                    String className = pkgname + '.' + file.substring(0, file.length() - 6);

                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("ClassNotFoundException loading " + className);
                    }
                }
            }
        } else {
            try {
                String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
                JarFile jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                        String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                        try {
                            classes.add(Class.forName(className));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("ClassNotFoundException loading " + className);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(pkgname + " (" + directory + ") does not appear to be a valid package", e);
            }
        }
        return classes;
    }
}
