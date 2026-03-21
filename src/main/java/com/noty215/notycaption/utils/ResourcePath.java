package com.noty215.notycaption.utils;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class ResourcePath {
    private static final java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(ResourcePath.class.getName());
    
    public static String getResourcePath(String relativePath) {
        URL resource = ResourcePath.class.getResource("/" + relativePath);
        if (resource != null) {
            return resource.getPath();
        }
        return relativePath;
    }
    
    public static InputStream getResourceStream(String relativePath) {
        return ResourcePath.class.getResourceAsStream("/" + relativePath);
    }
    
    public static ImageIcon getImageIcon(String relativePath) {
        URL resource = ResourcePath.class.getResource("/" + relativePath);
        if (resource != null) {
            return new ImageIcon(resource);
        }
        return null;
    }
    
    public static String getAppDataDir() {
        return System.getProperty("user.home") + "/.notycaption";
    }
    
    public static File getFile(String relativePath) {
        URL resource = ResourcePath.class.getResource("/" + relativePath);
        if (resource != null) {
            return new File(resource.getPath());
        }
        return new File(relativePath);
    }
    
    public static boolean resourceExists(String relativePath) {
        return ResourcePath.class.getResource("/" + relativePath) != null;
    }
}