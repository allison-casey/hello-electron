(ns app.renderer.macros)

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(defmacro defipc-handler
  [ipc key & args]
  `(.on ~ipc ~key (fn ~@args)))
