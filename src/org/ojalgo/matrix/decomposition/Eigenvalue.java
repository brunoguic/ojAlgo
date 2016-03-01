/*
 * Copyright 1997-2015 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.matrix.decomposition;

import java.math.BigDecimal;

import org.ojalgo.access.Access2D;
import org.ojalgo.access.Structure2D;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.BasicArray;
import org.ojalgo.matrix.MatrixUtils;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.scalar.ComplexNumber;

/**
 * [A] = [V][D][V]<sup>-1</sup> ([A][V] = [V][D])
 * <ul>
 * <li>[A] = any square matrix.</li>
 * <li>[V] = contains the eigenvectors as columns.</li>
 * <li>[D] = a diagonal matrix with the eigenvalues on the diagonal (possibly in blocks).</li>
 * </ul>
 * <p>
 * [A] is normal if [A][A]<sup>H</sup> = [A]<sup>H</sup>[A], and [A] is normal if and only if there exists a
 * unitary matrix [Q] such that [A] = [Q][D][Q]<sup>H</sup>. Hermitian matrices are normal.
 * </p>
 * <p>
 * [V] and [D] can always be calculated in the sense that they will satisfy [A][V] = [V][D], but it is not
 * always possible to calculate [V]<sup>-1</sup>. (Check the rank and/or the condition number of [V] to
 * determine the validity of [V][D][V]<sup>-1</sup>.)
 * </p>
 *
 * @author apete
 */
public interface Eigenvalue<N extends Number>
        extends MatrixDecomposition<N>, MatrixDecomposition.Hermitian<N>, MatrixDecomposition.Determinant<N>, MatrixDecomposition.Values<N> {

    interface Factory<N extends Number> extends MatrixDecomposition.Factory<Eigenvalue<N>> {

        default Eigenvalue<N> make(final Structure2D template) {
            if (template instanceof Access2D) {
                return this.make(template, MatrixUtils.isHermitian((Access2D<?>) template));
            } else {
                return this.make(template, false);
            }
        }

        Eigenvalue<N> make(Structure2D template, boolean hermitian);

    }

    public static final Factory<BigDecimal> BIG = new Factory<BigDecimal>() {

        public Eigenvalue<BigDecimal> make(final Structure2D template, final boolean hermitian) {
            return hermitian ? new HermitianEvD.Big() : null;
        }

    };

    public static final Factory<ComplexNumber> COMPLEX = new Factory<ComplexNumber>() {

        public Eigenvalue<ComplexNumber> make(final Structure2D template, final boolean hermitian) {
            return hermitian ? new HermitianEvD.Complex() : null;
        }

    };

    public static final Factory<Double> PRIMITIVE = new Factory<Double>() {

        public Eigenvalue<Double> make(final Structure2D template) {
            if ((8192L < template.countColumns()) && (template.count() <= BasicArray.MAX_ARRAY_SIZE)) {
                return new DynamicEvD.Primitive();
            } else {
                return new RawEigenvalue.Dynamic();
            }
        }

        public Eigenvalue<Double> make(final Structure2D template, final boolean hermitian) {
            if ((8192L < template.countColumns()) && (template.count() <= BasicArray.MAX_ARRAY_SIZE)) {
                return hermitian ? new HermitianEvD.Primitive() : new GeneralEvD.Primitive();
            } else {
                return hermitian ? new RawEigenvalue.Symmetric() : new RawEigenvalue.General();
            }
        }

    };

    public static <N extends Number> Eigenvalue<N> make(final Access2D<N> typical) {
        return Eigenvalue.make(typical, MatrixUtils.isHermitian(typical));
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number> Eigenvalue<N> make(final Access2D<N> typical, final boolean hermitian) {

        final N tmpNumber = typical.get(0L, 0L);

        if (tmpNumber instanceof BigDecimal) {
            return (Eigenvalue<N>) BIG.make(typical, hermitian);
        } else if (tmpNumber instanceof ComplexNumber) {
            return (Eigenvalue<N>) COMPLEX.make(typical, hermitian);
        } else if (tmpNumber instanceof Double) {
            return (Eigenvalue<N>) PRIMITIVE.make(typical, hermitian);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @deprecated v40 Use {@link #BIG} instead
     */
    @Deprecated
    public static Eigenvalue<BigDecimal> makeBig() {
        return Eigenvalue.makeBig(true);
    }

    /**
     * @deprecated v40 Use {@link #BIG} instead
     */
    @Deprecated
    public static Eigenvalue<BigDecimal> makeBig(final boolean symmetric) {
        return BIG.make(TYPICAL, symmetric);
    }

    /**
     * @deprecated v40 Use {@link #COMPLEX} instead
     */
    @Deprecated
    public static Eigenvalue<ComplexNumber> makeComplex() {
        return Eigenvalue.makeComplex(true);
    }

    /**
     * @deprecated v40 Use {@link #COMPLEX} instead
     */
    @Deprecated
    public static Eigenvalue<ComplexNumber> makeComplex(final boolean hermitian) {
        return COMPLEX.make(TYPICAL, hermitian);
    }

    /**
     * @deprecated v40 Use {@link #PRIMITIVE}
     */
    @Deprecated
    public static Eigenvalue<Double> makePrimitive() {
        return PRIMITIVE.make();
    }

    /**
     * @deprecated v40 Use {@link #PRIMITIVE}
     */
    @Deprecated
    public static Eigenvalue<Double> makePrimitive(final boolean symmetric) {
        return PRIMITIVE.make(TYPICAL, symmetric);
    }

    /**
     * The only requirements on [D] are that it should contain the eigenvalues and that [A][V] = [V][D]. The
     * ordering of the eigenvalues is not specified.
     * <ul>
     * <li>If [A] is real and symmetric then [D] is (purely) diagonal with real eigenvalues.</li>
     * <li>If [A] is real but not symmetric then [D] is block-diagonal with real eigenvalues in 1-by-1 blocks
     * and complex eigenvalues in 2-by-2 blocks.</li>
     * <li>If [A] is complex then [D] is (purely) diagonal with complex eigenvalues.</li>
     * </ul>
     *
     * @return The (block) diagonal eigenvalue matrix.
     */
    MatrixStore<N> getD();

    /**
     * <p>
     * Even for real matrices the eigenvalues are potentially complex numbers. Typically they need to be
     * expressed as complex numbers when [A] is not symmetric.
     * </p>
     * <p>
     * The eigenvalues in this array should always be ordered in descending order - largest (modulus) first.
     * </p>
     *
     * @return The eigenvalues in an ordered array.
     */
    Array1D<ComplexNumber> getEigenvalues();

    /**
     * A matrix' trace is the sum of the diagonal elements. It is also the sum of the eigenvalues. This method
     * should return the sum of the eigenvalues.
     *
     * @return The matrix' trace
     */
    ComplexNumber getTrace();

    /**
     * The columns of [V] represent the eigenvectors of [A] in the sense that [A][V] = [V][D].
     *
     * @return The eigenvector matrix.
     */
    MatrixStore<N> getV();

    /**
     * If [A] is hermitian then [V][D][V]<sup>-1</sup> becomes [Q][D][Q]<sup>H</sup>...
     */
    boolean isHermitian();

    /**
     * The eigenvalues in D (and the eigenvectors in V) are not necessarily ordered. This is a property of the
     * algorithm/implementation, not the data.
     *
     * @return true if they are ordered
     */
    boolean isOrdered();

    default MatrixStore<N> reconstruct() {
        return MatrixUtils.reconstruct(this);
    }

}
