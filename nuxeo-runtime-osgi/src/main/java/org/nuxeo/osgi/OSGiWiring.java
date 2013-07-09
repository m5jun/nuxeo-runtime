package org.nuxeo.osgi;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class OSGiWiring {

    protected final Map<String, Set<OSGiLoader>> contents = new HashMap<String, Set<OSGiLoader>>();

    protected final OSGiLoader loader;

    protected OSGiWiring(OSGiLoader loader) {
        this.loader = loader;
    }

    protected void wire(String path, Set<OSGiLoader> loaders) {
        if (!contents.containsKey(path)) {
            contents.put(path, new HashSet<OSGiLoader>());
        }
        Set<OSGiLoader> set = contents.get(path);
        set.addAll(loaders);
    }

    protected void wire(String path, OSGiLoader loader) {
        if (!contents.containsKey(path)) {
            contents.put(path, new HashSet<OSGiLoader>());
        }
        Set<OSGiLoader> set = contents.get(path);
        set.add(loader);
    }

    protected void unwire(String path, Set<OSGiLoader> loaders) {
        Set<OSGiLoader> set = contents.get(path);
        if (set != null) {
            set.removeAll(loaders);
        }
    }

    protected String bundlePath(URL location) {
        String protocol = location.getProtocol();
        String bundlePath = loader.getPath();
        if ("jar".equals(protocol)) {
            return jarPath(bundlePath, location.getPath());
        } else if ("file".equals(protocol)) {
            return filePath(bundlePath, location.getPath());
        }
        return null;
    }

    protected String jarPath(String jarPath, String filePath) {
        return filePath.substring(5 + jarPath.length() + 2);
    }

    protected String filePath(String rootPath, String filePath) {
        if (!filePath.startsWith(rootPath)) {
            return null; // tycho+maven hack
        }
        return filePath.substring(rootPath.length() + 1);
    }

    public void merge(OSGiWiring wiring) {
        for (Map.Entry<String, Set<OSGiLoader>> entry : wiring.contents.entrySet()) {
            wire(entry.getKey(), entry.getValue());
        }
    }

    public void substract(OSGiWiring wiring) {
        for (Map.Entry<String, Set<OSGiLoader>> entry : wiring.contents.entrySet()) {
            wire(entry.getKey(), entry.getValue());
        }
    }

    protected void load() {
        Enumeration<URL> entries = loader.listLocalFiles();
        while (entries.hasMoreElements()) {
            URL entry = entries.nextElement();
            String path = bundlePath(entry);
            if (path == null) {
                continue;
            }
            if (path.equals("META-INF/MANIFEST.MF")) {
                continue;
            }
            if (path.startsWith("META-INF")) {
                wire(path, loader);
                continue;
            }
            wire(shrinkPath(path), loader);
        }
    }

    public Set<OSGiLoader> mayContains(String path) {
        Set<OSGiLoader> loaders = contents.get(shrinkPath(path));
        if (loaders == null) {
            return Collections.emptySet();
        }
        return loaders;
    }

    protected String shrinkPath(String path) {
        String components[] = path.split("/");
        return shrinkPath(3, components.length - 2, components);
    }

    protected String shrinkPath(int maxDepth, int index, String[] components) {
        if (maxDepth <= 0) {
            return "";
        }
        if (index <= 0) {
            return "/" + components[0];
        }
        return shrinkPath(maxDepth - 1, index - 1, components) + "/"
                + components[index];
    }

    public void clear() {
        contents.clear();
    }

}