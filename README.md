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
a `pComplex` Polar Complex number. The magntiude is retievable at the `:r`
key, and the phase at the `:t` key.

The frequency bins returned by the algorithm will be ordered by frequencies:
positive first, and then negative; the first N/2 bins are the positive bins,
the next are negative bins.

The divisions of the frequency bins are window-dependent. The frequencies
available are between 0 and the Nyquist frequency (1/2 of the sampling frequency).
The bins will be as wide as (Nyquist / window-size).

## Algorithm
The TSWiFT is based on the work by L Richardson of Carnegie Mellon University,
who developed a k-dimension version. This implementation is only a 1D-version.

The TSWiFT algorithm is based on a traditional FFT, when executed in rapid
succession. This produces a time-frequency distribution, allowing the user
to observe a changing frequency over time. The difficulty is in the runtime
requirements of the FFT are overly demanding, and would result in excessive
calculations.

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
intermediary sample-points.

## TODO
* Windowing - Frequency-domain convolution windowing, to reduce frequency smearing around the edges
* Instance-based - allow the use to provide the structures on a per-instance basis
* Pure - Remove side effects

## License
MIT Public License (see LICENSE)

