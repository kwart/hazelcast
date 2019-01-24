/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.util;

import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.Provider;
import java.security.Security;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class MD5UtilTest extends HazelcastTestSupport {
    @Test
    public void testConstructorIsPrivate() {
        assertUtilityConstructor(MD5Util.class);
    }

    @Test
    public void testToMD5String() {
        String result = MD5Util.toMD5String("hazelcast");
        assertEquals("58c304267fc3edac49d35c395f0fbc0f", result);
    }

    @Test
    public void testToMD5StringWithProvider() {
        for (Provider p: Security.getProviders()) {
            String name = p.getName();
            String result = MD5Util.toMD5String("hazelcast", name);
            if (p.getProperty("MessageDigest.MD5")!=null) {
                assertEquals("Provider " + name + " computes wrong hash", "58c304267fc3edac49d35c395f0fbc0f", result);
            } else {
                assertNull("Provider " + name + " is expected to fail to compute hash",result);
            }
        }
        assertNull(MD5Util.toMD5String("hazelcast", "dummy-provider-name"));
    }

    @Test
    public void testToMD5String_whenNullString() {
        String str = null;
        assertNull(MD5Util.toMD5String(str));
    }

}
