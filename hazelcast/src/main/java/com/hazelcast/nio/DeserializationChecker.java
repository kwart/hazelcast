/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.nio;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.hazelcast.util.StringUtil;

/**
 * A helper class which allows to check deserialized class names (and their packages) against configured whitelist and
 * blacklist. The configuration is done through system properties {@value #PROPERTY_BLACKLIST} and {@value #PROPERTY_WHITELIST}
 * - the values are separated by comma (','). If the whitelist is null or empty, whitelisting will be disabled and all classes
 * are considered to appear in it. The blacklist is always checked first.
 */
public final class DeserializationChecker {

    /**
     * Name of system property which sets deserialization whitelist.
     */
    public static final String PROPERTY_WHITELIST = "hazelcast.deserialization.whitelist";
    /**
     * Name of system property which sets deserialization blacklist.
     */
    public static final String PROPERTY_BLACKLIST = "hazelcast.deserialization.blacklist";

    /**
     * A default blacklist (classes usable in deserialization exploit gadgets).
     */
    private static final String DEFAULT_BLACKLIST = "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl,"
            + "bsh.XThis,org.apache.commons.beanutils.BeanComparator,"
            + "org.apache.commons.collections.functors,org.apache.commons.collections4.functors,"
            + "org.codehaus.groovy.runtime.ConvertedClosure,org.codehaus.groovy.runtime.MethodClosure,"
            + "org.springframework.beans.factory.ObjectFactory,com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl";

    /**
     * A default whitelist. The {@code null} means all classes are whitelisted.
     */
    private static final String DEFAULT_WHITELIST = null;

    private static final String DESERIALIZATION_ERROR = "Resolving class %s is not allowed.";

    private DeserializationChecker() {
        // private constructor - utility class
    }

    /**
     * Throws {@link ClassNotFoundException} if the given class name appears on the blacklist or does not appear on a non-empty
     * whitelist.
     *
     * @param className class name to check
     * @throws ClassNotFoundException if the classname is not allowed for deserialization
     */
    public static void checkClassNameForResolution(String className) throws ClassNotFoundException {
        ListsConfig current = ListsConfig.getConfig();

        if (current.blacklist != null && current.blacklist.contains(className)) {
            throw new ClassNotFoundException(format(DESERIALIZATION_ERROR, className));
        }

        int dotPosition = className.lastIndexOf(".");
        if (dotPosition > 0) {
            String packageName = className.substring(0, dotPosition);

            if (current.blacklist != null && current.blacklist.contains(packageName)) {
                throw new ClassNotFoundException(format(DESERIALIZATION_ERROR, className));
            }

            if (current.whitelist != null && !current.whitelist.contains(className)
                    && !current.whitelist.contains(packageName)) {
                throw new ClassNotFoundException(format(DESERIALIZATION_ERROR, className));
            }
        } else {
            if (current.whitelist != null && !current.whitelist.contains(className)) {
                throw new ClassNotFoundException(format(DESERIALIZATION_ERROR, className));
            }
        }
    }

    private static String getSystemProperty(final String key, final String def) {
        return getSecurityManager() == null ? getProperty(key, def) : doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return getProperty(key, def);
            }
        });
    }

    /**
     * Class which caches the whitelist and blacklist to avoid unnecessary parsing of the lists.
     */
    private static final class ListsConfig {

        private static volatile ListsConfig listsConfigCache;

        private final String whitelistValue;
        private final String blacklistValue;
        private final Set<String> whitelist;
        private final Set<String> blacklist;

        private ListsConfig(String whitelistValue, String blacklistValue) {
            this.whitelistValue = whitelistValue;
            this.blacklistValue = blacklistValue;
            this.whitelist = parse(whitelistValue);
            this.blacklist = parse(blacklistValue);
        }

        private static Set<String> parse(String listValue) {
            if (StringUtil.isNullOrEmpty(listValue)) {
                return null;
            }
            String[] split = StringUtil.splitByComma(listValue, false);
            if (split.length == 0) {
                return null;
            }
            return new HashSet<String>(Arrays.asList(split));
        }

        static ListsConfig getConfig() {
            String propWhite = getSystemProperty(PROPERTY_WHITELIST, DEFAULT_WHITELIST);
            String propBlack = getSystemProperty(PROPERTY_BLACKLIST, DEFAULT_BLACKLIST);
            ListsConfig current = listsConfigCache;
            if (current != null) {
                if (StringUtil.equals(current.whitelistValue, propWhite)
                        && StringUtil.equals(current.blacklistValue, propBlack)) {
                    return current;
                }
            }
            current = new ListsConfig(propWhite, propBlack);
            listsConfigCache = current;
            return current;
        }
    }
}
