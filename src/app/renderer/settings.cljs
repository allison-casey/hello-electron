(ns app.renderer.settings
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [button input-text label]]
            [re-dnd.events :as dnd]
            [re-dnd.views :as dndv]
            [cuerdas.core :as cuerdas]
            [app.renderer.subs :as subs]))

(def electron (js/window.require "electron"))
(def ipc (.-ipcRenderer electron))

;; ** Drag and Drop Test
(def last-id (r/atom 0))

(rf/reg-event-fx
 :my-drop-dispatch
 (fn [{db :db}
      [_
       [source-drop-zone-id source-element-id]
       [drop-zone-id dropped-element-id dropped-position]]]
   (swap! last-id inc)
   {:db       db
    :dispatch
    (if (= source-drop-zone-id drop-zone-id)
      [:dnd/move-drop-zone-element drop-zone-id source-element-id dropped-position]

      [:dnd/add-drop-zone-element
       drop-zone-id
       {:id   (keyword (str (name source-element-id) "-dropped-" @last-id))
        :type (if (odd? @last-id )
                :bluebox
                :redbox)}
       dropped-position])}))

(defmethod dndv/dropped-widget
  :my-drop-marker
  [{:keys [type id]}]
  [:div.drop-marker])

(defmethod dndv/dropped-widget
  :bluebox
  [{:keys [type id]}]
  [:div.box.blue-box
   (str type ", " id)])

(defmethod dndv/drag-handle
  :bluebox
  [{:keys [type id]}]
  [:div "bluedraghandle"])

(defmethod dndv/dropped-widget
  :redbox
  [{:keys [type id]}]
  [:div.list-group-item.d-flex.list-group-flush
   [:h4 "Ranger Bob"]])

(defmethod dndv/drag-handle
  :redbox
  [{:keys [type id]}]
  [:div "handle"])

(defmethod dndv/drag-handle
  "character"
  [_]
  [:div [:i.fa.fa-fw.fa-bars]])

(defmethod dndv/dropped-widget
  "character"
  [{:keys [name]}]
  [:div.list-group-item
   {:style {:padding-left "2.5em"}}
   [:span name]])

(defn dnd-example
  "test drag and drop component"
  []
  (let [drag-box-state (rf/subscribe [:dnd/drag-box])
        last-id (r/atom 1)
        characters @(rf/subscribe [::subs/characters])]
    (rf/dispatch [:dnd/initialize-drop-zone
                  :initiative-tracker
                  {:drop-dispatch [:my-drop-dispatch]
                   :drop-marker :my-drop-marker}
                  (->> characters
                     (sort-by (comp :initiative :tracker/internal))
                     (map #(update % :id keyword)))
                  ;; [{:type :character
                  ;;   :name "Ranger Bob"
                  ;;   :id "ranger.bob"}
                  ;;  {:type :character
                  ;;   :name "Larissa"
                  ;;   :id (keyword (cuerdas/kebab "Larissa"))}]
                  ])
    (fn []
      [:div.container
       (when @drag-box-state
         [dndv/drag-box])
       [dndv/drop-zone :initiative-tracker]])))


;; ** Render
(defn render []
  [:div.column
   [:div.row [label :label "Template Directory"]]
   [:div.row
    [input-text
     :model (rf/subscribe [::subs/template-dir])
     :on-change (constantly nil)
     :style {:cursor "pointer"}
     :attr {:on-click #(.send ipc "set-template-directory")
            :readOnly true}]]
   [dnd-example]])
