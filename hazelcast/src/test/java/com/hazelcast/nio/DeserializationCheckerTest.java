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

import static com.hazelcast.test.OverridePropertyRule.clear;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.OverridePropertyRule;
import com.hazelcast.test.annotation.QuickTest;

/**
 * Unit tests for {@link DeserializationChecker}.
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class DeserializationCheckerTest {

    @Rule
    public final OverridePropertyRule ruleSysPropBlacklist = clear(DeserializationChecker.PROPERTY_BLACKLIST);
    @Rule
    public final OverridePropertyRule ruleSysPropWhitelist = clear(DeserializationChecker.PROPERTY_WHITELIST);

    /**
     * <pre>
     * Given: Neither whitelist nor blacklist is configured.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called.
     * Then: no exception is thrown
     * </pre>
     */
    @Test
    public void testNoList() throws ClassNotFoundException {
        DeserializationChecker.checkClassNameForResolution("java.lang.Object");
    }

    /**
     * <pre>
     * Given: Neither whitelist nor blacklist is configured.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a class name which is included in the default blacklist.
     * Then: {@link ClassNotFoundException} is thrown
     * </pre>
     */
    @Test(expected=ClassNotFoundException.class)
    public void testClassDefaultBlacklisted() throws ClassNotFoundException {
        DeserializationChecker.checkClassNameForResolution("bsh.XThis");
    }

    /**
     * <pre>
     * Given: Neither whitelist nor blacklist is configured.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a class in package which is included in the default blacklist.
     * Then: {@link ClassNotFoundException} is thrown
     * </pre>
     */
    @Test(expected=ClassNotFoundException.class)
    public void testPackageDefaultBlacklisted() throws ClassNotFoundException {
        DeserializationChecker.checkClassNameForResolution("org.apache.commons.collections.functors.Test");
    }

    /**
     * <pre>
     * Given: Blacklist is set to empty string (i.e. blacklisting disabled).
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a class included in the default blacklist.
     * Then: no exception is thrown
     * </pre>
     */
    @Test
    public void testEmptyBlacklist() throws ClassNotFoundException {
        ruleSysPropBlacklist.setOrClear("");
        DeserializationChecker.checkClassNameForResolution("bsh.XThis");
    }

    /**
     * <pre>
     * Given: Whitelist is set.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a whitelisted class.
     * Then: no exception is thrown
     * </pre>
     */
    @Test
    public void testClassInWhitelist() throws ClassNotFoundException {
        ruleSysPropWhitelist.setOrClear("java.lang.Test1,java.lang.Test2,java.lang.Test3");
        DeserializationChecker.checkClassNameForResolution("java.lang.Test2");
    }

    /**
     * <pre>
     * Given: Whitelist is set.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a class which has whitelisted package.
     * Then: no exception is thrown
     * </pre>
     */
    @Test
    public void testPackageInWhitelist() throws ClassNotFoundException {
        ruleSysPropWhitelist.setOrClear("java.lang.Test1,com.whitelisted");
        DeserializationChecker.checkClassNameForResolution("com.whitelisted.Test2");
    }

    /**
     * <pre>
     * Given: Whitelist is set.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a not whitelisted class.
     * Then: {@link ClassNotFoundException} is thrown
     * </pre>
     */
    @Test(expected=ClassNotFoundException.class)
    public void testClassNotInWhitelist() throws ClassNotFoundException {
        ruleSysPropWhitelist.setOrClear("java.lang.Test1,java.lang.Test2,java.lang.Test3");
        DeserializationChecker.checkClassNameForResolution("java.lang.Test4");
    }

    /**
     * <pre>
     * Given: Blacklist and Whitelist are set.
     * When: {@link DeserializationChecker#checkClassNameForResolution(String)} is called for a class which is whitelisted and blacklisted together.
     * Then: {@link ClassNotFoundException} is thrown
     * </pre>
     */
    @Test(expected=ClassNotFoundException.class)
    public void testWhitelistedAndBlacklisted() throws ClassNotFoundException {
        ruleSysPropWhitelist.setOrClear("java.lang.Test1,java.lang.Test2,java.lang.Test3");
        ruleSysPropBlacklist.setOrClear("java.lang.Test3,java.lang.Test2,java.lang.Test1");
        DeserializationChecker.checkClassNameForResolution("java.lang.Test1");
    }
}