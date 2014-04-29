(ns feedback.processes
  (:require [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as p])
  (:import [clojure.lang IFn]
           [clojure.core.async.impl.protocols WritePort ReadPort]))

(defrecord AProcess [in-ch out-ch f]
  IFn
  (invoke [_] (f))

  WritePort
  (put! [port val handler]
    (p/put! in-ch val handler))
  ReadPort
  (take! [this handler]
    (p/take! out-ch handler)))

(defmacro defprocess [name args & body]
  `(let [in-ch# (async/chan)
         out-ch# (async/chan)
         ~args [in-ch# out-ch#]]
     (def ~name (AProcess. in-ch#
                           out-ch#
                           (fn ~(symbol (str name "-processfn"))
                             []
                             ~@body)))))


(comment
  (defprocess say-hello [in-ch out-ch]
    (async/go-loop [m (async/<! in-ch)]
      (async/>! out-ch (str "Hello " m))
      (recur (async/<! in-ch))))

  (say-hello) ; start the process

  ;; now we describe a go-loop consuming values
  ;; from the process
  (async/go-loop [m (async/<! say-hello)]
    (when m
      (println "Received:" m)
      (recur (async/<! say-hello))))

  ;; finally, lets send some values to the running process
  (async/put! say-hello "World 1")
  (async/put! say-hello "World 2")
)
