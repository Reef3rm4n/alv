package io.alv.core.cluster.storage;

public class ByteConverter {

  public static long convertToBytes(String input) {
    String trimmedInput = input.trim().toLowerCase();
    long multiplier;

    if (trimmedInput.endsWith("gb") || trimmedInput.endsWith("g")) {
      multiplier = 1073741824L; // 1 GB in bytes
      trimmedInput = trimmedInput.substring(0, trimmedInput.length() - 2);
    } else if (trimmedInput.endsWith("mb") || trimmedInput.endsWith("m")) {
      multiplier = 1048576L; // 1 MB in bytes
      trimmedInput = trimmedInput.substring(0, trimmedInput.length() - 2);
    } else if (trimmedInput.endsWith("kb") || trimmedInput.endsWith("k")) {
      multiplier = 1024L; // 1 KB in bytes
      trimmedInput = trimmedInput.substring(0, trimmedInput.length() - 2);
    } else {
      throw new IllegalArgumentException("Invalid size unit. Expected GB, MB, or KB.");
    }

    try {
      long value = Long.parseLong(trimmedInput.trim());
      return value * multiplier;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format in input string.", e);
    }
  }
}
