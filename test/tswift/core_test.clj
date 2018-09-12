(ns tswift.core-test
  (:require [clojure.test :refer :all]
            [cmplx.core :refer :all]
            [tswift.core :refer :all]))

(defrecord signal-options [mag phase freq])

(defn ^:private sin-gen
  "Lazily generate a sine wave of the given magnitude and frequency"
  [sig-opt]
  (let [mag (double (:mag sig-opt))
        phase (double (:phase sig-opt))
        freq (double (:freq sig-opt))
        n (cycle (range freq))]
    ((fn step [i]
       (lazy-seq
        (cons (* mag (Math/sin (+ (* 2.0 Math/PI (/ (first i) freq)) phase)))
              (step (rest i)))))
     n)))

(defn ^:private signal-gen
  "Lazily generate a summed signal of sine waves, using the given mags and frequencies"
  [sig-opts]
  (apply map + (map sin-gen sig-opts)))

(defn ^:private calc-magnitudes
  "Calculate the magnitudes of the provided frequency bins"
  [window-size bins]
  (loop [cnt 0
         out []]
    (if (< cnt window-size)
      (let [bin (get bins cnt)
            mag (/ (cmagnitude bin) (double window-size))]
        (recur (inc cnt) (conj out mag)))
      out)))

(defn ^:private print-bins
  "Print off the prvided bins"
  [bins]
  (dotimes [cnt (count bins)]
    (prn (str cnt " : " (get bins cnt)))))

(deftest analyze-sine-waves
  (testing "Simple Sine wave"
    (let [window-size 32
          iter-count 64
          options (->tswift-options window-size)
          sig-opts [(->signal-options 10.0 1.0 8)]
          signal (vec (take iter-count (signal-gen sig-opts)))]
      ;; Initialize the TSWiFT module
      (tswift.core/init options)
      (prn (str "Computing " iter-count " samples"))
      ;; Submit the samples and catalogue the results
      ;; stores only the last bin (injects windowing bias, which is a problem)
      (time (dotimes [cnt iter-count]
        (when-let [bins (submit-sample (get signal cnt))]
          (def freq-bins (apply-window :hann window-size bins)))))
      ;; Calculate and print the magnitudes
      (print-bins (calc-magnitudes window-size freq-bins)))))


