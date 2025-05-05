package com.daniking.backtools.utils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class that acts as a container for a value of one of two types. An Either
 * can be either be a "Left", containing a LHS value or a "Right" containing a RHS value,
 * but it cannot be "neither" nor "both".
 *
 * <p>An Either can be used to express a success or failure case. By convention,
 * a Right contains the result of a successful computation,
 * and a Left contains some kind of failure object.
 *
 * @param <L> the type of the LHS value
 * @param <R> the type of the RHS value
 */
public abstract class Either<L, R> {

    protected Either() {
    }

    /**
     * Factory method for creating an Either instance from a left-supplier and a
     * right-supplier; if both are provided, the right one is preferred.
     *
     * @return either a Left or a Right instance, depending on which values are available.
     */
    public static <L, R> Either<L, R> either(Supplier<L> leftSupplier, Supplier<R> rightSupplier) {
        R rightValue = rightSupplier.get();
        if (rightValue != null) {
            return Either.right(rightValue);
        } else {
            return Either.left(leftSupplier.get());
        }
    }


    public static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }


    public static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    /**
     * If this is a Right, returns a Right containing the result of applying
     * the mapper function to the RHS value.
     * Otherwise returns a Left containing the LHS value.
     *
     * @param mapper the function to apply to the RHS value, if this is a Right
     * @param <R2>   the new RHS type
     * @return an equivalent instance if this is a Left, otherwise a Right containing
     * the result of applying {@code mapper} to the RHS value
     */
    public abstract <R2> Either<L, R2> mapRight(Function<? super R, ? extends R2> mapper);

    /**
     * If this is a Right, returns the result of applying the mapper function to the RHS value.
     * Otherwise returns a Left containing the LHS value.
     *
     * @param mapper a mapper function
     * @param <R2>   the new RHS type
     * @return an equivalent instance if this is a Left, otherwise the result of
     * applying {@code mapper} to the RHS value
     */
    public abstract <R2> Either<L, R2> flatMapRight(Function<? super R, ? extends Either<? extends L, ? extends R2>> mapper);

    /**
     * If this is a Left, returns a Left containing the result of applying the mapper function to the LHS value.
     * Otherwise returns a Right containing the RHS value.
     *
     * @param mapper the function to apply to the LHS value
     * @param <L2>   the new LHS type
     * @return an equivalent instance if this is a Right, otherwise a Left containing
     * the result of applying {@code mapper} to the LHS value
     */
    public abstract <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> mapper);

    /**
     * If this is a Left, returns the result of applying the mapper function to the LHS value.
     * Otherwise returns a Right containing the RHS value.
     *
     * @param mapper a mapper function
     * @param <L2>   the new LHS type
     * @return an equivalent instance if this is a Right, otherwise the result of
     * applying {@code mapper} to the LHS value
     */
    public abstract <L2> Either<L2, R> flatMapLeft(Function<? super L, ? extends Either<? extends L2, ? extends R>> mapper);

    /**
     * If this is a Left, returns the result of applying the {@code leftMapper} to the LHS value.
     * Otherwise returns the result of applying the {@code rightMapper} to the RHS value.
     *
     * @param leftMapper  the function to apply if this is a Left
     * @param rightMapper the function to apply if this is a Right
     * @param <U>         the result type of both {@code leftMapper} and {@code rightMapper}
     * @return the result of applying either {@code leftMapper} or {@code rightMapper}
     */
    public abstract <U> U fold(
        Function<? super L, ? extends U> leftMapper,
        Function<? super R, ? extends U> rightMapper);


    /**
     * If this is a Left, performs the {@code leftConsumer} with the LHS value.
     * Otherwise, performs the {@code rightConsumer} with the RHS value.
     *
     * @param leftConsumer  action to run if this is a Left
     * @param rightConsumer action to run if this is a Right
     */
    public abstract void run(
        Consumer<? super L> leftConsumer,
        Consumer<? super R> rightConsumer);

    /**
     * If this is a Left, performs the {@code leftConsumer} with the LHS value.
     * Otherwise, performs the {@code rightConsumer} with the RHS value.
     *
     * @param leftConsumer  action to run if this is a Left
     * @param rightConsumer action to run if this is a Right
     */
    public abstract <E extends Exception> void runExceptionally(
        ThrowingConsumer<? super L, E> leftConsumer,
        ThrowingConsumer<? super R, E> rightConsumer) throws E;

    /**
     * If this is a Right, returns the RHS value.
     * Otherwise throws an exception produced by the exception supplying function.
     *
     * @param exceptionSupplier exception supplying function
     * @param <X>               type of the exception
     * @return the RHS value, if this is a Right
     * @throws X the result of applying {@code exceptionSupplier} to the LHS value, if this is a Left
     */
    public abstract <X extends Throwable> R orElseThrow(Function<? super L, ? extends X> exceptionSupplier) throws X;

    /**
     * Returns {@code true} if this is a Left, otherwise {@code false}.
     *
     * @return {@code true} if this is a Left, otherwise {@code false}
     */
    public abstract boolean isLeft();

    /**
     * Returns {@code true} if this is a Right, otherwise {@code false}.
     *
     * @return {@code true} if this is a Right, otherwise {@code false}
     */
    public final boolean isRight() {
        return !isLeft();
    }

    /**
     * Forcibly gets the left-wrapped value if this is a Left, or throws a
     * {@link java.util.NoSuchElementException} if this is a Right.
     *
     * @return the contents of the Left if this is a Left.
     * @throws java.util.NoSuchElementException if this is a Right.
     */
    public abstract L getLeft();

    /**
     * Returns the left-side value if this is a Left; otherwise throws the
     * exception which is a result of transforming Right by `rightToException`.
     *
     * @param rightToException a Function that gets Right and returns a Throwable that will be thrown if this is a Right.
     * @return the left-side value if this is a Left.
     * @throws T if this is a Right.
     */
    public abstract <T extends Throwable> L getLeftOrElseThrow(Function<R, T> rightToException) throws T;

    /**
     * @return the left-side value if this is a Left otherwise, the supplied other value.
     */
    public L getLeftOrElse(L other) {
        return fold(
            Function.identity(),
            right -> other
        );
    }

    /**
     * Forcibly gets the right-wrapped value if this is a Right, or throws a
     * {@link java.util.NoSuchElementException} if this is a Left.
     *
     * @return the contents of the Right if this is a Right.
     * @throws java.util.NoSuchElementException if this is a Left.
     */
    public abstract R getRight();

    /**
     * Returns the right-side value if this is a Right; otherwise throws the
     * exception which is a result of transforming Left by `leftToException`.
     *
     * @param leftToException a Function that gets Left and returns a Throwable that will be thrown if this is a Left.
     * @return the right-side value if this is a Right.
     * @throws T if this is a Left.
     */
    public abstract <T extends Throwable> R getRightOrElseThrow(Function<L, T> leftToException) throws T;

    /**
     * @return the right-side value if this is a Right otherwise, the supplied other value.
     */
    public R getRightOrElse(R other) {
        return fold(
            left -> other,
            Function.identity()
        );
    }

    /**
     * Returns a string representation of this {@code Either}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     *
     * @return the string representation of this instance
     */
    @Override
    public abstract String toString();

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }
}
