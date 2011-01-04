/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.merge;

import dalvik.system.PathClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import junit.framework.TestCase;

/**
 * Test that DexMerge works by merging dex files, and then loading them into
 * the current VM.
 */
public final class DexMergeTest extends TestCase {

    public void testFillArrayData() throws Exception {
        ClassLoader loader = mergeAndLoad(
                "/testdata/Basic.dex",
                "/testdata/FillArrayData.dex");

        Class<?> basic = loader.loadClass("testdata.Basic");
        assertEquals(1, basic.getDeclaredMethods().length);

        Class<?> fillArrayData = loader.loadClass("testdata.FillArrayData");
        assertTrue(Arrays.equals(
                new byte[] { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, -112, -23, 121 },
                (byte[]) fillArrayData.getMethod("newByteArray").invoke(null)));
        assertTrue(Arrays.equals(
                new char[] { 0xFFFF, 0x4321, 0xABCD, 0, 'a', 'b', 'c' },
                (char[]) fillArrayData.getMethod("newCharArray").invoke(null)));
        assertTrue(Arrays.equals(
                new long[] { 4660046610375530309L, 7540113804746346429L, -6246583658587674878L },
                (long[]) fillArrayData.getMethod("newLongArray").invoke(null)));
    }

    public void testTryCatchFinally() throws Exception {
        ClassLoader loader = mergeAndLoad(
                "/testdata/Basic.dex",
                "/testdata/TryCatchFinally.dex");

        Class<?> basic = loader.loadClass("testdata.Basic");
        assertEquals(1, basic.getDeclaredMethods().length);

        Class<?> tryCatchFinally = loader.loadClass("testdata.TryCatchFinally");
        tryCatchFinally.getDeclaredMethod("method").invoke(null);
    }

    public void testStaticValues() throws Exception {
        ClassLoader loader = mergeAndLoad(
                "/testdata/Basic.dex",
                "/testdata/StaticValues.dex");

        Class<?> basic = loader.loadClass("testdata.Basic");
        assertEquals(1, basic.getDeclaredMethods().length);

        Class<?> staticValues = loader.loadClass("testdata.StaticValues");
        assertEquals((byte) 1, staticValues.getField("a").get(null));
        assertEquals((short) 2, staticValues.getField("b").get(null));
        assertEquals('C', staticValues.getField("c").get(null));
        assertEquals(0xabcd1234, staticValues.getField("d").get(null));
        assertEquals(4660046610375530309L,staticValues.getField("e").get(null));
        assertEquals(0.5f, staticValues.getField("f").get(null));
        assertEquals(-0.25, staticValues.getField("g").get(null));
        assertEquals("this is a String", staticValues.getField("h").get(null));
        assertEquals(String.class, staticValues.getField("i").get(null));
        assertEquals("[0, 1]", Arrays.toString((int[]) staticValues.getField("j").get(null)));
        assertEquals(null, staticValues.getField("k").get(null));
        assertEquals(true, staticValues.getField("l").get(null));
        assertEquals(false, staticValues.getField("m").get(null));
    }

    public ClassLoader mergeAndLoad(String dexAResource, String dexBResource) throws IOException {
        File dexA = resourceToFile(dexAResource);
        File dexB = resourceToFile(dexBResource);
        File mergedDex = File.createTempFile("DexMergeTest", ".classes.dex");
        new DexMerger(mergedDex, dexA, dexB).merge();
        File mergedJar = dexToJar(mergedDex);
        return new PathClassLoader(mergedJar.getPath(), getClass().getClassLoader());
    }

    private File resourceToFile(String resource) throws IOException {
        File result = File.createTempFile("DexMergeTest", ".resource");
        result.deleteOnExit();
        FileOutputStream out = new FileOutputStream(result);
        InputStream in = getClass().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("No such resource: " + resource);
        }
        copy(in, out);
        out.close();
        return result;
    }

    private File dexToJar(File dex) throws IOException {
        File result = File.createTempFile("DexMergeTest", ".jar");
        result.deleteOnExit();
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(result));
        jarOut.putNextEntry(new JarEntry("classes.dex"));
        copy(new FileInputStream(dex), jarOut);
        jarOut.closeEntry();
        jarOut.close();
        return result;
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        in.close();
    }
}
