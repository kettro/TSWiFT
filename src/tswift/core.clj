(ns tswift.core
  (:require [cmplx.core :refer :all]))

;; TSWiFT

(defrecord tswift-options [window-size])

(def _module-options (atom (->tswift-options 8)))
(def _module-sample-count (atom 0))
(def _module-sample-history (atom []))
(def _module-twiddle-table (atom []))
(def _module-max-level (atom 1))

(declare generate-twiddle-table)
(declare log2)
(declare apply-hann-window)

;; Initialization of the module options
(defn init
  "Initialize the TSWiFT module"
  [options]
  ;; Copy the provided options to the global state
  (reset! _module-options options)
  ;; Generate the Twiddle Table
  (reset! _module-max-level (log2 (:window-size options)))
  (reset! _module-twiddle-table (generate-twiddle-table (:window-size options))))

(defn log2 [v] (/ (Math/log v) (Math/log 2)))

(defn generate-twiddle-table
  [window-size]
  ;; Loop: for level 1 -> 2^l and idx 0 -> N-1
   (vec
    (for [lvl (range 1 (inc (int @_module-max-level)))]
      (vec
        (for [idx (range 0 window-size)]
          (cconvert (->pComplex 1 (/ (double (* 2.0 Math/PI (int idx))) (bit-shift-left 1 (int lvl))))))))))

(defn get-node
  "Get the node at the specified indices"
  [level tree idx]
  ;; Use Threading!
  (-> @_module-sample-history (get tree) (get level) (get idx)))

(defn compute-node
  "Compute the desired node
  Returns a Cartesian complex number"
  [tree window-size level idx]
  ;; The current Node
  (let [l (dec level)
        t (int (- (int (/ (int window-size) 2)) (int (/ (int window-size) (bit-shift-left 1 level)))))
        i (mod idx level)]
    (let [current (-> @tree (get l) (get i))
          previous (get-node l t i)
          ;;twiddle (->pComplex 1 (/ (* 2.0 Math/PI idx) (Math/pow 2 level)))
          twiddle (-> @_module-twiddle-table (get l) (get idx))
          node (c+ (c* twiddle current) previous)]
        node)))

(defn compute-level
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

(defn compute-tree
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

(defn handle-initial-sample
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

(defn submit-sample
  "Submit a sample to the DFT"
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

(defn swap-range [at coll]
  (vec (concat (drop at coll) (take at coll))))

(defn apply-hann-window
  "Apply a Hann Window to the input frequency buffer using convolution.
  Convolution in the Frequency Domain is multiplication in the time domain.
  Results in a set with a gain of 4N"
  [bins window-size]
  (let [window-size (int window-size)
        half-window (int (bit-shift-right window-size 1))
        reordered-bins (swap-range half-window bins)]
    (loop [cnt (int 0)
           output []]
      ;; Do the calculations
      (let [item (c- (c* (get reordered-bins cnt) (double 2.0))
                     (c+ (get reordered-bins (dec cnt)) (get reordered-bins (inc cnt))))]
        (if (< cnt (dec window-size))
          (recur (inc cnt) (conj output item))
          (swap-range half-window (conj output item)))))))

