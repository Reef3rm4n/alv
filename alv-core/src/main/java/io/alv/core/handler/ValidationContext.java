package io.alv.core.handler;

import io.alv.core.handler.messages.objects.ConstraintViolation;

import java.util.List;

public final class ValidationContext<M> {

  public final M message;
  public final long timestamp;
  private final List<ConstraintViolation> violations;

  ValidationContext(
    final M message,
    final long timestamp,
    final List<ConstraintViolation> violations
  ) {
    this.message = message;
    this.timestamp = timestamp;
    this.violations = violations;
  }

  public void unicast(ConstraintViolation constraintViolation) {
    violations.add(constraintViolation);
  }

}
