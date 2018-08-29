(ns cmplx.core)

(defrecord cComplex [x y])
(defrecord pComplex [r t])

;; Multimethod declarations
(defmulti c+        (fn [lv rv & more] [(type lv) (type rv)]))
(defmulti c-        (fn [lv rv & more] [(type lv) (type rv)]))
(defmulti c*        (fn [lv rv & more] [(type lv) (type rv)]))
(defmulti cdiv      (fn [lv rv & more] [(type lv) (type rv)]))
(defmulti cpow      (fn [lv rv] [(type lv) (type rv)]))
(defmulti convert   (fn [v] (type v)))
(defmulti conjugate (fn [v] (type v)))

;; Complex Addition
(defmethod c+ [cComplex cComplex]
  ([c1 c2]
   (->cComplex (+ (:x c1) (:x c2)) (+ (:y c1) (:y c2))))
  ([c1 c2 & more]
   (apply c+ (c+ c1 c2 ) more)))
(defmethod c+ [cComplex java.lang.Long]
  ([c r]
   (->cComplex (+ (:x c) r) (:y c)))
  ([c r & more]
   (apply c+ (c+ c r ) more)))
(defmethod c+ [cComplex java.lang.Double]
  ([c r]
   (->cComplex (+ (:x c) r) (:y c)))
  ([c r & more]
   (apply c+ (c+ c r ) more)))
(defmethod c+ [cComplex pComplex]
  ([c p]
   (c+ c (convert p)))
  ([c p & more]
   (apply c+ (c+ c p) more)))
(defmethod c+ [pComplex pComplex]
  ([p1 p2]
   (let [c1 (convert p1)
         c2 (convert p2)]
     (convert (c+ c1 c2))))
  ([p1 p2 & more]
   (apply c+ (c+ p2 p2) more)))
(defmethod c+ [pComplex java.lang.Long]
  ([p r]
   (convert (c+ (convert p) r)))
  ([ p r & more]
   (apply c+ (c+ p r) more)))
(defmethod c+ [pComplex java.lang.Double]
  ([p r]
   (convert (c+ (convert p) r)))
  ([ p r & more]
   (apply c+ (c+ p r) more)))
(defmethod c+ [pComplex cComplex]
  ([p c]
   (convert (c+ c p)))
  ([p c & more]
   (apply c+ (c+ c p) more)))

;; Complex Subtraction - basically the same as c+
(defmethod c- [cComplex cComplex]
  ([c1 c2]
   (->cComplex (- (:x c1) (:x c2)) (- (:y c1) (:y c2))))
  ([c1 c2 & more]
   (apply c- (c+ c1 c2 ) more)))
(defmethod c- [cComplex java.lang.Long]
  ([c r]
   (->cComplex (- (:x c) r) (:y c)))
  ([c r & more]
   (apply c- (c- c r ) more)))
(defmethod c- [cComplex java.lang.Double]
  ([c r]
   (->cComplex (- (:x c) r) (:y c)))
  ([c r & more]
   (apply c- (c- c r ) more)))
(defmethod c- [cComplex pComplex]
  ([c p]
   (c- c (convert p)))
  ([c p & more]
   (apply c- (c- c p) more)))
(defmethod c- [pComplex pComplex]
  ([p1 p2]
   (let [c1 (convert p1)
         c2 (convert p2)]
     (convert (c- c1 c2))))
  ([p1 p2 & more]
   (apply c+ (c- p2 p2) more)))
(defmethod c- [pComplex java.lang.Long]
  ([p r]
   (convert (c- (convert p) r)))
  ([ p r & more]
   (apply c+ (c- p r) more)))
(defmethod c- [pComplex java.lang.Double]
  ([p r]
   (convert (c- (convert p) r)))
  ([ p r & more]
   (apply c+ (c- p r) more)))
(defmethod c- [pComplex cComplex]
  ([p c]
   (convert (c- c p)))
  ([p c & more]
   (apply c- (c- c p) more)))

;; Complex Multiplication
(defmethod c* [cComplex cComplex]
  ([c1 c2]
   (let [c1r (:x c1)
         c1i (:y c1)
         c2r (:x c2)
         c2i (:y c2)]
     ;; Not the simple multiplication, need to
     ;; account for the j term
     (->cComplex (- (* c1r c2r) (* c1i c2i))
                 (+ (* c1r c2i) (* c2r c1i)))))
  ([c1 c2 & more]
   (apply c* (c* c1 c2) more)))
