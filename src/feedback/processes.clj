(ns feedback.processes
  (:require [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as p])
  (:import [clojure.lang IFn]
           [clojure.core.async.impl.protocols WritePort ReadPort]))


(defrecord ASource [out-ch f]
    IFn
    (invoke [_] (f))
    ReadPort
    (take! [this handler]
      (p/take! out-ch handler)))

(defrecord ASink [in-ch f]
    IFn
    (invoke [_] (f))
    WritePort
    (put! [port val handler]
      (p/put! in-ch val handler)))

(defrecord AProcess [in-ch out-ch f]
  IFn
  (invoke [_] (f))

  WritePort
  (put! [port val handler]
    (p/put! in-ch val handler))
  ReadPort
  (take! [this handler]
    (p/take! out-ch handler)))

(defmacro defsink [name args & body]
  `(let [in-ch# (async/chan)
         ~args [in-ch#]]
     (def ~name (ASink. in-ch#
                        (fn ~(symbol (str name "-sinkfn"))
                          []
                          ~@body)))))

(defmacro defprocess [name args & body]
  `(let [in-ch# (async/chan)
         out-ch# (async/chan)
         ~args [in-ch# out-ch#]]
     (def ~name (AProcess. in-ch#
                           out-ch#
                           (fn ~(symbol (str name "-processfn"))
                             []
                             ~@body)))))

(defn connect
  "Forwards messages from process to process."
  [{:keys [out-ch] :as from} {:keys [in-ch] :as to}]
  (async/go-loop [val (async/<! from)]
    (when val
      (async/>! in-ch val)
      (recur (async/<! from)))))

(comment
  (defprocess say-hello [in-ch out-ch]
    (async/go-loop [m (async/<! in-ch)]
      (async/>! out-ch (str "Hello " m))
      (recur (async/<! in-ch))))

  ;; now we describe a go-loop consuming values
  ;; from the process
  (defsink print-message [in-ch]
    (async/go-loop [m (async/<! in-ch)]
      (when m
        (println "Received:" m)
        (recur (async/<! in-ch)))))

  (print-message) ; start the sink
  (say-hello) ; start our processing step
  (connect say-hello print-message)

  ;; finally, lets send some values to the running process
  (async/put! say-hello "World 1")
  (async/put! say-hello "World 2")
)
