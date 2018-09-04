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
    (def converted-p (cconvert subject-polar))
    (is (= (roundc target-cart 6) (roundc converted-p 6)))))

(deftest convert-cartesian-to-polar
  (testing "Converting a Cartesian cComplex to a Polar pComplex"
   ;; Subject is (1,1)
   ;; Target is (sqrt(2), PI/4)
   (def subject (->cComplex 1.0 1.0))
   (def target (roundp (->pComplex (Math/sqrt 2.0) (/ Math/PI 4)) 6))
   (def rounded-converted (roundp (cconvert subject) 6))
   (is (= target rounded-converted))))

;; Conjugate
(deftest conjugate-cartesian-value
  (testing "Generate a conjugate to a Cartesian Complex number"
    (def target (->cComplex 1 -6))
    (def subject (->cComplex 1 6))
    (def conj-subject (cconjugate subject))
    (is (= target conj-subject))))

(deftest conjugate-polar-value
  (testing "Generate a conjugate to a Polar Complex number"
    (def target (->pComplex 4 (/ Math/PI 5)))
    (def subject (->pComplex 4 (* -1 (/ Math/PI 5))))
    (is (= target (cconjugate subject)))))

;; cpow - Complex Powers
; Only need to test the polar version, as the Cartesian just uses that
(deftest polar-power
  (testing "Raising a Polar value to a real power"
    (def target (->pComplex 8.0 (* Math/PI 1.5)))
    (def subject (->pComplex 2.0 (/ Math/PI 2.0)))
    (def pow-subj (cpow subject 3))
    (is (= (roundp target 6) (roundp pow-subj 6)))))

;; c+ - Complex Addition
(deftest cadd-cartesian-cartesian
  (testing "Adding a Cartesian to a Cartesian"
    (def target (->cComplex 12 21))
    (def s1 (->cComplex 4 4))
    (def s2 (->cComplex 8 17))
    (is (= target (c+ s1 s2)))))

(deftest cadd-cartesian-polar
  (testing "Adding a Cartesian to a Polar"
    (def target (->cComplex 3.0 4.0))
    (def s1 (->cComplex 2 3))
    (def s2 (->pComplex (Math/sqrt 2.0) (/ Math/PI 4)))
    (is (= target (roundc (c+ s1 s2) 5)))))

;; c* - Complex Multiplication
(deftest cmul-cartesian-cartesian
  (testing "Multiplying a Cartesian by a Cartesian"
    (def target (->cComplex 8 49))
    (def s1 (->cComplex 2 5))
    (def s2 (->cComplex 9 2))
    (is (= target (c* s1 s2)))))

(deftest cmul-polar-polar
  (testing "Multiplying a Polar by a Polar"
    (def target (->pComplex 6 (/ (* Math/PI 4) 6)))
    (def s1 (->pComplex 2 (/ Math/PI 6)))
    (def s2 (->pComplex 3 (/ Math/PI 2)))
    (is (= target (c* s1 s2)))))

;; cdiv - Complex Division
(deftest cdiv-cart-cart
  (testing "Dividing Cartesian by Cartesian"
    (def target (->cComplex (/ 6.0 25.0) (/ 17.0 25.0)))
    (def s1 (->cComplex 3.0 2.0))
    (def s2 (->cComplex 4.0 -3.0))
    (def division (cdiv s1 s2))
    (is (= (roundc target 5) (roundc division 5)))))

(deftest cdiv-polar-polar
  (testing "Dividing Polar by Polar"
    (def target (->pComplex 2 (/ Math/PI 6.0)))
    (def s1 (->pComplex 4 (/ Math/PI 3.0)))
    (def s2 (->pComplex 2 (/ Math/PI 6.0)))
    (def division (cdiv s1 s2))
    (is (= target division))))

