package com.agora.gpt;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class Base64Utils {

   /**
    * 将字符串编码为Base64格式
    *
    * @param text 要编码的字符串
    * @return 编码后的字符串
    */
   public static String encode(String text) {
      byte[] data = text.getBytes(StandardCharsets.UTF_8);
      return Base64.encodeToString(data, Base64.DEFAULT);
   }

   /**
    * 将Base64格式的字符串解码为普通字符串
    *
    * @param base64String Base64格式的字符串
    * @return 解码后的字符串
    */
   public static String decode(String base64String) {
      byte[] data = Base64.decode(base64String, Base64.DEFAULT);
      return new String(data, StandardCharsets.UTF_8);
   }
}
