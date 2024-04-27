package io.alv.core.test.model;

import io.alv.core.Encoding;
import io.alv.core.Model;

@Model(Encoding.FURY)
public record Counter(
  int current
) {
}
