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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Utility class for hashing with MD5
 */
public final class MD5Util {

    private MD5Util() {
    }

    /**
     * Converts given string to MD5 hash. Returns MD5 hash from UTF-8 String encoding as a hex-string.
     *
     * @param str string to be hashed with MD5
     */
    public static String toMD5String(String str) {
        return toMD5String(str, null);
    }

    /**
     * Converts given string to MD5 hash. Returns MD5 hash from UTF-8 String encoding as a hex-string. If a not-{@code null}
     * provider argument is given, but the provider name doesn't exist, then {@code null} is returned. The {@code null} is 
     * also returned when MD5 algorithm is not available in the given provider or registered providers.
     *
     * @param str string to be hashed with MD5
     * @param provider Security Provider name to be used for the {@link MessageDigest}
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String toMD5String(String str, String provider) {
        try {
            MessageDigest md = provider==null
                    ? MessageDigest.getInstance("MD5")
                    : MessageDigest.getInstance("MD5", provider);
            if (md == null || str == null) {
                return null;
            }
            byte[] byteData = md.digest(str.getBytes(Charset.forName("UTF-8")));

            StringBuilder sb = new StringBuilder();
            for (byte aByteData : byteData) {
                sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        } catch (NoSuchProviderException e) {
            return null;
        }
    }
}
