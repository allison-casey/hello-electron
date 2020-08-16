(ns app.main.core
  (:require ["electron" :refer [app BrowserWindow crashReporter ipcMain]]
            ["fs" :as fs]
            ["yaml" :as yaml]
            [cljs-node-io.core :as io]))

(def main-window (atom nil))
(def ipc (.-ipcMain (js/require "electron")))



(.on ipcMain
     "load-templates"
     (let [directory (str js/__dirname "/public/templates")]
       (fn [event arg]
         (.readdir fs
                   directory
                   (fn [err files]
                     (->> files
                          js->clj
                          (map #(->> (str directory \/ %)
                                     io/slurp
                                     (.parse yaml)))
                          clj->js
                          (event.reply "templates-reply")))))))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                        (clj->js {:width 800
                                  :height 600
                                  :webPreferences {:nodeIntegration true}})))
  ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on @main-window "closed" #(reset! main-window nil)))

(defn main []
  ; CrashReporter can just be omitted
  (.start crashReporter
          (clj->js
            {:companyName "MyAwesomeCompany"
             :productName "MyAwesomeApp"
             :submitURL "https://example.com/submit-url"
             :autoSubmit false}))

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser))
