(ns cmplx.core-test
  (:require [clojure.test :refer :all]
            [cmplx.core :refer :all]))

;; Test utilities for rounding because OFMG
(defn roundf
  [float-to-round places]
  (read-string (format (str "%0" places "f") float-to-round )))
(defn roundc
  [complex places]
  (->cComplex (roundf (:x complex) places)
            (roundf (:y complex) places)))
(defn roundp
  [complex places]
  (->pComplex (roundf (:r complex) places)
            (roundf (:t complex) places)))

;; Test the Records - Are they making valid objects
(deftest create-pComplex-record
  (testing "Creating a pComplex Record"
    (def polar (->pComplex 1 (/ Math/PI 4.0)))
    (is (= cmplx.core.pComplex (type polar )))))

(deftest create-cComplex-record
  (testing "Creating a cComplex Record"
    (def cart (->cComplex 2 3)))
    (is (= cmplx.core.cComplex (type cart))))

;; Conversion
(deftest convert-polar-to-cartesian
  (testing "Converting a Polar pComplex to Cartesian cComplex"
    ;; subject is sqrt(2), PI/4
    ;; Target is (1,1)
    (def target-cart (->cComplex 1.0 1.0))
    (def subject-polar (->pComplex (Math/sqrt 2) (/ Math/PI 4)))
    (def converted-p (convert subject-polar))
    (is (= (roundc target-cart 6) (roundc converted-p 6)))))

(deftest convert-cartesian-to-polar
  (testing "Converting a Cartesian cComplex to a Polar pComplex"
   ;; Subject is (1,1)
   ;; Target is (sqrt(2), PI/4)
   (def subject (->cComplex 1.0 1.0))
   (def target (roundp (->pComplex (Math/sqrt 2.0) (/ Math/PI 4)) 6))
   (def rounded-converted (roundp (convert subject) 6))
   (is (= target rounded-converted))))

;; Conjugate