(defmethod c* [cComplex java.lang.Long]
  ([c r]
   (->cComplex (* (:x c) r) (* (:y c) r))))
(defmethod c* [cComplex java.lang.Double]
  ([c r]
   (->cComplex (* (:x c) r) (* (:y c) r))))
(defmethod c* [cComplex pComplex]
  ([c p]
   (c* c (convert p)))
  ([c p & more]
   (apply c* (c* c p) more )))
(defmethod c* [pComplex pComplex]
  ([p1 p2]
   (->pComplex (* (:r p1) (:r p2))
               (+ (:t p1) (:t p2))))
  ([p1 p2 & more]
   (apply c* (c* p1 p2) more)))
(defmethod c* [pComplex java.lang.Long]
  ([p r]
   (->pComplex (* (:r p) r) (:t p)))
  ([p r & more]
   (apply c* (c* p r) more)))
(defmethod c* [pComplex java.lang.Double]
  ([p r]
   (->pComplex (* (:r p) r) (:t p)))
  ([p r & more]
   (apply c* (c* p r) more)))
(defmethod c* [pComplex cComplex]
  ([p c]
   (c* p (convert c)))
  ([p c & more]
   (apply c* (c* p c) more)))

;; Complex Division
(defmethod cdiv [cComplex cComplex]
  ([c1 c2]
   (let [cconj (conjugate c2)
         numer (c* c1 cconj)
         denom (c* c2 cconj)]
     (cdiv numer (:x denom))))
  ([c1 c2 & more]
   (apply cdiv (cdiv c1 c2) more)))
(defmethod cdiv [cComplex java.lang.Double]
  ([c r]
   (->cComplex (/ (:x c) r)
               (/ (:y c) r)))
  ([c r & more]
   (apply cdiv (cdiv c r) more)))
(defmethod cdiv [cComplex java.lang.Long]
  ([c r]
   (->cComplex (/ (:x c) r)
               (/ (:y c) r)))
  ([c r & more]
   (apply cdiv (cdiv c r) more)))
(defmethod cdiv [cComplex pComplex]
  ([c p]
   (cdiv c (convert p)))
  ([c p & more]
   (apply cdiv (cdiv c p) more)))
(defmethod cdiv [pComplex pComplex]
  ([p1 p2]
   (->pComplex (/ (:r p1) (:r p2))
               (- (:t p1) (:t p2))))
  ([p1 p2 & more]
   (apply cdiv (cdiv p1 p2) more)))
(defmethod cdiv [pComplex java.lang.Long]
  ([p r]
   (->pComplex (/ (:r p) r) (:t p)))
  ([p r & more]
   (apply cdiv (cdiv p r) more)))
(defmethod cdiv [pComplex java.lang.Double]
  ([p r]
   (->pComplex (/ (:r p) r) (:t p)))
  ([p r & more]
   (apply cdiv (cdiv p r) more)))
(defmethod cdiv [pComplex cComplex]
  ([p c]
   (cdiv p (convert c)))
  ([p c & more]
   (apply cdiv (cdiv p c) more)))

;; Complex Exponentiation
(defmethod cpow [cComplex java.lang.Long]
  [c r]
  (convert (cpow (convert c) r)))
(defmethod cpow [cComplex java.lang.Double]
  [c r]
  (convert (cpow (convert c) r)))
(defmethod cpow [pComplex java.lang.Long]
  [p r]
  (->pComplex (Math/pow (:r p) r) (* (:t p) r)))
(defmethod cpow [pComplex java.lang.Double]
  [p r]
  (->pComplex (Math/pow (:r p) r) (* (:t p) r)))

;; Complex Conjugate
(defmethod conjugate cComplex
  [c]
  (->cComplex (:x c) (- (:y c))))
(defmethod conjugate pComplex
  [p]
  (->pComplex (:r p) (- (:t p))))

;; Complex Coordinate-plane Conversion (Polar <-> Cartesian)
(defmethod convert cComplex
  [c]
  (let [mag (Math/sqrt (+ (* (:x c) (:x c)) (* (:y c) (:y c))))
        ang (Math/atan2 (:y c) (:x c))]
    (->pComplex mag ang)))
(defmethod convert pComplex
  [p]
  (let [real (* (:r p) (Math/cos (:t p)))
        imag (* (:r p) (Math/sin (:t p)))]
    (->cComplex real imag)))
