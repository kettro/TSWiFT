(ns tswift.core
  (:require [cmplx.core :refer :all]))

;; TSWiFT

(defrecord tswift-options [window-size])

(def _module-options (atom (->tswift-options 8)))
(def _module-sample-count (atom 0))
(def _module-sample-history (atom []))

;; Initialization of the module options
(defn init
  "Initialize the TSWiFT module"
  [options]
  ;; Copy the provided options to the global state
  (reset! _module-options options))

(defn log2 [v] (/ (Math/log v) (Math/log 2)))

(defn get-node
  "Get the node at the specified indices"
  [level tree idx]
  ;; Use Threading!
  (-> @_module-sample-history (get tree) (get level) (get idx)))

(defn compute-node
  "Compute the desired node
  Returns a polar complex number"
  [tree window level idx]
  ;; The current Node
  (let [l (dec level)
        t (int (- (/ window 2) (/ window (bit-shift-left 1 level))))
        i (mod idx level)]
    (let [current (-> @tree (get l) (get i))
          previous (get-node l t i)
          twiddle (->pComplex 1 (/ (* 2.0 Math/PI idx) (Math/pow 2 level)))
          node (c+ (c* twiddle current) previous)]
      (if (= cmplx.core.cComplex (type node))
        (convert node)
        node))))

(defn compute-level
  "Compute the provided level for the current tree
  Retuns a vector of computed nodes"
  [tree level window]
  ;; define function constants
  (let [max-node-idx (bit-shift-left 1 level)]
    ;; Loop over idx, producing a node vector
    (loop [idx 0
           node-vec []]
      ;; Pre-check: idx is less than max-node-idx
      (if (< idx max-node-idx)
        ;; True: compute this node
        (let [target (compute-node tree window level idx)
              new-vec (conj node-vec target)]
          (recur (inc idx) new-vec))
        ;; False: return the computed node vector
        node-vec))))

(defn compute-tree
  "Compute the current tree for the given window size
  Returns a vector of vectors of nodes, each sub-vector
  is a level"
  [tree window]
  (let [max-level (log2 window)]
    (loop [lvl 1]
      (if (<= lvl max-level)
        ;; True: levels to compute
        (let [target (compute-level tree lvl window)]
          ;; Update the tree with the newly computed level!
          (swap! tree conj target)
          ;; Loop back around
          (recur (inc lvl)))
        ;; False: return the lvl-vector
        tree))))

(defn handle-initial-sample
  [tree window-size]
  (loop [level 1
         thresh (/ window-size 2)]
    (if (>= @_module-sample-count thresh)
      ;; True: can calculate this level, then recurse
      ;; Calculate this level
      (let [level-vec (compute-level tree level window-size)]
        ;; atom-Conj it to the tree
        (swap! tree conj level-vec)
        ;; Increment the level
        ;; thresh = thresh + (window / ((level + 1) >> 1))
        (recur (inc level)
               ;(+ thresh (/ window-size (bit-shift-right (inc level) 1)))))
               (+ thresh (/ window-size (bit-shift-left 1 (inc level))))))
      ;; False: Return the Tree
      tree)))

(defn scale-freq-bins
  "Scale the output Frequency bins by the Window Size"
  [window-size bins]
  (vec (map #(->pComplex (/ (:r %) window-size) (:t %)) bins)))

(defn submit-sample
  "Submit a sample to the DFT"
  [sample]
  (let [window (:window-size @_module-options)]
    ;; create a new tree for the current sample
    (def current-tree (atom [[sample]]))
    ;; Compute the tree, return the full vector of vectors
    (if (< @_module-sample-count (dec window))
      (let [handled-tree (handle-initial-sample current-tree window)]
        (swap! _module-sample-count inc)
        (when (>= (count @_module-sample-history) (/ window 2))
          (swap! _module-sample-history rest)
          (swap! _module-sample-history vec))
        (swap! _module-sample-history conj @handled-tree))
      (let [tree (compute-tree current-tree window)]
        ;; Drop the Window/2 tree from the buffer
        (swap! _module-sample-history rest)
        ;; Convert back to a vector
        (swap! _module-sample-history vec)
        ;; Add on the new tree to the History
        (swap! _module-sample-history conj @tree)
        ;; return to the user the computed freq-bins- the last item in the tree
        (scale-freq-bins window (last @tree))))))

