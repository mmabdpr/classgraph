/*
 * This file is part of FastClasspathScanner.
 * 
 * Author: Luke Hutchison
 * 
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 * 
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Luke Hutchison
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.lukehutch.fastclasspathscanner.scanner;

import java.io.File;
import java.io.IOException;

import io.github.lukehutch.fastclasspathscanner.utils.AdditionOrderedSet;
import io.github.lukehutch.fastclasspathscanner.utils.JarUtils;
import io.github.lukehutch.fastclasspathscanner.utils.LogNode;
import io.github.lukehutch.fastclasspathscanner.utils.NestedJarHandler;

/** A class to find the unique ordered classpath elements. */
public class ClasspathOrder {
    final NestedJarHandler nestedJarHandler;

    private final AdditionOrderedSet<RelativePath> classpathOrder = new AdditionOrderedSet<>();

    ClasspathOrder(final NestedJarHandler nestedJarHandler) {
        this.nestedJarHandler = nestedJarHandler;
    }

    /** Get the order of classpath elements. */
    public AdditionOrderedSet<RelativePath> get() {
        return classpathOrder;
    }

    /**
     * Add a classpath element relative to a base file. May be called by a ClassLoaderHandler to add classpath
     * elements that it knows about. ClassLoaders will be called in order.
     * 
     * @param pathElement
     *            the URL or path of the classpath element.
     * @param classLoaders
     *            the ClassLoader(s) that this classpath element was obtained from.
     * @param classpathElementOrderOut
     *            the AdditionOrderedSet to add classpath elements to.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElement(final String pathElement, final ClassLoader[] classLoaders,
            final LogNode log) {
        if (pathElement == null || pathElement.isEmpty()) {
            return false;
        } else if (pathElement.endsWith("*")) {
            if (pathElement.length() == 1 || //
                    (pathElement.length() > 2 && pathElement.charAt(pathElement.length() - 1) == '*'
                            && pathElement.charAt(pathElement.length() - 2) == File.separatorChar)) {
                // Got wildcard path element (allowable for local classpaths as of JDK 6)
                try {
                    final File classpathEltParentDir = new RelativePath(ClasspathFinder.currDirPathStr,
                            pathElement.substring(0, pathElement.length() - 1), classLoaders, nestedJarHandler)
                                    .getFile();
                    if (!classpathEltParentDir.exists()) {
                        if (log != null) {
                            log.log("Directory does not exist for wildcard classpath element: " + pathElement);
                        }
                        return false;
                    }
                    if (!classpathEltParentDir.isDirectory()) {
                        if (log != null) {
                            log.log("Wildcard classpath element is not a directory: " + pathElement);
                        }
                        return false;
                    }
                    final LogNode subLog = log == null ? null
                            : log.log("Including wildcard classpath element: " + pathElement);
                    for (final File fileInDir : classpathEltParentDir.listFiles()) {
                        final String name = fileInDir.getName();
                        if (!name.equals(".") && !name.equals("..")) {
                            // Add each directory entry as a classpath element
                            addClasspathElement(fileInDir.getPath(), classLoaders, subLog);
                        }
                    }
                    return true;
                } catch (final IOException e) {
                    if (log != null) {
                        log.log("Could not add wildcard classpath element " + pathElement + " : " + e);
                    }
                    return false;
                }
            } else {
                if (log != null) {
                    log.log("Wildcard classpath elements can only end with \"" + File.separatorChar
                            + "*\", can't have a partial name and then a wildcard: " + pathElement);
                }
                return false;
            }
        } else {
            final RelativePath classpathEltPath = new RelativePath(ClasspathFinder.currDirPathStr, pathElement,
                    classLoaders, nestedJarHandler);
            if (classpathOrder.add(classpathEltPath)) {
                if (log != null) {
                    log.log("Found classpath element: " + classpathEltPath);
                }
            } else {
                if (log != null) {
                    log.log("Ignoring duplicate classpath element: " + classpathEltPath);
                }
            }
            return true;
        }
    }

    /**
     * Add classpath elements, separated by the system path separator character. May be called by a
     * ClassLoaderHandler to add a path string that it knows about.
     * 
     * @param pathStr
     *            the delimited string of URLs or paths of the classpath.
     * @param classLoaders
     *            the ClassLoader(s) that this classpath was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElements(final String pathStr, final ClassLoader[] classLoaders, final LogNode log) {
        if (pathStr == null || pathStr.isEmpty()) {
            return false;
        } else {
            final String[] parts = JarUtils.smartPathSplit(pathStr);
            if (parts.length == 0) {
                return false;
            } else {
                for (final String pathElement : parts) {
                    addClasspathElement(pathElement, classLoaders, log);
                }
                return true;
            }
        }
    }

    /**
     * Add a classpath element relative to a base file. May be called by a ClassLoaderHandler to add classpath
     * elements that it knows about.
     * 
     * @param pathElement
     *            the URL or path of the classpath element.
     * @param classLoader
     *            the ClassLoader that this classpath element was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElement(final String pathElement, final ClassLoader classLoader, final LogNode log) {
        return addClasspathElement(pathElement, new ClassLoader[] { classLoader }, log);
    }

    /**
     * Add classpath elements, separated by the system path separator character. May be called by a
     * ClassLoaderHandler to add a path string that it knows about.
     * 
     * @param pathStr
     *            the delimited string of URLs or paths of the classpath.
     * @param classLoader
     *            the ClassLoader that this classpath was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathEl)ement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElements(final String pathStr, final ClassLoader classLoader, final LogNode log) {
        return addClasspathElements(pathStr, new ClassLoader[] { classLoader }, log);
    }

    /**
     * Add all classpath elements in another ClasspathElementOrder after the elements in this order.
     * 
     * @param subsequentOrder
     *            the ordering to add after this one.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    boolean addClasspathElements(final ClasspathOrder subsequentOrder) {
        return this.classpathOrder.addAll(subsequentOrder.classpathOrder);
    }
}