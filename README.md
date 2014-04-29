# feedback

Control theory applied to core.async. Following examples and discussion from the ["Feedback Control for Computer Systems"](http://shop.oreilly.com/product/0636920028970.do) book published by O'Reilly.

## Usage
Coming soon ;)

### Creating a process
A `process` consumes values from an input channel and produces output for an output channel. `feedback.processes` aims to make this a little easier:

```clojure
(ns user
  (:require [feedback.processes :refer (defprocess)]
            [core.async :refer (<! >! go-loop)]))
  
(defprocess say-hello [in-ch out-ch]
  (go-loop [m (<! in-ch)]
    (when m
      (>! out-ch (str "Hello" m))
      (recur (<! in-ch)))))

;; start the process
(say-hello)

;; consume the process output
(go-loop [m (<! say-hello)]
  (when m
    (println "Received:" m)
    (recur (<! say-hello))))

;; put some values to be handled by our say-hello process
(put! say-hello "Peter")
(put! say-hello "Paul")
```

To do:
* Process chaining
 * Channel re-wiring for process dependencies

## License

Copyright © 2014 Paul Ingles

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
