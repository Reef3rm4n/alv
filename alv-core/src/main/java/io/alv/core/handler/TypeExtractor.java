package io.alv.core.handler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

public class TypeExtractor {

  private TypeExtractor() {
  }
  public static <T> Class<?> getType(T object) {
    return getType(object, 0);
  }

  public static <T> Class<?> getType(T object, int index) {
    Optional<Type> genericInterfaces = Arrays.stream(object.getClass().getGenericInterfaces()).findFirst();
    if (genericInterfaces.isPresent()) {
      return getClass(genericInterfaces.get(), index);
    } else {
      return getClass(object.getClass().getGenericSuperclass(), index);
    }
  }

  private static Class<?> getClass(Type genericInterfaces, int index) {
    if (genericInterfaces instanceof ParameterizedType parameterizedType) {
      Type[] genericTypes = parameterizedType.getActualTypeArguments();
      try {
        return Class.forName(genericTypes[index].getTypeName());
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Unable to get generic types -> ", e);
      }
    } else {
      throw new IllegalArgumentException("Invalid generic interface -> " + genericInterfaces.getClass());
    }
  }


  public static String camelToSnake(String str) {
    // Regular Expression
    String regex = "([a-z])([A-Z]+)";

    // Replacement string
    String replacement = "$1_$2";

    // Replace the given regex
    // with replacement string
    // and convert it to lower case.
    str = str
      .replaceAll(
        regex, replacement)
      .toLowerCase();

    // return string
    return str;
  }
}
