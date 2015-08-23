(ns db-quiz.geometry)

(defn path-segment-distances
  "Computes distances between [x, y] points on a path segment." 
  [path-segment]
  (map (fn [[ax ay] [bx by]]
            (js/Math.sqrt (+ (js/Math.pow (- ax bx) 2)
                             (js/Math.pow (- ay by) 2))))
       path-segment
       (rest path-segment)))

(defn path-segment-length
  "Compute the length of a path segment.
  Path segment is a vector of [x y] coordinates."
  [path-segment]
  (apply + (path-segment-distances path-segment)))

(defn path-length
  "Total path length. Path consists of segments (vectors) consisting of [x, y] points.
  For example [[[10 10] [20 20]] [[80 80] [90 90]]] is a path, where [[10 10] [20 20]] is
  the first segment and [[80 80] [90 90]] is the second segment."
  [path]
  (apply + (map path-segment-length path)))

(defn intercept-coords
  "Computes [x, y] coordinates of an intercept of the line between A and B,
  where length is the distance between A and the intercept."
  [[ax ay] [bx by] length]
  (let [angle (js/Math.atan (/ (js/Math.abs (- ay by))
                               (js/Math.abs (- ax bx))))]
    [((if (< ax bx) + -) ax (* length (js/Math.sin angle)))
     ((if (< ay by) + -) ay (* length (js/Math.cos angle)))]))

(defn path-travelled
  "Returns points up to distance travelled."
  ; FIXME: Refactor nasty sequence manipulations.
  [path distance-travelled]
  {:pre [; Every path segment needs to have at least 2 points.
         (every? (partial < 1) (map count path))
         ; Distance travelled must be shorter than the path length.
         (>= (path-length path) distance-travelled)]}
  (let [[travelled-segments [[_ current-segment] & _]] (split-with (comp (partial > distance-travelled) first)
                                                                   (map vector
                                                                        (reductions +
                                                                                    (map path-segment-length path))
                                                                        path))
        segment-distance (- distance-travelled (or (first (last travelled-segments)) 0))
        [visited-points [[_ next-point] & _]] (split-with (comp (partial > segment-distance) first)
                                                          (map vector
                                                               (cons 0
                                                                     (reductions + (path-segment-distances
                                                                                     current-segment)))
                                                               current-segment))
        [point-distance last-visited-point] (last visited-points)
        intercept-distance (- segment-distance point-distance)
        intercept (intercept-coords last-visited-point
                                    next-point
                                    intercept-distance)]
    (conj (vec (map second travelled-segments))
          (conj (vec (map second visited-points))
                intercept))))

(defn relative-to-absolute-coords
  "Convert relative coordinates in path to absolute coordinates
  bounded by width and height." 
  [[width height] path]
  (map (partial map (fn [[x y]] [(* width x) (* height y)])) path))
