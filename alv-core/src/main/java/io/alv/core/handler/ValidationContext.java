package io.alv.core.handler;

import io.alv.core.handler.messages.objects.ConstraintViolation;

import java.util.List;

public class ValidationContext<M> {

  public final M message;
  public final long timestamp;
  private final List<ConstraintViolation> violations;
  public final ReadOnlyState state;

  public ValidationContext(
    final M message,
    final long timestamp,
    final List<ConstraintViolation> violations,
    final ReadOnlyState state
  ) {
    this.message = message;
    this.timestamp = timestamp;
    this.violations = violations;
    this.state = state;
  }

  public void unicast(ConstraintViolation constraintViolation) {
    violations.add(constraintViolation);
  }

}
