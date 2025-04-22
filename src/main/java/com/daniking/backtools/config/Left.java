package com.daniking.backtools.config;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Internal implementation of a Left-Either.
 *
 * @param <L> the type of the LHS value
 * @param <R> the type of the RHS value
 */
final class Left<L, R> extends Either<L, R> {

    private final L value;

    Left(L value) {
        this.value = value;
    }

    @Override
    public L getLeft() {
        return value;
    }

    @Override
    public <T extends Throwable> L getLeftOrElseThrow(Function<R, T> rightToException) throws T {
        return value;
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public R getRight() {
        throw new NoSuchElementException();
    }

    @Override
    public <T extends Throwable> R getRightOrElseThrow(Function<L, T> leftToException) throws T {
        throw leftToException.apply(value);
    }

    @Override
    public <R2> Either<L, R2> mapRight(Function<? super R, ? extends R2> mapper) {
        @SuppressWarnings("unchecked")
        Either<L, R2> result = (Either<L, R2>) this;
        return result;
    }

    @Override
    public <R2> Either<L, R2> flatMapRight(Function<? super R, ? extends Either<? extends L, ? extends R2>> mapper) {
        @SuppressWarnings("unchecked")
        Either<L, R2> result = (Either<L, R2>) this;
        return result;
    }

    @Override
    public <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> mapper) {
        return new Left<>(mapper.apply(value));
    }

    @Override
    public <L2> Either<L2, R> flatMapLeft(Function<? super L, ? extends Either<? extends L2, ? extends R>> mapper) {
        @SuppressWarnings("unchecked")
        Either<L2, R> result = (Either<L2, R>) mapper.apply(value);
        return result;
    }

    @Override
    public <U> U fold(
        Function<? super L, ? extends U> leftMapper,
        Function<? super R, ? extends U> rightMapper) {
        return leftMapper.apply(value);
    }

    @Override
    public void run(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
        leftConsumer.accept(value);
    }

    @Override
    public <E extends Exception> void runExceptionally(ThrowingConsumer<? super L, E> leftConsumer,
                                                       ThrowingConsumer<? super R, E> rightConsumer) throws E {
        leftConsumer.accept(value);
    }

    @Override
    public <X extends Throwable> R orElseThrow(Function<? super L, ? extends X> exceptionSupplier) throws X {
        throw exceptionSupplier.apply(value);
    }

    @Override
    public String toString() {
        return String.format("Left[%s]", value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Left)) {
            return false;
        }

        Left<?, ?> other = (Left<?, ?>) obj;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return 31 * value.hashCode();
    }
}