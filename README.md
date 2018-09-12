# TSWiFT - The Tree Sliding Window Fourier Transform

A fast method of computing a time-frequency spectrum of data, in Clojure!
Packages a small Complex Number utility  as a complement.

This was done purely as a "let's learn Clojure" project. I normally would
have implemented this in C with static structures, but wanted the challenge
of working in a functional language, and to learn a new language in a totally
different idiom.

## Usage
Initialize the module using `(init options)`, and then provide new samples
to the TSWiFT using `(submit-sample sample)`. This will return either `nil`,
if the module has not yet "warmed up" (Takes 1 full window to do so), or
a `cComplex` Cartesian Complex number. The magntidue can be computed using
the `cmagnitude` method, or by converting the value to a Polar value,
using the `cconvert` method.

The frequency bins returned by the algorithm will be ordered by frequencies:
positive first, and then negative; the first N/2 bins are the positive bins,
the next are negative bins.

The divisions of the frequency bins are window-dependent. The frequencies
available are between 0 and the Nyquist frequency (1/2 of the sampling frequency).
The bins will be as wide as (Nyquist / window-size).

The values in the bins that are returned will be scaled, to a gain of `window-size`.
this can be taken off of the resultant bins by the user for whichever bins are desired
to be scaled. The scaling by the window-size is due to the FFT.

The windowing can be applied by using the `apply-window` method. It should
be noted that the windowing does have a performance penalty, incurred
mainly by the required reordering of the bins. To use this method, the windowing
type (either `:hann`, `:hamming`, or `:rectangular` for their respective windows),
the window-size, and the resultant frequency bins must be provided.

## Algorithm
The TSWiFT is based on the work by L Richardson of Carnegie Mellon University,
who developed a k-dimension version. This implementation is only a 1D-version.

The TSWiFT algorithm is based on a traditional FFT, when executed in rapid
succession. This produces a time-frequency distribution, allowing the user
to observe a changing frequency over time. The difficulty is in the runtime
requirements of the FFT are overly demanding, and would result in excessive
calculations.

### Node Calculations
The formula for the calculation of a single node in a Coolie-Tukey FFT
is as follows:

G[N, l, t, i] = G[n, l-1, t-(N/2^l), i%l] + e^(jwi/(2^l))G[N, l-1, t, i%l]

Where:

* G[] is the  value of a given term,
* N is the Window Size, a power-of-2,
* l is the node-level, 1->2^l,
* t is the tree index, 0 -> 2^l,
* i is the node index within the tree, 0->2^l
* e is Euler's number,
* j is the imaginary constant,
* w is the frequency, omega, in radians

The formula states that any given node (l,t,i), is dependent on the node one level above it
in the tree, as well as the node N/(2^l) trees behind it.

This formula can be applies recursively to determine the value of which calculations
a given node (i), in a given level (l), of a given tree (w). To simplify space demands,
some of these indices can be combined together (tree and node index, for example).

The essence of the TSWiFT algorithm  comes from the savings that can be realized from
repeated calculations, where prior nodes can be reused to provide information about
future nodes. Additionally, if an FFT is not required at every sample point,
even further savings can be realized, by omitting certain calculations of the
intermediary sample-points. These latter savings have yet to be implemented, however.

### Windowing
The windowing is applied at the end of the TSWiFT algorithm, to the output bins,
where each bin is windowed in the Frequency Domain. A Hann window was chosen for
simplicity, but another could be implemented in its stead.

The windowing implemented is to allow for 3-coefficient raised cosine windows to be
applied at will, per sample. This model allows for rectangular windowing, Hann
windowing, or Hamming windowing. The windowing could be extended to include 5-term
windows, such as Blackman windows, but this is not yet implemented. Windowing can
incur a performance penaly, owing to the need to compute extra multiplies and additions
per sample.

The formula used for the Frequency-domain Hann window is as follows:

X'[m] = (aX[m] - b(X[m-1] + X[m+1]))

where `a` and `b` are the coefficients of the desired window.

## Testing
Tests are implemented for both the Complex Number library included (cmplx.core), as well as
for the main package, (tswift.core). The tests can all be run using `lein test`.

The tests for the TSWiFT package produce output of the final frequency bins. Additionally,
the tests do not have any assertions associated with them. This is due to the difficulty in
both floating point rounding, as well as in judging the validity of a given output.
This is especially true when frequency smearing is to be taken into account, where, due to
a combination of windowing and signal aliasing, it can be difficult to predict the desired
result.

In order to better validate this package, then, it should be compared to a standard FFT.
This has yet to be done.

## TODO
* Instance-based - allow the use to provide the structures on a per-instance basis
* Pure - Remove side effects
* Intermediate skipping
* Allow for 5-term windows
* Validate against a standard, validated FFT library.

## License
MIT Public License (see LICENSE)

