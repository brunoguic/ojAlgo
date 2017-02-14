package org.ojalgo.array;

import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.random.Distribution;
import org.ojalgo.scalar.Scalar;

/**
 * To be used by implementations that delegate to a DenseArray
 *
 * @author apete
 */
final class DenseStrategy<N extends Number> {

    static long DEFAULT = 16_384L;
    static long MINIMAL = 16L;

    private long myChunk = DEFAULT;
    private final DenseArray.Factory<N> myDenseFactory;
    private long myInitial = MINIMAL;

    DenseStrategy(final DenseArray.Factory<N> denseFactory) {

        super();

        myDenseFactory = denseFactory;
    }

    DenseStrategy<N> capacity(final Distribution expected) {
        final long stdDev = (long) expected.getStandardDeviation();
        final long exp = (long) expected.getExpected();
        return this.chunk(stdDev).initial(exp + stdDev);
    }

    DenseStrategy<N> chunk(final long chunk) {
        final int power = PrimitiveMath.powerOf2Smaller(chunk);
        myChunk = Math.max(MINIMAL, 1L << power);
        return this;
    }

    long grow(final long current) {

        long required = current + 1L;

        long retVal = myChunk;

        if (required >= myChunk) {
            while (retVal < required) {
                required += myChunk;
            }
        } else {
            long maybe = retVal / 2L;
            while (maybe >= required) {
                retVal = maybe;
                maybe /= 2L;
            }
        }

        return retVal;
    }

    boolean isChunked(final long count) {
        return count >= myChunk;
    }

    DenseStrategy<N> initial(final long initial) {
        myInitial = Math.max(MINIMAL, initial);
        return this;
    }

    SegmentedArray<N> makeSegmented(final BasicArray<N> segment) {
        if (segment.count() == myChunk) {
            return myDenseFactory.wrapAsSegments(segment, this.makeChunk());
        } else {
            throw new IllegalStateException();
        }
    }

    DenseArray<N> make(final long size) {
        return myDenseFactory.make(size);
    }

    DenseArray<N> makeChunk() {
        return this.make(myChunk);
    }

    DenseArray<N> makeInitial() {
        return this.make(myInitial);
    }

    long[] makeInitialIndices() {
        return new long[(int) myInitial];
    }

    Scalar<N> zero() {
        return myDenseFactory.zero();
    }

}