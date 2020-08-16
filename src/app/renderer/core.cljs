(ns app.renderer.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text button]]
            [cljs-node-io.core :as io :refer [slurp spit]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [app.renderer.character-select :as char-select]
            [app.renderer.combat-tracker :as combat-tracker]
            [goog.string :as gstring]
            [goog.string.format]))

(def ipc (.-ipcRenderer (js/window.require "electron")))

(enable-console-print!)

(defn process-template [{:keys [abilities] :as template}]
  (assoc template :abilities (#(zipmap (map :id %) %) abilities)))

(.on ipc
     "templates-reply"
     (fn [event arg]
       (rf/dispatch [:initialize-templates
                     (map process-template (js->clj arg :keywordize-keys true))])))

(defn tabs []
  (let [selected-tab @(rf/subscribe [::subs/tab])
        tab (fn [name id]
              [:li.nav-itme {:on-click #(rf/dispatch [:change-tab id])}
               [:a.nav-link {:href "#"} [:h3 (if (= id selected-tab) [:u name] name)]]])]
    [:ul.nav.justify-content-center.p-3
     [tab "Character Select" :character-select]
     [tab "Combat Tracker" :combat-tracker]]))

(defn root-component []
  (let [tab @(rf/subscribe [::subs/tab])]
    [:div.container
      [:div.row.justify-content-center
       [tabs]]
      [:div.row
       (case tab
         :character-select [char-select/render]
         :combat-tracker [combat-tracker/render])]]))

(defn start! []
  (rf/dispatch-sync [:initialize])
  (.send ipc "load-templates")
  (rd/render
   [root-component]
   (js/document.getElementById "app-container")))
