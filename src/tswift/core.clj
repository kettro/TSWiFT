(ns tswift.core
  (:require [cmplx.core :refer :all]))

;; TSWiFT

;; Records
(defrecord tswift-options [window-size])
(defrecord window-coeff [a b])

;; Global state
;; TODO: store in a user-provided instance
(def ^:private _module-options (atom (->tswift-options 8)))
(def ^:private _module-sample-count (atom 0))
(def ^:private _module-sample-history (atom []))
(def ^:private _module-twiddle-table (atom []))
(def ^:private _module-max-level (atom 1))

;; Function declarations
(declare log2)
(declare generate-twiddle-table)
(declare get-node)
(declare compute-node)
(declare compute-level)
(declare compute-tree)
(declare handle-initial-sample)
(declare get-windowing-coeffs)
(declare init)
(declare submit-sample)
(declare apply-window)

;; Implementations
;; Private functions

(defn ^:private log2 [v] (/ (Math/log v) (Math/log 2)))

(defn ^:private generate-twiddle-table
  [window-size]
  ;; Loop: for level 1 -> 2^l and idx 0 -> N-1
   (vec
    (for [lvl (range 1 (inc (int @_module-max-level)))]
      (vec
        (for [idx (range 0 window-size)]
          (cconvert (->pComplex 1 (/ (double (* 2.0 Math/PI (int idx))) (bit-shift-left 1 (int lvl))))))))))

(defn ^:private get-node
  "Get the node at the specified indices"
  [level tree idx]
  ;; Use Threading!
  (-> @_module-sample-history (get tree) (get level) (get idx)))

(defn ^:private compute-node
  "Compute the desired node
  Returns a Cartesian complex number"
  [tree window-size level idx]
  ;; The current Node
  (let [l (dec level)
        t (int (- (int (/ (int window-size) 2)) (int (/ (int window-size) (bit-shift-left 1 level)))))
        i (mod idx level)]
    (let [current (-> @tree (get l) (get i))
          previous (get-node l t i)
          twiddle (-> @_module-twiddle-table (get l) (get idx))
          node (c+ (c* twiddle current) previous)]
        node)))

(defn ^:private compute-level
  "Compute the provided level for the current tree
  Retuns a vector of computed nodes"
  [tree level window-size]
  ;; define function constants
  (let [max-node-idx (bit-shift-left 1 level)]
    ;; Loop over idx, producing a node vector
    (loop [idx 0
           node-vec []]
      ;; Pre-check: idx is less than max-node-idx
      (if (< idx max-node-idx)
        ;; True: compute this node
        (let [target (compute-node tree window-size level idx)
              new-vec (conj node-vec target)]
          (recur (inc idx) new-vec))
        ;; False: return the computed node vector
        node-vec))))

(defn ^:private compute-tree
  "Compute the current tree for the given window size
  Returns a vector of vectors of nodes, each sub-vector
  is a level"
  [tree window-size]
  (let [max-level @_module-max-level]
    (loop [lvl 1]
      (if (<= lvl max-level)
        ;; True: levels to compute
        (let [target (compute-level tree lvl window-size)]
          ;; Update the tree with the newly computed level!
          (swap! tree conj target)
          ;; Loop back around
          (recur (inc lvl)))
        ;; False: return the lvl-vector
        tree))))

(defn ^:private handle-initial-sample
  "Process samples while the sample count is below
  the window size; simply fills the bins, until the window
  is filled, whence the steady-state algorithm will take over
  and begin to produce frequency bins."
  [tree window-size]
  (loop [level 1
         thresh (int (bit-shift-right window-size 1))]
    (if (>= @_module-sample-count thresh)
      ;; True: can calculate this level, then recurse
      ;; Calculate this level
      (let [level-vec (compute-level tree level window-size)]
        ;; atom-Conj it to the tree
        (swap! tree conj level-vec)
        ;; Increment the level
        (recur (inc level)
               (+ thresh (int (/ (int window-size) (bit-shift-left 1 (inc level)))))))
      ;; False: Return the Tree
      tree)))

(defn ^:private get-windowing-coeffs
  "Determine the coefficient set for the provided windowing function"
  [window-type]
  (case window-type
    :rectangle (->window-coeff 1 0)
    :hann (->window-coeff 0.5 0.25)
    :hamming (->window-coeff (double (/ 25.0 46.0)) (double (/ 21.0 46.0 2.0)))))

;; Public Functions
(defn init
  "Initialize the TSWiFT module"
  [options]
  ;; Copy the provided options to the global state
  (reset! _module-options options)
  ;; Generate the Twiddle Table
  (reset! _module-max-level (log2 (:window-size options)))
  (reset! _module-twiddle-table (generate-twiddle-table (:window-size options))))

(defn submit-sample
  "Submit a sample to the FFT, and produce a vector of
  frequency bins. The Bins are ordered with the positive frequency
  bins first (bins 0 -> N/2 - 1), and the negative frequency bins
  afterwards (bins N/2 -> N - 1).
  Requires that the module has been initialized, use:
  (tswift.core/init tswift-options)
  Samples are provided as real values, as doubles.
  Each call will either return nil, or a vector of frequency bins.
  Nil will be returned when the system is 'warming up', aka, while
  the sample count is less than the window size.
  The returned vector will be a vector of cComplex values, which
  can be referenced in the cmplx pakage."
  [sample]
  (let [window-size (:window-size @_module-options)]
    ;; create a new tree for the current sample
    (def current-tree (atom [[sample]]))
    ;; Compute the tree, return the full vector of vectors
    (if (< @_module-sample-count (dec window-size))
      (let [handled-tree (handle-initial-sample current-tree window-size)]
        (swap! _module-sample-count inc)
        (when (>= (count @_module-sample-history) (bit-shift-right window-size 1))
          (swap! _module-sample-history rest)
          (swap! _module-sample-history vec))
        (swap! _module-sample-history conj @handled-tree)
        ;; Return nil when we have no valid bins to produce
        nil)
      (let [tree (compute-tree current-tree window-size)]
        ;; Drop the Window/2 tree from the buffer
        (swap! _module-sample-history rest)
        ;; Convert back to a vector
        (swap! _module-sample-history vec)
        ;; Add on the new tree to the History
        (swap! _module-sample-history conj @tree)
        ;; return to the user the computed freq-bins- the last item in the tree
        (last @tree)))))

(defn apply-window
  "Apply a windowing function to the data set.
  Allows the user to select and apply one of: Hann, Hamming, Rectangular.
  Very slight performance impact (~2-5%) over using apply-hann-window, but
  allows for far more flexibility, potentially allowing further expansion into
  other Raised-Cosine windows"
  [window-type window-size bins]
  (let [window-size (int window-size)
        half-window (bit-shift-right window-size 1)
        mask (dec window-size)
        coeffs (get-windowing-coeffs window-type)]
    (loop [cnt 0
           output []]
      (if (< cnt window-size)
        (let [curr (c* (get bins cnt) (double (:a coeffs)))
              prev (if (= half-window cnt)
                     nil (get bins (bit-and (dec cnt) mask)))
              post (if (= half-window (dec cnt))
                     nil (get bins (bit-and (inc cnt) mask)))
              side-lobes (c* (c+ prev post) (double (:b coeffs)))
              item (c- curr side-lobes)]
          (recur (inc cnt) (conj output item)))
        output))))

