package com.thatmg393.bettertpa4fabric.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A disjoint union type that holds either a Left value or a Right value (or
 * both).
 * By convention, Left represents success and Right represents failure/error.
 * But this convention can be not followed.
 *
 * @param <L> the left type
 * @param <R> the right type
 */
public abstract class Either<L, R> {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Wraps a left value. */
    public static <L, R> Either<L, R> left(L left) {
        return new Left<>(left);
    }

    /** Wraps a right value. */
    public static <L, R> Either<L, R> right(R right) {
        return new Right<>(right);
    }

    /** Wraps both left and right value. */
    public static <L, R> Either<L, R> both(L left, R right) {
        return new Both<>(left, right);
    }

    // -------------------------------------------------------------------------
    // Abstract accessors
    // -------------------------------------------------------------------------

    public abstract Optional<L> getLeft();
    public abstract Optional<R> getRight();

    // -------------------------------------------------------------------------
    // Default predicates
    // -------------------------------------------------------------------------

    public boolean hasLeft() {
        return getLeft().isPresent();
    }

    public boolean hasRight() {
        return getRight().isPresent();
    }

    /** True only when a left value is present AND no right value is present. */
    public boolean isLeft() {
        return hasLeft() && !hasRight();
    }

    /** True only when a right value is present AND no left value is present. */
    public boolean isRight() {
        return !hasLeft() && hasRight();
    }

    /** True when both a left and a right value are present. */
    public boolean isBoth() {
        return hasLeft() && hasRight();
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Maps the right value if present, leaving the left value unchanged.
     * Conventional functor map — treats Right as the "happy path".
     */
    public <R2> Either<L, R2> mapRight(Function<? super R, ? extends R2> f) {
        if (hasRight()) {
            R2 newRight = f.apply(getRight().get());
            return hasLeft() ? new Both<>(getLeft().get(), newRight) : new Right<>(newRight);
        }
        // No right value — carry left through with the new R2 phantom type
        @SuppressWarnings("unchecked")
        Either<L, R2> cast = (Either<L, R2>) this;
        return cast;
    }

    /** Maps the left value if present, leaving the right value unchanged. */
    public <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> f) {
        if (hasLeft()) {
            L2 newLeft = f.apply(getLeft().get());
            return hasRight() ? new Both<>(newLeft, getRight().get()) : new Left<>(newLeft);
        }
        @SuppressWarnings("unchecked")
        Either<L2, R> cast = (Either<L2, R>) this;
        return cast;
    }

    /**
     * Runs the given consumer with the left value if present; does nothing otherwise.
     * Returns {@code this} for chaining.
     */
    public Either<L, R> ifLeft(Consumer<? super L> consumer) {
        getLeft().ifPresent(consumer);
        return this;
    }

    /**
     * Runs the given consumer with the right value if present; does nothing otherwise.
     * Returns {@code this} for chaining.
     */
    public Either<L, R> ifRight(Consumer<? super R> consumer) {
        getRight().ifPresent(consumer);
        return this;
    }

    /**
     * Folds both sides into a single value.
     *
     * @param onLeft  called with the left value when present
     * @param onRight called with the right value when present
     * @param onBoth  called when both values are present
     * @param onNone  called when neither value is present
     */
    public <T> T fold(
        Function<? super L, ? extends T> onLeft,
        Function<? super R, ? extends T> onRight,
        BiFunction<? super L, ? super R, ? extends T> onBoth,
        Supplier<? extends T> onNone
    ) {
        if (isBoth()) return onBoth.apply(getLeft().get(), getRight().get());
        if (isLeft()) return onLeft.apply(getLeft().get());
        if (isRight()) return onRight.apply(getRight().get());
        return onNone.get();
    }

    // -------------------------------------------------------------------------
    // Concrete subclasses
    // -------------------------------------------------------------------------

    /** Holds only a left value. */
    private static final class Left<L, R> extends Either<L, R> {
        private final L value;

        Left(L value) {
            this.value = value;
        }

        @Override
        public Optional<L> getLeft() {
            return Optional.ofNullable(value);
        }

        @Override
        public Optional<R> getRight() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Either.Left(" + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Left)) return false;

            Left<?, ?> that = (Left<?, ?>) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Left", value);
        }
    }

    /** Holds only a right value. */
    private static final class Right<L, R> extends Either<L, R> {
        private final R value;

        Right(R value) {
            this.value = value;
        }

        @Override
        public Optional<L> getLeft() {
            return Optional.empty();
        }

        @Override
        public Optional<R> getRight() {
            return Optional.ofNullable(value);
        }

        @Override
        public String toString() {
            return "Either.Right(" + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Right)) return false;

            Right<?, ?> that = (Right<?, ?>) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Right", value);
        }
    }

    /** Holds both a left and a right value simultaneously. */
    private static final class Both<L, R> extends Either<L, R> {
        private final L left;
        private final R right;

        Both(L left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Optional<L> getLeft() {
            return Optional.ofNullable(left);
        }

        @Override
        public Optional<R> getRight() {
            return Optional.ofNullable(right);
        }

        @Override
        public String toString() {
            return "Either.Both(left=" + left + ", right=" + right + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Both)) return false;

            Both<?, ?> that = (Both<?, ?>) o;
            return Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Both", left, right);
        }
    }
}
