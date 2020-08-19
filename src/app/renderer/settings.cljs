(ns app.renderer.settings
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [button input-text label]]
            [app.renderer.subs :as subs]
            ))

(def electron (js/window.require "electron"))
(def ipc (.-ipcRenderer electron))

(defn render []
  [:div
   [label :label "Template Directory"]
   [input-text
    :model (rf/subscribe [::subs/template-dir])
    :on-change (constantly nil)
    :style {:cursor "pointer"}
    :attr {:on-click #(.send ipc "set-template-directory")
           :readOnly true}]])
