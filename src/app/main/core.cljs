(ns app.main.core
  (:require ["electron" :refer [app BrowserWindow crashReporter ipcMain]]
            ["fs" :as fs]
            ["yaml" :as yaml]
            ["electron-store" :as Store]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [defipc-handler]]
            [cljs-node-io.core :as io]))

(enable-console-print!)

(def main-window (atom nil))
(def store (Store.))
(def electron (js/require "electron"))
(def ipc (.-ipcMain electron))
(def dialog (.-dialog electron))

(defn read-templates
  [directory]
  (let [files (.readdirSync fs directory)
        template-paths (filter #(cuerdas/ends-with? % ".yaml") (js->clj files))
        parse-template (fn [template]
                         (->> template
                            (str directory \/ )
                            io/slurp
                            (.parse yaml)))]
    (->> template-paths
       js->clj
       (map parse-template)
       clj->js)))

(defipc-handler ipc "set-template-directory"
  [event _]
  (when-let [directory (first
                        (.showOpenDialogSync
                         dialog
                         (clj->js {:properties ["openDirectory"]})))]
    (.set store "template_directory" directory)
    (event.reply "templates-reply" (read-templates directory))))

(defipc-handler ipc "load-settings"
  [event _]
  (event.reply "settings" (.-store store)))

(defipc-handler ipc "load-templates"
  [event _]
  (when-let [directory (.get store "template_directory")]
    (let [templates (read-templates directory)]
      (event.reply "templates-reply" templates))))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                       (clj->js {:width 800
                                :height 600
                                :webPreferences {:nodeIntegration true}})))
                                        ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL @main-window (str "file://" js/__dirname "/public/index.html"))
  (.onDidAnyChange store (fn [old new]
                           (.send (.-webContents @main-window) "settings" new)))
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
