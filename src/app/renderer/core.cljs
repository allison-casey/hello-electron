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

(defn active-characters []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:div
     [:h4 "Characters"]
     [:ul.list-group
      (for [{:keys [uuid name]} characters]
        ^{:key uuid}
        [:li
         {:on-click #(rf/dispatch [:set-selected-character-id uuid])
          :class ["list-group-item" (if (= uuid active-character) "active")]}
         name])]]))


(defn tabs []
  (let [selected-tab @(rf/subscribe [::subs/tab])]
    (fn []
      [:ul.nav.justify-content-center
       [:li.nav-item {:on-click #(rf/dispatch [:change-tab :character-select])}
        [:a.nav-link.active {:href "#"} "Character Select"]]
       [:li.nav-item {:on-click #(rf/dispatch [:change-tab :combat-tracker])}
        [:a.nav-link {:href "#"} "Combat Tracker"]]])))

(defn root-component []
  (let [tab @(rf/subscribe [::subs/tab])]
    [:div
     [:div.container
      [:div.row.justify-content-center
       [tabs]]
      [:div.row
       (case tab
         :character-select [char-select/render]
         :combat-tracker [combat-tracker/render])]]]))

;; [(add-character)]
;; [active-characters]
;; [character-info]
;; [button
;;  :label "Increment Round"
;;  :on-click #(rf/dispatch [:increment-round])]
;; [button
;;  :label "Increment Interleaved Round"
;;  :on-click #(rf/dispatch [:increment-interleaved-round])]
;; [ability-timeline]

(defn start! []
  (rf/dispatch-sync [:initialize])
  (.send ipc "load-templates")
  (rd/render
   [root-component]
   (js/document.getElementById "app-container")))
