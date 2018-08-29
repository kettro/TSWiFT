# TSWiFT - The Tree Sliding Window Fourier Transform

An fast method of computing a time-frequency spectrum of data, in Clojure!
Packages a small Complex Number utility  as a complement.


## Algorithm
The TSWiFT is based on the work by L Richardson of Carnegi Mellon University,
who developed a k-dimension version. This implementation is only a 1D-version.

The TSWiFT algorithm is based on a traditional FFT, when executed in rapid
succession. This produces a time-frequency distribution, allowing the user
to observe a changing frequency over time. The difficulty is in the runtime
requirements of the FFT are overly demanding, and would result in excessive
calculations.

The formula for the calculation of a single node in a Coolie-Tukey FFT
is as follows:

G[N, l, t, i] = G[n, l-1, t-(N/2^l), i%l] + e^(jwi/2^l)G[N, l-1, t, i%l]

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

## Usage

## License
MIT Public License (see LICENSE)

