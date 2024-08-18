/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.util;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Phong
 */
public class GZipUtils {

    public static byte[] decompress(byte[] contentBytes) {
        try {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ByteArrayInputStream bais = new ByteArrayInputStream(contentBytes); GZIPInputStream gzis = new GZIPInputStream(bais)) {
                IOUtils.copy(gzis, out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            return new byte[1];
        }
    }

    public static byte[] compress(byte[] content) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(content);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }
}
